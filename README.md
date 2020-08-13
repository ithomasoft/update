# Android AppUpdate

[![](https://jitpack.io/v/ithomasoft/update.svg)](https://jitpack.io/#ithomasoft/update)

Android 版本升级库。
未集成其它第三方框架。


## 摘要

  - [功能介绍](#功能介绍)
  - [DownloadManager](#downloadmanager配置文档)
  - [UpdateConfiguration](#updateconfiguration配置文档)
  - [使用步骤](#使用步骤)
  - [使用技巧](#使用技巧)
  - [更新日志](#更新日志)
  - [关于作者](#关于作者)
  - [致谢](#致谢)
  
  
## 功能介绍

* [x] 支持AndroidX
* [x] 支持后台下载
* [x] 支持强制更新
* [x] 支持自定义下载过程
* [x] 支持 设备 >= Android M 动态权限的申请
* [x] 支持通知栏进度条展示(或者自定义显示进度)
* [x] 支持Android N
* [x] 支持Android O
* [x] 支持Android P
* [x] 支持Android Q
* [x] 支持中/英文双语（国际化）
* [x] 支持自定义内置对话框样式
* [x] 支持取消下载(如果发送了通知栏消息，则会移除)
* [x] 支持下载完成 打开新版本后删除旧安装包文件
* [x] 使用HttpURLConnection下载，未集成其他第三方框架

## DownloadManager：配置文档

> 初始化使用`DownloadManager.getInstance(this)`

| 属性             | 描述                                                                                    | 默认值                | 是否必须设置 |
|:-------------- |:----------------------------------------------------------------------------------------- |:--------------------- |:------------ |
| context        | 上下文                                                                                    | null                  | true         |
| apkUrl         | apk的下载地址                                                                             | null                  | true         |
| apkName        | apk下载好的名字                                                                           | null                  | true         |
| downloadPath   | apk下载的位置 (2.7.0以上版本已过时)                                                       | getExternalCacheDir() | false        |
| showNewerToast | 是否提示用户 "当前已是最新版本"                                                           | false                 | false        |
| smallIcon      | 通知栏的图标(资源id)                                                                      | -1                    | true         |
| configuration  | 这个库的额外配置                                                                          | null                  | false        |
| apkVersionCode | 更新apk的versionCode <br>(如果设置了那么库中将会进行版本判断<br>下面的属性也就需要设置了)           | Integer.MIN_VALUE     | false        |
| apkVersionName | 更新apk的versionName                                                                      | null                  | false        |
| apkDescription | 更新描述                                                                                  | null                  | false        |
| apkSize        | 新版本的安装包大小（单位M）                                                               | null                  | false        |
| apkMD5         | 新安装包的md5（32位)                                                                      | null                  | false        |

## UpdateConfiguration：配置文档

| 属性                  | 描述                                   | 默认值       |
|:--------------------- |:-------------------------------------- |:------       |
| notifyId              | 通知栏消息id                           | 1011         |
| notificationChannel   | 适配Android O的渠道通知                | 详情查阅源码 |
| httpManager           | 设置自己的下载过程                     | null         |
| onDownloadListener    | 下载过程的回调                         | null         |
| jumpInstallPage       | 下载完成是否自动弹出安装页面           | true         |
| showNotification      | 是否显示通知栏进度（后台下载提示）     | true         |
| forcedUpgrade         | 是否强制升级                           | false        |
| showBgdToast          | 是否提示 "正在后台下载新版本…"        | true         |
| onButtonClickListener | 按钮点击事件回调                       | null         |
| dialogImage           | 对话框背景图片资源(图片规范参考demo)   | -1           |
| dialogButtonColor     | 对话框按钮的颜色                       | -1           |
| dialogButtonTextColor | 对话框按钮的文字颜色                   | -1           |
| dialogProgressBarColor| 对话框进度条和文字颜色                 | -1           |


## 怎么使用

1. 在项目根目录的`build.gradle`文件中添加如下代码：
        
         
    	    allprojects {
    		    repositories {
    			    ...
    			    maven { url 'https://jitpack.io' }
    		    }
    	    }  
    	
    	
2.  添加项目依赖：
        
        
            dependencies {
                implementation 'com.github.ithomasoft:update:1.0.0'
            } 
        
            
3.   创建`DownloadManager`

    ```java
        DownloadManager manager = DownloadManager.getInstance(this);
        manager.setApkName("appupdate.apk")
                .setApkUrl("https://raw.githubusercontent.com/azhon/AppUpdate/master/apk/appupdate.apk")
                .setSmallIcon(R.mipmap.ic_launcher)
                .download();
    ```

## 更新日志

 - 1.0.0：初始版本，AppUpdate 2.8.0

## 关于作者

渣渣一枚

## 致谢

[AppUpdate](https://github.com/azhon/AppUpdate)