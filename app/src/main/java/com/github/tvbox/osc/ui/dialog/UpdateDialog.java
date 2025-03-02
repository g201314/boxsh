package com.github.tvbox.osc.ui.dialog;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.util.AppUpdate;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class UpdateDialog extends BaseDialog {

    TextView txtVerNo;
    TextView txtUpdatedInfo;
    TextView btnUpdateNow;
    TextView btnCancel;
    TextView btnForgetVersion;

    private BaseActivity baseActivity;
    private AppUpdate.VersionInfo versionInfo;

    public UpdateDialog(@NonNull @NotNull Context context, AppUpdate.VersionInfo versionInfo) {
        super(context);
        baseActivity = (BaseActivity)context;
        setContentView(R.layout.dialog_update);
        this.versionInfo = versionInfo;
        txtVerNo = findViewById(R.id.txt_ver_no);
        txtUpdatedInfo = findViewById(R.id.txt_updated_info);
        btnUpdateNow = findViewById(R.id.btn_update);
        btnCancel = findViewById(R.id.btn_cancel);
        btnForgetVersion = findViewById(R.id.btn_next_time);
        txtVerNo.setText(versionInfo.VersionNo);
        txtUpdatedInfo.setText(versionInfo.UpdatedInfo);
        bindBtnEvent();
    }

    public void bindBtnEvent() {
        View.OnFocusChangeListener changeColor = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                Drawable drawable = b ?
                        getContext().getResources().getDrawable(R.drawable.button_detail_quick_search):
                        getContext().getResources().getDrawable(R.drawable.button_dialog_main);
                view.setBackground(drawable);
            }
        };
        btnUpdateNow.setOnFocusChangeListener(changeColor);
        btnCancel.setOnFocusChangeListener(changeColor);
        btnForgetVersion.setOnFocusChangeListener(changeColor);
        btnUpdateNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startUpdate();
                UpdateDialog.this.cancel();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpdateDialog.this.cancel();
            }
        });
        btnForgetVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Hawk.put(HawkConfig.FORGET_NEW_VERSION, versionInfo.VersionNo);
                UpdateDialog.this.cancel();
            }
        });
    }

    private void startUpdate() {
        if(this.versionInfo.VersionNo == null || this.versionInfo.VersionNo.length() <= 0)
            return;

        if(Build.VERSION.SDK_INT >= 23) {
            if (App.getInstance().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(baseActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                this.dismiss();
                return;
            }
        }

        String packageName = this.getContext().getPackageName();
        File sdcardDir = Environment.getExternalStorageDirectory(); // /storage/emulated/0/
        String appApkDir = sdcardDir + File.separator + "Android" + File.separator + "data" + File.separator + packageName + File.separator;
        String destinationStr = appApkDir + "update/update.apk";
        File cache = new File(destinationStr);
        File cacheDir = cache.getParentFile();
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        if (cache.exists())
            cache.delete();

        String url = this.versionInfo.Source + this.versionInfo.VersionNo + ".apk";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("FlymeTV 更新版本 " + this.versionInfo.VersionNo + " 来自Github");
        request.setTitle("FlymeTV");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        Uri destination = FileProvider
                .getUriForFile(baseActivity.getBaseContext(), baseActivity.getPackageName() + ".fileprovider", cache);
        request.setDestinationUri(Uri.fromFile(cache));

        final DownloadManager manager = (DownloadManager) baseActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(baseActivity, "下载完成，准备安装新版本", Toast.LENGTH_SHORT).show();
                try {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                    install.setDataAndType(destination, "application/vnd.android.package-archive");
                    baseActivity.startActivity(install);

                    baseActivity.unregisterReceiver(this);
                    baseActivity.finish();
                }catch (Exception ex) {
                    Toast.makeText(baseActivity, "错误：" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        baseActivity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Toast.makeText(baseActivity, "正在下载新版本...", Toast.LENGTH_SHORT).show();
        this.dismiss();
    }

}
