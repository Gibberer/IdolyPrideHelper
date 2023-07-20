from rebullet import Bullet, VerticalPrompt, Input, YesNo
import cv2 as cv
import time
from typing import Tuple
import msvcrt
import threading

from .device import DeviceHelper, ADBDriver
from .driver_adapter import DriverAdapter
from .tasks import Task, VenusTowerTask, MasterLiveTask


class Client:
    def __init__(self) -> None:
        self.tasks: list[Tuple[Task, str]] = []

    def start(self):
        helper = DeviceHelper()
        devices = helper.getDevices()
        if not devices:
            raise RuntimeError("当前无正在连接的设备")
        cli = VerticalPrompt(
            [
                Bullet(prompt="选择目标设备：", choices=devices),
                YesNo(prompt="是否设置资源重写目录", default="n"),
            ]
        )
        result = cli.launch()
        device_name = result[0][1]
        override_dir = ""
        if result[1][1]:
            override_dir = Input("请输入资源重写目录").launch()
        driver = DriverAdapter(ADBDriver(device_name))
        print(f"已连接设备：{device_name}, 分辨率：{driver.device_height}x{driver.device_width}")
        self._inital_tasks(driver, override_dir)
        self._start(driver)

    def _start(self, driver: DriverAdapter):
        print("运行中接受以下命令：\n\033[1mq\033[0m 退出当前正在执行的任务\n\033[1ms\033[0m 生成当前设备截图")
        def start_task(task:Task):
            for log in task():
                if log:
                    print(log)
                if task.is_canceled():
                    break
        task:Task = None
        while True:
            if not task or task.is_canceled():
                task = self.select_task()
                threading.Thread(target=start_task, args=(task,), daemon=True).start()
            if msvcrt.kbhit():
                ch = msvcrt.getch()
                if ch == b'q':
                    print("正在取消任务...")
                    task.cancel()
                    time.sleep(2)
                elif ch == b's':
                    output = f"{int(time.time() * 1000)}.png"
                    cv.imwrite(output, driver.screenshot())
                    print(f"已生成截图:{output}")

    def _inital_tasks(self, driver: DriverAdapter, override_dir: str):
        self.tasks = [
            (
                VenusTowerTask(driver, override_dir),
                "Venus Tower任务，需要游戏内进入Tower页面（显示决定按钮的页面）",
            ),
            (
                MasterLiveTask(driver, override_dir),
                "主要Live任务，需要进入Live页面（通过首页点击ライブ按钮进入）",
            ),
        ]

    def select_task(self) -> Task:
        cli = Bullet(
            prompt="选择任务：",
            choices=list(map(lambda x: x[1], self.tasks)),
            return_index=True,
        )
        _, pos = cli.launch()
        task:Task = self.tasks[pos][0]
        task.reset()
        return task
    
