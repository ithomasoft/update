package com.thomas.update.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.thomas.update.R;
import com.thomas.update.config.Constant;
import com.thomas.update.config.UpdateConfiguration;
import com.thomas.update.listener.OnDownloadListener;
import com.thomas.update.manager.BaseHttpDownloadManager;
import com.thomas.update.manager.DownloadManager;
import com.thomas.update.manager.HttpDownloadManager;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

public final class DownloadService extends Service implements OnDownloadListener {

    private static final String TAG = Constant.TAG + "DownloadService";
    private int smallIcon;
    private String apkUrl;
    private String apkName;
    private String downloadPath;
    private List<OnDownloadListener> listeners;
    private boolean showNotification;
    private boolean showBgdToast;
    private boolean jumpInstallPage;
    private int lastProgress;
    private DownloadManager downloadManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent) {
            return START_STICKY;
        }
        init();
        return super.onStartCommand(intent, flags, startId);
    }


    private void init() {
        downloadManager = DownloadManager.getInstance();
        if (downloadManager == null) {
            Log.d(TAG, "init DownloadManager.getInstance() = null ,请先调用 getInstance(Context context) !");
            return;
        }
        apkUrl = downloadManager.getApkUrl();
        apkName = downloadManager.getApkName();
        downloadPath = downloadManager.getDownloadPath();
        smallIcon = downloadManager.getSmallIcon();
        //创建apk文件存储文件夹
        createDirDirectory(downloadPath);

        UpdateConfiguration configuration = downloadManager.getConfiguration();
        listeners = configuration.getOnDownloadListener();
        showNotification = configuration.isShowNotification();
        showBgdToast = configuration.isShowBgdToast();
        jumpInstallPage = configuration.isJumpInstallPage();
        //获取app通知开关是否打开
        boolean enable = notificationEnable(this);
        Log.d(TAG, enable ? "应用的通知栏开关状态：已打开" : "应用的通知栏开关状态：已关闭");
        if (checkApkMD5()) {
            Log.d(TAG, "文件已经存在直接进行安装");
            //直接调用完成监听即可
            done(new File(downloadPath, apkName));
        } else {
            Log.d(TAG, "文件不存在开始下载");
            download(configuration);
        }
    }

    /**
     * 校验Apk是否已经下载好了，不重复下载
     *
     * @return 是否下载完成
     */
    private boolean checkApkMD5() {
        if (fileExists(downloadPath, apkName)) {
            String fileMD5 = getFileMD5(new File(downloadPath, apkName));
            return fileMD5.equalsIgnoreCase(downloadManager.getApkMD5());
        }
        return false;
    }

    /**
     * 获取下载管理者
     */
    private synchronized void download(UpdateConfiguration configuration) {
        if (downloadManager.isDownloading()) {
            Log.e(TAG, "download: 当前正在下载，请务重复下载！");
            return;
        }
        BaseHttpDownloadManager manager = configuration.getHttpManager();
        //使用自己的下载
        if (manager == null) {
            manager = new HttpDownloadManager(downloadPath);
            configuration.setHttpManager(manager);
        }
        //如果用户自己定义了下载过程
        manager.download(apkUrl, apkName, this);
        downloadManager.setState(true);
    }


    @Override
    public void start() {
        if (showNotification) {
            if (showBgdToast) {
                handler.sendEmptyMessage(0);
            }
            String startDownload = getResources().getString(R.string.start_download);
            String startDownloadHint = getResources().getString(R.string.start_download_hint);
            showNotification(this, smallIcon, startDownload, startDownloadHint);
        }
        handler.sendEmptyMessage(1);
    }

    @Override
    public void downloading(int max, int progress) {
        Log.i(TAG, "max: " + max + " --- progress: " + progress);
        if (showNotification) {
            //优化通知栏更新，减少通知栏更新次数
            int curr = (int) (progress / (double) max * 100.0);
            if (curr != lastProgress) {
                lastProgress = curr;
                String downloading = getResources().getString(R.string.start_downloading);
                String content = curr < 0 ? "" : curr + "%";
               showProgressNotification(this, smallIcon, downloading,
                        content, max == -1 ? -1 : 100, curr);
            }
        }
        handler.obtainMessage(2, max, progress).sendToTarget();
    }

    @Override
    public void done(File apk) {
        Log.d(TAG, "done: 文件已下载至" + apk.toString());
        downloadManager.setState(false);
        //如果是android Q（api=29）及其以上版本showNotification=false也会发送一个下载完成通知
        if (showNotification || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String downloadCompleted = getResources().getString(R.string.download_completed);
            String clickHint = getResources().getString(R.string.click_hint);
            showDoneNotification(this, smallIcon, downloadCompleted,
                    clickHint, apk);
        }
        if (jumpInstallPage) {
            installApk(this, apk);
        }
        //如果用户设置了回调 则先处理用户的事件 在执行自己的
        handler.obtainMessage(3, apk).sendToTarget();
    }

    @Override
    public void cancel() {
        downloadManager.setState(false);
        if (showNotification) {
            cancelNotification(this);
        }
        handler.sendEmptyMessage(4);
    }

    @Override
    public void error(Exception e) {
        Log.e(TAG, "error: " + e);
        downloadManager.setState(false);
        if (showNotification) {
            String msg = e.getMessage();
            String downloadError = getResources().getString(R.string.download_error);
            String conDownloading = getResources().getString(R.string.continue_downloading);
            if (!TextUtils.isEmpty(msg) &&
                    msg.contains("android.content.res.XmlResourceParser")) {
                downloadError = getResources().getString(R.string.error_config);
                conDownloading = getResources().getString(R.string.read_readme);
            }
            showErrorNotification(this, smallIcon, downloadError, conDownloading);
        }
        handler.obtainMessage(5, e).sendToTarget();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Toast.makeText(DownloadService.this, R.string.background_downloading, Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    for (OnDownloadListener listener : listeners) {
                        listener.start();
                    }
                    break;
                case 2:
                    for (OnDownloadListener listener : listeners) {
                        listener.downloading(msg.arg1, msg.arg2);
                    }
                    break;
                case 3:
                    for (OnDownloadListener listener : listeners) {
                        listener.done((File) msg.obj);
                    }
                    //执行了完成开始释放资源
                    releaseResources();
                    break;
                case 4:
                    for (OnDownloadListener listener : listeners) {
                        listener.cancel();
                    }
                    break;
                case 5:
                    for (OnDownloadListener listener : listeners) {
                        listener.error((Exception) msg.obj);
                    }
                    break;
                default:
                    break;
            }

        }
    };

    /**
     * 下载完成释放资源
     */
    private void releaseResources() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        stopSelf();
        downloadManager.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * 安装一个apk
     *
     * @param context 上下文
     * @param apk     安装包文件
     */
    private static void installApk(Context context, File apk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".update.provider", apk);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(apk);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /**
     * 创建保存的文件夹
     */
    private static void createDirDirectory(String downloadPath) {
        File dirDirectory = new File(downloadPath);
        if (!dirDirectory.exists()) {
            dirDirectory.mkdirs();
        }
    }

    /**
     * 查看一个文件是否存在
     *
     * @param downloadPath 路径
     * @param fileName     名字
     * @return true | false
     */
    private static boolean fileExists(String downloadPath, String fileName) {
        return new File(downloadPath, fileName).exists();
    }

    /**
     * 获取一个文件的MD5
     *
     * @param file 文件
     * @return MD5
     */
    private static String getFileMD5(File file) {
        try {
            byte[] buffer = new byte[1024];
            int len;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream in = new FileInputStream(file);
            while ((len = in.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
            BigInteger bigInt = new BigInteger(1, digest.digest());
            return bigInt.toString(16).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }



    /**
     * 构建一个消息
     *
     * @param context 上下文
     * @param icon    图标id
     * @param title   标题
     * @param content 内容
     */
    private static NotificationCompat.Builder builderNotification(Context context, int icon, String title, String content) {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = getNotificationChannelId();
        }
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setWhen(System.currentTimeMillis())
                .setContentText(content)
                //不能删除
                .setAutoCancel(false)
                //正在交互（如播放音乐）
                .setOngoing(true);
    }

    /**
     * 显示刚开始下载的通知
     *
     * @param context 上下文
     * @param icon    图标
     * @param title   标题
     * @param content 内容
     */
    private static void showNotification(Context context, int icon, String title, String content) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            afterO(manager);
        }
        NotificationCompat.Builder builder = builderNotification(context, icon, title, content)
                .setDefaults(Notification.DEFAULT_SOUND);
        manager.notify(requireManagerNotNull().getNotifyId(), builder.build());
    }

    /**
     * 显示正在下载的通知
     *
     * @param context 上下文
     * @param icon    图标
     * @param title   标题
     * @param content 内容
     */
    private static void showProgressNotification(Context context, int icon, String title, String content,
                                                int max, int progress) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = builderNotification(context, icon, title, content)
                //indeterminate:true表示不确定进度，false表示确定进度
                //当下载进度没有获取到content-length时，使用不确定进度条
                .setProgress(max, progress, max == -1);
        manager.notify(requireManagerNotNull().getNotifyId(), builder.build());
    }

    /**
     * 显示下载完成的通知,点击进行安装
     *
     * @param context     上下文
     * @param icon        图标
     * @param title       标题
     * @param content     内容
     * @param apk         安装包
     */
    private static void showDoneNotification(Context context, int icon, String title, String content, File apk) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //不知道为什么需要先取消之前的进度通知，才能显示完成的通知。
        manager.cancel(requireManagerNotNull().getNotifyId());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".update.provider", apk);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(apk);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder builder = builderNotification(context, icon, title, content)
                .setContentIntent(pi);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        manager.notify(requireManagerNotNull().getNotifyId(), notification);
    }

    /**
     * 显示下载错误的通知,点击继续下载
     *
     * @param context 上下文
     * @param icon    图标
     * @param title   标题
     * @param content 内容
     */
    private static void showErrorNotification(Context context, int icon, String title, String content) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            afterO(manager);
        }
        Intent intent = new Intent(context, DownloadService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = builderNotification(context, icon, title, content)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(pi)
                .setDefaults(Notification.DEFAULT_SOUND);
        manager.notify(requireManagerNotNull().getNotifyId(), builder.build());
    }

    /**
     * 取消通知
     *
     * @param context 上下文
     */
    private static void cancelNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(requireManagerNotNull().getNotifyId());
    }

    /**
     * 获取通知栏开关状态
     *
     * @return true |false
     */
    private static boolean notificationEnable(Context context) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        return notificationManagerCompat.areNotificationsEnabled();
    }

    /**
     * 适配 Android O 通知渠道
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void afterO(NotificationManager manager) {
        UpdateConfiguration config = requireManagerNotNull();
        NotificationChannel channel = config.getNotificationChannel();
        //如果用户没有设置
        if (channel == null) {
            //IMPORTANCE_LOW：默认关闭声音与震动、IMPORTANCE_DEFAULT：开启声音与震动
            channel = new NotificationChannel(Constant.DEFAULT_CHANNEL_ID, Constant.DEFAULT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            //是否在桌面icon右上角展示小圆点
            channel.enableLights(true);
            //是否在久按桌面图标时显示此渠道的通知
            channel.setShowBadge(true);
            //在Android O 上更新进度 不震动
//            channel.enableVibration(true);
        }
        manager.createNotificationChannel(channel);
    }

    /**
     * 获取设置的通知渠道id
     *
     * @return 如果没有设置则使用默认的 'appUpdate'
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String getNotificationChannelId() {
        NotificationChannel channel = requireManagerNotNull().getNotificationChannel();
        if (channel == null) {
            return Constant.DEFAULT_CHANNEL_ID;
        }
        String channelId = channel.getId();
        if (TextUtils.isEmpty(channelId)) {
            return Constant.DEFAULT_CHANNEL_ID;
        }
        return channelId;
    }

    @NonNull
    private static UpdateConfiguration requireManagerNotNull() {
        if (DownloadManager.getInstance() == null) {
            return new UpdateConfiguration();
        }
        return DownloadManager.getInstance().getConfiguration();
    }
}
