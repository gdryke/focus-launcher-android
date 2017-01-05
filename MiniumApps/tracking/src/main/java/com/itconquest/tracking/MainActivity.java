package com.itconquest.tracking;

import android.Manifest;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.adeel.library.easyFTP;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.DownloadProgressListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.itconquest.tracking.event.CheckVersionEvent;
import com.itconquest.tracking.event.DownloadApkEvent;
import com.itconquest.tracking.listener.NotificationListener_;
import com.itconquest.tracking.services.ApiClient;
import com.itconquest.tracking.services.GlobalTouchService_;
import com.itconquest.tracking.services.HomePressService;
import com.itconquest.tracking.services.HomePressService_;
import com.itconquest.tracking.services.ScreenOnOffService_;
import com.itconquest.tracking.util.TrackingLogger;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.Subscribe;
import minium.co.core.app.CoreApplication;
import minium.co.core.app.DroidPrefs_;
import minium.co.core.log.Tracer;
import minium.co.core.ui.CoreActivity;
import minium.co.core.util.DateUtils;
import minium.co.core.util.ServiceUtils;
import minium.co.core.util.UIUtils;

@EActivity(resName = "activity_main_tracking")
public class MainActivity extends CoreActivity {

    public final static int REQUEST_CODE = 20;

    @ViewById
    Toolbar toolbar;

    @Pref
    DroidPrefs_ prefs;

    @ViewById
    FloatingActionButton fab;

    @SystemService
    UsageStatsManager usageStatsManager;

    @SystemService
    ConnectivityManager connectivityManager;

    boolean isDisplayedActionNotificationListenerDialog = false;
    boolean isDisplayedPermitUsageAccessDialog = false;

    @AfterViews
    void afterViews() {
        setSupportActionBar(toolbar);
        new TedPermission(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission, app can not provide you the seamless integration.\n\nPlease consider turn on permissions at Setting > Permission")
                .setPermissions(Manifest.permission.SYSTEM_ALERT_WINDOW)
                .check();
        checkVersion();

    }

    private void startServices() {
        if (prefs.isTrackingRunning().get()) {
            GlobalTouchService_.intent(getApplication()).start();
            ScreenOnOffService_.intent(getApplication()).start();
            HomePressService_.intent(getApplication()).start();

        } else {
            getAppUsage();
            GlobalTouchService_.intent(getApplication()).stop();
            ScreenOnOffService_.intent(getApplication()).stop();
            HomePressService_.intent(getApplication()).stop();
        }
    }

    private void getAppUsage() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        List<UsageStats> queryUsageStats = usageStatsManager
                .queryUsageStats(UsageStatsManager.INTERVAL_BEST, cal.getTimeInMillis(),
                        System.currentTimeMillis());

        for (UsageStats stat : queryUsageStats) {

            if (stat.getTotalTimeInForeground() != 0) {

                String info = "Package: " + stat.getPackageName()
                        + " Usage time: " + DateUtils.interval(stat.getTotalTimeInForeground())
                        + " Last time used: " + SimpleDateFormat.getDateTimeInstance().format(new Date(stat.getLastTimeUsed()));

                TrackingLogger.log(info, null);
                Tracer.d(info);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isEnabled(this) && !isDisplayedActionNotificationListenerDialog) {
            isDisplayedActionNotificationListenerDialog = true;
            UIUtils.confirm(this, "Tracker service is not enabled. Please allow Tracking to access notification service", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    dialog.dismiss();
                }
            });
        } else if (getPackageManager().checkPermission(Manifest.permission.PACKAGE_USAGE_STATS, getPackageName()) != PackageManager.PERMISSION_GRANTED && !isDisplayedPermitUsageAccessDialog) {
            isDisplayedPermitUsageAccessDialog = true;
            UIUtils.confirm(this, "Tracker usage stats is not enabled. Please allow Tracking to access usage stats", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    dialog.dismiss();
                }
            });
        }
    }

    /** @return True if {@link NotificationListener_} is enabled. */
    public static boolean isEnabled(Context mContext) {
        return ServiceUtils.isNotificationListenerServiceRunning(mContext, NotificationListener_.class);
    }

    void loadViews() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (prefs.isTrackingRunning().get()) {
                    Snackbar.make(view, "Tracking paused", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    fab.setImageResource(android.R.drawable.ic_media_play);

                } else {
                    Snackbar.make(view, "Tracking started", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                }

                prefs.isTrackingRunning().put(!prefs.isTrackingRunning().get());
                startServices();

            }
        });

        if (prefs.isTrackingRunning().get()) {
            fab.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            fab.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tracking, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_upload) {
            UIUtils.toast(this, "Uploading file...");
            uploadFile();
            uploadFileToAWS();
        }

        return super.onOptionsItemSelected(item);
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            loadViews();
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            UIUtils.toast(MainActivity.this, "Permission denied");
        }
    };

    @Background
    void uploadFile() {
        new ApiClient().uploadFileToFTP();
    }

    @Background
    void uploadFileToAWS() {
        new ApiClient().uploadFileToAWS();
    }

    @Background
    void checkVersion() {
        new ApiClient().checkAppVersion();
    }

    @Subscribe
    public void checkVersionEvent(CheckVersionEvent event) {
        if (event.getVersion() > BuildConfig.VERSION_CODE) {
            downloadApk();
        }
    }

    private void downloadApk() {
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
            new ApiClient().downloadApk();
        }
    }

    @Subscribe
    public void downloadApkEvent(DownloadApkEvent event) {
        try {
            Intent updateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.getPath()));
            startActivity(updateIntent);
        } catch (Exception e) {
            Tracer.e(e, e.getMessage());
        }
    }
}
