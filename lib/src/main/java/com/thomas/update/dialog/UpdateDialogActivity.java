package com.thomas.update.dialog;


import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.thomas.update.R;
import com.thomas.update.config.UpdateConfiguration;
import com.thomas.update.listener.OnButtonClickListener;
import com.thomas.update.listener.OnDownloadListener;
import com.thomas.update.manager.DownloadManager;
import com.thomas.update.service.DownloadService;

import java.io.File;

public class UpdateDialogActivity extends AppCompatActivity implements View.OnClickListener, OnDownloadListener {

    private DownloadManager manager;
    private Button update;
    private NumberProgressBar progressBar;
    private OnButtonClickListener buttonClickListener;
    private File apk;
    private int dialogImage, dialogButtonTextColor, dialogButtonColor, dialogProgressBarColor;
    private boolean forcedUpgrade;
    private final int install = 0x45F;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setTitle("");
        setContentView(R.layout.dialog_update);
        init();
    }

    /**
     * 初始化布局
     */
    private void init() {
        manager = DownloadManager.getInstance();
        UpdateConfiguration configuration = manager.getConfiguration();
        configuration.setOnDownloadListener(this);
        forcedUpgrade = configuration.isForcedUpgrade();
        buttonClickListener = configuration.getOnButtonClickListener();
        dialogImage = configuration.getDialogImage();
        dialogButtonTextColor = configuration.getDialogButtonTextColor();
        dialogButtonColor = configuration.getDialogButtonColor();
        dialogProgressBarColor = configuration.getDialogProgressBarColor();
        setWindowSize();
        initView();
    }

    private void initView() {
        View ibClose = findViewById(R.id.ib_close);
        ImageView ivBg = findViewById(R.id.iv_bg);
        TextView title = findViewById(R.id.tv_title);
        TextView size = findViewById(R.id.tv_size);
        TextView description = findViewById(R.id.tv_description);
        progressBar = findViewById(R.id.np_bar);
        progressBar.setVisibility(forcedUpgrade ? View.VISIBLE : View.GONE);
        update = findViewById(R.id.btn_update);
        update.setTag(0);
        View line = findViewById(R.id.line);
        update.setOnClickListener(this);
        ibClose.setOnClickListener(this);
        //自定义
        if (dialogImage != -1) {
            ivBg.setBackgroundResource(dialogImage);
        }
        if (dialogButtonTextColor != -1) {
            update.setTextColor(dialogButtonTextColor);
        }
        if (dialogButtonColor != -1) {
            StateListDrawable drawable = new StateListDrawable();
            GradientDrawable colorDrawable = new GradientDrawable();
            colorDrawable.setColor(dialogButtonColor);
            colorDrawable.setCornerRadius(dip2px(this, 3));
            drawable.addState(new int[]{android.R.attr.state_pressed}, colorDrawable);
            drawable.addState(new int[]{}, colorDrawable);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                //Android系统大于等于API16，使用setBackground
                update.setBackground(drawable);
            } else {
                //Android系统小于API16，使用setBackground
                update.setBackgroundDrawable(drawable);
            }
        }
        if (dialogProgressBarColor != -1) {
            progressBar.setReachedBarColor(dialogProgressBarColor);
            progressBar.setProgressTextColor(dialogProgressBarColor);
        }
        //强制升级
        if (forcedUpgrade) {
            line.setVisibility(View.GONE);
            ibClose.setVisibility(View.GONE);
        }
        //设置界面数据
        if (!TextUtils.isEmpty(manager.getApkVersionName())) {
            String newVersion = getResources().getString(R.string.dialog_new);
            title.setText(String.format(newVersion, manager.getApkVersionName()));
        }
        if (!TextUtils.isEmpty(manager.getApkSize())) {
            String newVersionSize = getResources().getString(R.string.dialog_new_size);
            size.setText(String.format(newVersionSize, manager.getApkSize()));
            size.setVisibility(View.VISIBLE);
        }
        description.setText(manager.getApkDescription());
    }

    private void setWindowSize() {
        Window dialogWindow = this.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = (int) (getWith(this) * 0.7f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        dialogWindow.setAttributes(lp);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ib_close) {
            if (!forcedUpgrade) {
                finish();
            }
            //回调点击事件
            if (buttonClickListener != null) {
                buttonClickListener.onButtonClick(OnButtonClickListener.CANCEL);
            }
        } else if (id == R.id.btn_update) {
            if ((int) update.getTag() == install) {
                installApk();
                return;
            }
            if (forcedUpgrade) {
                update.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                update.setEnabled(false);
                update.setText(R.string.background_downloading);
            } else {
                finish();
            }
            //回调点击事件
            if (buttonClickListener != null) {
                buttonClickListener.onButtonClick(OnButtonClickListener.UPDATE);
            }
            startService(new Intent(this, DownloadService.class));
        }
    }

    /**
     * 强制更新，点击进行安装
     */
    private void installApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this, getPackageName() + ".update.provider", apk);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(apk);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (forcedUpgrade) return;
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void start() {

    }

    @Override
    public void downloading(int max, int progress) {
        if (max != -1 && progressBar.getVisibility() == View.VISIBLE) {
            int curr = (int) (progress / (double) max * 100.0);
            progressBar.setProgress(curr);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void done(File apk) {
        this.apk = apk;
        if (forcedUpgrade) {
            update.setTag(install);
            update.setEnabled(true);
            update.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            update.setText(R.string.click_hint);
        }
    }

    @Override
    public void cancel() {

    }

    @Override
    public void error(Exception e) {

    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    /**
     * 获取屏幕宽度（像素）
     *
     * @param context 上下文
     * @return px
     */
    private static int getWith(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }
}
