from dataclasses import dataclass

_DEFAULT_WIDTH = 1080
_DEFAULT_HEIGHT = 2400

@dataclass
class Scalar:
    value:int
    define_size:int

    def __mul__(self, rhs):
        return Scalar(self.value * rhs, self.define_size)
    
    def __neg__(self):
        return Scalar(-self.value, self.define_size)

@dataclass
class VScalar(Scalar):
    define_size:int = _DEFAULT_HEIGHT

@dataclass
class HScalar(Scalar):
    define_size:int = _DEFAULT_WIDTH

@dataclass
class Point:
    x:int
    y:int
    define_width:int = _DEFAULT_WIDTH
    define_height:int = _DEFAULT_HEIGHT

@dataclass
class Rect:
    left:int
    top:int
    right:int
    bottom:int
    define_width:int = _DEFAULT_WIDTH
    define_height:int = _DEFAULT_HEIGHT

    def convert(self, target_width, target_height):
        scaleX = self.define_width / target_width
        scaleY = self.define_height / target_height
        return Rect(int(self.left*scaleX), int(self.top*scaleY), int(self.right*scaleX), int(self.bottom*scaleY), target_width, target_height)
    
    def inset(self, l:Scalar = None, t:Scalar = None, r:Scalar=None, b:Scalar=None):
        left,top,right,bottom = self.left,self.top,self.right,self.bottom
        if l:
            scalex = self.define_width/l.define_size
            left += int(l.value * scalex)
        if r:
            scalex = self.define_width/r.define_size
            right += int(r.value * scalex)
        if t:
            scaley = self.define_height/t.define_size
            top += int(t.value * scaley)
        if b:
            scaley = self.define_height/b.define_size
            bottom += int(b.value * scaley)
        return Rect(left, top, right, bottom, self.define_width, self.define_height)

    def contain(self, other):
        other = other.convert(self.define_width, self.define_height)
        return other.left >= self.left and other.right <= self.right and other.top >= self.top and other.bottom <= self.bottom 
    
    def vertical_overlap(self, other)->bool:
        other = other.convert(self.define_width, self.define_height)
        return not ((self.top > other.bottom) or (self.bottom < other.top))
    
    def center_point(self)->Point:
        return Point(int((self.left + self.right)/2), int((self.top + self.bottom)/2), self.define_width, self.define_height)
    
    def offset(self, x:Scalar = None, y:Scalar = None):
        left,top,right,bottom = self.left,self.top,self.right,self.bottom
        if x:
            scalex = self.define_width/x.define_size
            offset = int(x.value * scalex)
            left += offset
            right += offset
        if y:
            scaley = self.define_height/y.define_size
            offset = int(y.value * scaley)
            top += offset
            bottom += offset
        return Rect(left, top, right, bottom, self.define_width, self.define_height)