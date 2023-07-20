import cv2 as cv
import numpy as np
from pathlib import Path
from typing import Generator
from abc import ABC, abstractmethod
import time
from enum import Enum

from .driver_adapter import DriverAdapter
from .foundation import Rect,Point


class Status(Enum):
    Finished = 1


class Task(ABC):
    def __init__(
        self, driver_adapter: DriverAdapter, override_template_path: str = None
    ):
        self.driver = driver_adapter
        self.override_template_path = override_template_path
        self._canceled = False

    def __call__(self) -> Generator:
        yield from self.run()

    @abstractmethod
    def run(self) -> Generator:
        pass

    def cancel(self):
        self._canceled = True
    
    def is_canceled(self):
        return self._canceled
    
    def reset(self):
        self._canceled = False

    def _get_tempalte(self, name: str) -> np.ndarray:
        if "." not in name:
            name = f"{name}.png"
        if self.override_template_path:
            return cv.imread(str(Path(self.override_template_path) / name))
        return cv.imread(f"resources/{name}")

    def find_match_rects(
        self, screenshot, template_name, threshold=0.8, gray=False
    ) -> list[Rect]:
        template = self._get_tempalte(template_name)
        if gray:
            screenshot = cv.cvtColor(screenshot, cv.COLOR_BGR2GRAY)
            template = cv.cvtColor(template, cv.COLOR_BGR2GRAY)
        h, w = template.shape[:2]
        ret = cv.matchTemplate(screenshot, template, cv.TM_CCOEFF_NORMED)
        index_array = np.where(ret > threshold)
        if not index_array:
            return None
        matched_points = []
        for x, y in zip(*index_array[::-1]):
            duplicate = False
            for point in matched_points:
                if abs(point[0] - x) < 8 and abs(point[1] - y) < 8:
                    duplicate = True
                    break
            if not duplicate:
                matched_points.append((x, y))
        return [
            Rect(
                x, y, x + w, y + h, self.driver.device_width, self.driver.device_height
            )
            for x, y in matched_points
        ]

    def find_match_rect(self, screenshot, template_name, threshold=0.8) -> Rect:
        template = self._get_tempalte(template_name)
        h, w = template.shape[:2]
        ret = cv.matchTemplate(screenshot, template, cv.TM_CCOEFF_NORMED)
        min_val, max_val, min_loc, max_loc = cv.minMaxLoc(ret)
        if max_val > threshold:
            return Rect(
                max_loc[0],
                max_loc[1],
                max_loc[0] + w,
                max_loc[1] + h,
                self.driver.device_width,
                self.driver.device_height,
            )
    
    def retry(self, times: int = 3, interval: float = 0.5, func=None, args: tuple = None):
        if not func:
            return
        cnt = 0
        while cnt < times:
            ret = func(*args)
            if ret:
                return ret
            cnt += 1
            time.sleep(interval)
        
    def live_start(self)->Generator:
        while not self._canceled:
            yield
            screenshot = self.driver.screenshot()
            rect = self.find_match_rect(screenshot, "start")
            if not rect:
                continue
            recommend:Rect = self.retry(func=self.find_match_rect, args=(screenshot, "recommend"))
            if recommend:
                self.driver.click(recommend.center_point())
                time.sleep(3)
            self.driver.click(rect.center_point())
            break
        while not self._canceled:
            yield
            screenshot = self.driver.screenshot()
            rect = self.find_match_rect(screenshot, "close")
            if rect:
                self.driver.click(rect.center_point())
            rect = self.find_match_rect(screenshot, "live_logo")
            if rect:
                self.driver.swipe(Point(500, 1500), Point(500, 1500), duration=8)
                continue
            rect = self.find_match_rect(screenshot, "next")
            if rect:
                self.driver.click(rect.center_point())
                continue
            rect = self.find_match_rect(screenshot, "failed_next")
            if rect:
                yield Status.Finished
                break
            rect = self.find_match_rect(screenshot, "failed_logo")
            if rect:
                yield Status.Finished
                break
            rect = self.find_match_rect(screenshot, "finish")
            if rect:
                self.driver.click(rect.center_point())
                break
        yield


class VenusTowerTask(Task):
    def run(self) -> Generator:
        pre_turn = 0
        turn = 1
        start = time.time()
        enable_hint = True
        while not self._canceled:
            if turn != pre_turn:
                pre_turn = turn
                yield f"正在执行Venus Tower任务, 第{turn}次"
            else:
                yield
            screenshot = self.driver.screenshot()
            rect = self.find_match_rect(screenshot, "confirm")
            if not rect:
                if enable_hint and (time.time() - start) > 30:
                    enable_hint = False
                    yield "识别超时，请检查游戏页面"
                continue
            self.driver.click(rect.center_point())
            for log in self.live_start():
                if log == Status.Finished:
                    yield "演出失败终止任务"
                    self.cancel()
                    break
                else:
                    yield log
            turn += 1
            start = time.time()
            enable_hint = True
            

class MasterLiveTask(Task):
    def run(self) -> Generator:
        pre_turn = 0
        turn = 1
        start = time.time()
        enable_hint = True
        while not self._canceled:
            if turn != pre_turn:
                pre_turn = turn
                yield f"正在执行Master Live任务, 第{turn}次"
            else:
                yield
            screenshot = self.driver.screenshot()
            rect = self.find_match_rect(screenshot, "live_home_page")
            if rect:
                self.driver.click(Point(535, 1350))
                time.sleep(2)
            rect = self.find_match_rect(screenshot, "composition_confirm")
            if not rect:
                if enable_hint and (time.time() - start) > 30:
                    enable_hint = False
                    yield "识别超时，请检查游戏页面"
                continue
            self.driver.click(rect.center_point())
            for log in self.live_start():
                if log == Status.Finished:
                    yield "演出失败终止任务"
                    self.cancel()
                    break
                else:
                    yield log
            turn += 1
            start = time.time()
            enable_hint = True