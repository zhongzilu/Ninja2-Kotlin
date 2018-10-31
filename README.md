Ninja2-Kotlin LTS
===

![background.png](/Art/screenshot/background.png "background.png")

一款简约、轻量级的安卓浏览器。原型及创意来源于[Ninja2](https://github.com/mthli/Ninja2 "mthli/Ninja2")，由于Ninja2未开源，于是打算自己写一个版本，并在原有基础上增加一些实用的功能，该版本将受到长期支持。

[戳此下载最新版本APK](https://github.com/zhongzilu/Ninja2-Kotlin/releases/download/v1.0.0/Ninja2-preview.1.0.apk "Ninja2-preview.1.0.apk")

__由于使用了一些Android新版本的特性，故没有做Android 5.0版本以下的兼容。__

## 特性:

 - 采用Kotlin编写，应用足够轻量，并且没有申请多余的权限
 - 网页长截图
 - 广告屏蔽
 - 指纹传感器扩展
 - 分屏浏览
 - 独立的任务窗口显示网页
 - 网页二维码识别
 - 支持网页跳转原生应用
 - 更多功能正在开发中....

## 如何使用?

Ninja2浏览器界面简约直观，没有太复杂的页面布局和功能，操作起来很方便快捷，没什么难度，但有些功能仍然值得一提。

### 书签列表:

 - 首次安装并打开Ninja2，会自动生成一个操作说明的书签记录，点击该记录则会看到更详细的操作说明

 - __长按__某条书签并上下拖动，则可以对整个书签列表进行排序

 - __左滑__某条书签可以删除该条记录，底部会弹出撤销按钮
 
 - __右滑__某条书签可以编辑书签名

### 地址栏:

 - 你可以在【设置】中开启或关闭【地址栏固定】，关闭后，地址栏将跟随网页滚动显示或隐藏，
 - 还可以在【设置】中开启或关闭【地址栏控制】，开启后，你将可以通过左右滑动地址栏来前进或后退访问历史
 
__注意__：如果底部菜单栏处于展开的状态下，地址栏功能将失效。

### 广告屏蔽:

广告屏蔽功能默认未开启，你可以在【设置】中开启广告屏蔽，由于该功能并不完善，如果你发现某些网页加载出问题，你可以将该功能关闭了再试。

屏蔽文件 `AdHosts` 来源于 [hpHosts](http://hosts-file.net/ad_servers.txt "hpHosts").

### 网页截图:

Ninja2支持将整个网页截取为长图片，但并不意味着你可以截取很长很长的网页，容易造成__内存溢出__。

该功能默认为关闭状态，需要你手动在【设置】中开启【显示截图菜单】选项，开启后将对以后加载网页的性能有一定的影响`（经过测试没发现什么太大的影响）`

### 长按返回:

有时候我们访问的网页历史很多了，此时想要直接回到书签列表页，可以通过长按返回键来实现

### 指纹传感器扩展

该功能需要在系统的无障碍服务中开启，开启后将可以通过指纹传感器快速切换标签页或进入分屏

### 搜索引擎

为了鼓励大家多使用搜索，应用植入了5种搜索引擎，默认为谷歌，可以在【设置】中更改默认使用的搜索引擎，只需要在地址栏中输入搜索关键字，就可以跳转到搜索结果页面

### 书签的导入/导出

应用支持书签的导入/导出，默认导出的位置为内置存储器的根目录下，默认命名格式为`Ninja2.当天日期.html`，该书签文件可以导入到其他PC浏览器，也可以导入其他PC浏览器的书签到本应用中

### 快捷操作

长按网页的任意地方，将会弹出快捷操作菜单，通过该菜单可以复制链接、下载图片、识别图片二维码、后台打开新页面等操作

## 依赖:

 - [Kotlin Anko SQLite](https://github.com/Kotlin/anko "Kotlin Anko SQLite")

 - [Grant permissions](https://github.com/anthonycr/Grant "Grant permissions")

 - [ZXing Core](https://github.com/zxing/zxing "ZXing Core")

## License:

_[Apache License, Version 2.0](https://github.com/zhongzilu/Ninja2-Kotlin/blob/master/LICENSE "Apache License, Version 2.0")_
