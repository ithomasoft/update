package com.thomas.update.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import com.thomas.update.config.UpdateConfiguration;
import com.thomas.update.manager.DownloadManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UpdateConfiguration configuration = new UpdateConfiguration()
                //下载完成自动跳动安装页面
                .setJumpInstallPage(true)
                //设置对话框背景图片 (图片规范参照demo中的示例图)
                //.setDialogImage(R.drawable.ic_dialog)
                //设置按钮的颜色
                .setDialogButtonColor(Color.parseColor("#1FA2EA"))
                //设置对话框强制更新时进度条和文字的颜色
                .setDialogProgressBarColor(Color.parseColor("#1FA2EA"))
                //设置按钮的文字颜色
                .setDialogButtonTextColor(Color.WHITE)
                //设置是否显示通知栏进度
                .setShowNotification(true)
                //设置是否提示后台下载toast
                .setShowBgdToast(true)
                //设置强制更新
                .setForcedUpgrade(false);

        DownloadManager.getInstance(this).setApkName("1.1.0" + ".apk")
                .setApkUrl("https://www.baidu.com")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowNewerToast(true)
                .setConfiguration(configuration)
                .setApkVersionCode(2)
                .setApkVersionName("1.1.0")
                .setApkDescription("测试下载")
                .download();
    }
}