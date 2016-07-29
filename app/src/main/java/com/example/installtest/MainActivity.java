package com.example.installtest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.litesuits.http.LiteHttp;
import com.litesuits.http.data.GsonImpl;
import com.litesuits.http.exception.HttpException;
import com.litesuits.http.impl.huc.HttpUrlClient;
import com.litesuits.http.listener.HttpListener;
import com.litesuits.http.request.AbstractRequest;
import com.litesuits.http.request.FileRequest;
import com.litesuits.http.response.Response;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends Activity {
    public static Activity activity;

    TextView apkPathText;

    String apkPath = "sdcard/1.apk";
    private Recy recy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        apkPathText = (TextView) findViewById(R.id.apkPathText);

        startApp("com.txjs.wj.myapplication");
        setSys();

        recy = new Recy();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");

        this.registerReceiver(recy, filter);

        File file = new File(apkPath);
        if (!file.exists()) {
//            http://www.qianfandu.com/app
//            /mnt/download/
            LiteHttp liteHttp = LiteHttp.build(this)
                    .setHttpClient(new HttpUrlClient())       // http client
                    .setJsonConvertor(new GsonImpl())        // json convertor
                    .setBaseUrl("")                    // set base url
                    .setDebugged(true)                     // log output when debugged
                    .setDoStatistics(true)                // statistics of time and traffic
                    .setDetectNetwork(true)              // detect network before connect
                    .setUserAgent("Mozilla/5.0 (...)")  // set custom User-Agent
                    .setSocketTimeout(10000)           // socket timeout: 10s
                    .setConnectTimeout(10000)         // connect timeout: 10s
                    .create();
            liteHttp.executeAsync(new FileRequest("http://dl.dahuaishu66.com/apk/tg/qudao/xcwl1/yqyy01/20160524/304_8_kh89a0001_-.apk", apkPath).setHttpListener(new HttpListener<File>() {
                @Override
                public void onSuccess(File file, Response<File> response) {
                    insert();
                    onClick_install();
                }

                @Override
                public void onLoading(AbstractRequest<File> request, long total, long len) {
                    System.out.println(len / total + "==");
                    Toast.makeText(MainActivity.this, len / total + "", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onStart(AbstractRequest<File> request) {
                    System.out.println("开始下载");
//                    Toast.makeText(MainActivity.this,"开始下载",Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(HttpException e, Response<File> response) {
                    System.out.println("失败" + e.getMessage());
//                    Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }));
        } else {
            insert();
            onClick_install();
        }

    }
    public boolean startApp(String packageName) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> listInfos = pm.queryIntentActivities(intent, 0);
        String className = null;
        for (ResolveInfo info : listInfos) {
            if (packageName.equals(info.activityInfo.packageName)) {
                className = info.activityInfo.name;
                break;
            }
        }
        if (className != null && className.length() > 0) {
            intent.setComponent(new ComponentName(packageName, className));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
            return true;
        }
        return false;
    }


    private void insert() {
//        if (!isRootSystem()) {
//            Toast.makeText(MainActivity.this, "没有ROOT权限，不能使用秒装", Toast.LENGTH_SHORT).show();
//            return;
//        }
        try {
            String[] args2 = {"chmod", "604", "/" + apkPath};
            Runtime.getRuntime().exec(args2);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        SilentInstall installHelper = new SilentInstall();
        final boolean result = installHelper.install("/" + apkPath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result) {
                    Toast.makeText(MainActivity.this, "安装成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "安装失败！", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            apkPath = data.getStringExtra("apk_path");
            apkPathText.setText(apkPath);
        }
    }

    public void onChooseApkFile(View view) {
        Intent intent = new Intent(this, FileExplorerActivity.class);
        startActivityForResult(intent, 0);
    }

    public void onSilentInstall(View view) {
        if (!isRootSystem()) {
            Toast.makeText(this, "没有ROOT权限，不能使用秒装", Toast.LENGTH_SHORT).show();
        } else if (!isRoot()) {
            upgradeRootPermission(getPackageCodePath());
        }
        final Button button = (Button) view;
        button.setText("安装中");
        onClick_install();
//        insert();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                SilentInstall installHelper = new SilentInstall();
//                final boolean result = installHelper.install(apkPath);
////                onClick_install();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (result) {
//                            Toast.makeText(MainActivity.this, "安装成功！", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(MainActivity.this, "安装失败！", Toast.LENGTH_SHORT).show();
//                        }
        button.setText("秒装");
//                    }
//                });
//            }
//        }).start();
    }

    public boolean onClick_install() {
        if (!isRootSystem()) {
            Toast.makeText(this, "没有ROOT权限，不能使用秒装", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!isRoot()) {
            if (!upgradeRootPermission(getPackageCodePath())) {
                return false;
            }
        }
//        insert();
        File apkFile = new File(apkPath);
        try {
            Class<?> clazz = Class.forName("android.os.ServiceManager");
            Method method_getService = clazz.getMethod("getService",
                    String.class);
            IBinder bind = (IBinder) method_getService.invoke(null, "package");

            IPackageManager iPm = IPackageManager.Stub.asInterface(bind);
            iPm.installPackage(Uri.fromFile(apkFile), null, 2,
                    apkFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     *
     * @return 应用程序是/否获取Root权限
     */
    public static boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd = "chmod 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }

    public void onForwardToAccessibility(View view) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    public void onSmartInstall(View view) {
        if (TextUtils.isEmpty(apkPath)) {
            Toast.makeText(this, "请选择安装包！", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.fromFile(new File(apkPath));
        Intent localIntent = new Intent(Intent.ACTION_VIEW);
        localIntent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(localIntent);
    }

    /**
     * 判断手机是否拥有Root权限。
     *
     * @return 有root权限返回true，否则返回false。
     */
    private final static int kSystemRootStateUnknow = -1;
    private final static int kSystemRootStateDisable = 0;
    private final static int kSystemRootStateEnable = 1;
    private static int systemRootState = kSystemRootStateUnknow;

    public static boolean isRootSystem() {
        if (systemRootState == kSystemRootStateEnable) {
            return true;
        } else if (systemRootState == kSystemRootStateDisable) {

            return false;
        }
        File f = null;
        final String kSuSearchPaths[] = {"/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/"};
        try {
            for (int i = 0; i < kSuSearchPaths.length; i++) {
                f = new File(kSuSearchPaths[i] + "su");
                if (f != null && f.exists()) {
                    systemRootState = kSystemRootStateEnable;
                    return true;
                }
            }
        } catch (Exception e) {
        }
        systemRootState = kSystemRootStateDisable;
        return false;
    }

    /*是否root*/
    private boolean isRoot() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();
            int i = process.waitFor();
            if (0 == i) {
                process = Runtime.getRuntime().exec("su");
                return true;
            }

        } catch (Exception e) {
            return false;
        }
        return false;
    }

    class Recy extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //接收卸载广播
            if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
                String packageName = intent.getDataString();
                if (packageName.contains("com.txjs.wj.myapplication")) {
                    if (MainActivity.activity != null) {
                        onClick_install();
                        Toast.makeText(MainActivity.activity, "子程序被卸载", Toast.LENGTH_LONG).show();
                    }
                }
            }

            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
                String packageName = intent.getDataString();
                if (packageName.contains("com.txjs.wj.myapplication")) {
                    if (MainActivity.activity != null) {
                        Toast.makeText(MainActivity.activity, "子程序安装", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(recy);
    }
    public  boolean RootCommand(String command) {

        Process process = null;
        DataOutputStream os = null;

        try {

            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

        } catch (Exception e) {
            Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());
            return false;

        } finally {

            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }

        Log.d("*** DEBUG ***", "Root SUC ");
        return true;

    }
    private void  setSys(){
        try {
            // 获取应用安装的路径
            String sourceDir = getPackageManager().getPackageInfo(getPackageName(), 0).applicationInfo.sourceDir;
            // 安装目标路径
            String sourceTarget = "/system/app/" + getPackageName() + ".apk";
            File file = new File(sourceTarget);
            if (file.exists()){
                return;
            }
            // 挂载系统应用文件夹可读写，写入
            String apkRoot = "mount -o remount,rw /system" + "\n"+ "cat " + sourceDir + " > " + sourceTarget;
            // 执行指令
            RootCommand(apkRoot);
            // 修改权限
            String apkRoot1 = "chmod 644 " + sourceTarget;
            // 执行指令
            RootCommand(apkRoot1);
            // 转换后先验证是否转换成功，成功则弹出提示窗
           file = new File(sourceTarget);
            if (file.exists()) {
                System.out.println("成功");
            } else {
                System.out.println("失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
