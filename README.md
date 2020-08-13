# Android AppUpdate

[![](https://jitpack.io/v/ithomasoft/update.svg)](https://jitpack.io/#ithomasoft/update)

Android 版本升级库。
未集成其它第三方框架。


## 摘要

  - [功能介绍](#功能介绍)
  - [怎么使用](#怎么使用)
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
        
            
3.   开始使用！

## 更新日志

 - 1.0.0：初始版本，AppUpdate 2.8.0

## 关于作者

渣渣一枚

## 致谢

[AppUpdate](https://github.com/azhon/AppUpdate)