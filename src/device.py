from abc import ABCMeta, abstractmethod
from typing import Tuple
import numpy as np
import os
import subprocess
import cv2 as cv

class Driver(metaclass=ABCMeta):
    @abstractmethod
    def click(self, x, y):
        pass

    @abstractmethod
    def input(self, text):
        pass

    @abstractmethod
    def screenshot(self):
        pass

    @abstractmethod
    def getScreenSize(self) -> Tuple[int, int]:
        pass

    @abstractmethod
    def swipe(self, start: Tuple[int, int], end: Tuple[int, int], duration: int):
        pass
    
    def getRootWindowLocation(self) -> Tuple[int, int]:
        # 获取根窗口的位置坐标
        return (0,0)
    
    def getScale(self):
        return 1


class ADBDriver(Driver):
    def __init__(self, device_name):
        super().__init__()
        self.device_name = device_name
        self.device_width, self.device_height = self.getScreenSize()

    def click(self, x, y):
        self._shell("input tap {} {}".format(x, y), ret=False)

    def input(self, text):
        self._shell("input text {}".format(text), ret=False)

    def screenshot(self, output="screen_shot.png"):
        if not output:
            p = subprocess.Popen(f"adb -s {self.device_name} shell screencap", shell = True, stdout = subprocess.PIPE)
            # \r\n是由于windows系统，去掉前16个字符是由于多出来的部分，不清楚具体内容是什么
            image_buffer = p.stdout.read().replace(b'\r\n', b'\n')[16:]
            image = np.frombuffer(image_buffer, np.uint8)
            image.shape = (self.device_height, self.device_width, 4)
            # RGB
            return image[:,:,:3]
        self._shell("screencap -p /sdcard/opsd.png")
        output = "{}-{}".format(self.device_name, output)
        self._cmd("pull /sdcard/opsd.png {}".format(output))
        return cv.imread(output)


    def getScreenSize(self) -> Tuple[int, int]:
        return map(lambda x: int(x), self._shell("wm size", True).split(":")[-1].split("x"))
    

    def swipe(self, start, end=None, duration=500):
        if not end:
            end = start
        self._shell("input touchscreen swipe {} {} {} {} {}".format(
            *start, *end, duration))

    def _shell(self, cmd, ret=True):
        return self._cmd("shell {}".format(cmd), ret)

    def _cmd(self, cmd, ret=True):
        cmd = "adb -s {} {}".format(self.device_name, cmd)
        if ret:
           return os.popen(cmd).read()
        else:
           os.system(cmd)


class DeviceHelper:
    """
    正常支持ADB命令的设备
    """

    def __init__(self):
        pass

    def getDevices(self) -> list[str]:
        lines = os.popen("adb devices").readlines()
        if not lines or len(lines) < 2:
            print("没有设备信息：{}".format(lines[0] if lines else "None"))
            return None
        devices = []
        for line in lines:
            if '\t' in line:
                name, status = line.split('\t')
                if 'device' in status:
                    devices.append(name)
        return devices

    def getDirvers(self) -> list[Driver]:
        devices = self.getDevices()
        return list(map(lambda device: ADBDriver(device), devices))