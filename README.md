## 介绍
用于手游idolyPride里清理VENUSタワー和主线Live（マスター）。
工作流程如下：
1. 点击“决定”按钮
2. 点击“おまかせ”按钮
3. 点击“开始”按钮
4. 等待Live结束
5. 重复1步骤

## 使用说明
环境要求：
* Windows10/11
* Python3.11并且安装Pipenv
* 终端下adb命令可用
* 手机分辨率为2400x1080，其他分辨率如果效果不行可以考虑替换下[resource](/resources)里的图片

使用说明：
* 项目根目录`pipenv install`安装依赖
* 执行程序`pipenv run python main.py`

## 使用Android应用执行
由于使用频率比较高每次连接电脑不太方便，补充了一个使用Android无障碍服务实现的版本，具体可以参考[Android](/Android)目录。
