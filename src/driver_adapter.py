from .device import Driver
from .foundation import Point,Rect
import cv2 as cv


class DriverAdapter:
    """
    用于处理设备尺寸与代码硬编码尺寸不同的问题
    """

    def __init__(self, driver: Driver):
        self._driver = driver
        self.device_width, self.device_height = driver.getScreenSize()

    def click(self, point:Point):
        scaleX = self.device_width / point.define_width
        scaleY = self.device_height / point.define_height
        self._driver.click(int(point.x * scaleX), int(point.y * scaleY))
    
    def swipe(self, start:Point, end:Point, duration=2):
        scaleX = self.device_width / start.define_width
        scaleY = self.device_height / start.define_height
        self._driver.swipe((int(start.x * scaleX), int(start.y * scaleY)), (int(end.x*scaleX), int(end.y*scaleY)), int(duration * 1000))

    def screenshot(self):
        return self._driver.screenshot()

    def roi(self, screenshot, rect:Rect):
        scaleX = self.device_width / rect.define_width
        scaleY = self.device_height / rect.define_height
        left = int(rect.left * scaleX)
        right = int(rect.right * scaleX)
        top = int(rect.top * scaleY)
        bottom = int(rect.bottom * scaleY)
        return screenshot[top:bottom, left:right]
    
    def to_device_rect(self, rect:Rect)->Rect:
        scaleX = self.device_width / rect.define_width
        scaleY = self.device_height / rect.define_height
        left = int(rect.left * scaleX)
        right = int(rect.right * scaleX)
        top = int(rect.top * scaleY)
        bottom = int(rect.bottom * scaleY)
        return Rect(left, top, right, bottom, define_width=self.device_width, define_height=self.device_height)
