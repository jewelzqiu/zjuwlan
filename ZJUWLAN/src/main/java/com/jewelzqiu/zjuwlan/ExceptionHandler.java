package com.jewelzqiu.zjuwlan;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jewelzqiu on 4/8/14.
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    Context mContext;

    public ExceptionHandler(Context context) {
        mContext = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        ex.printStackTrace(printWriter);
        appendLog(stringWriter.toString());
    }

    public void appendLog(String log) {
        String version = "";
        try {
            PackageManager packageManager = mContext.getPackageManager();
            String packageName = mContext.getPackageName();
            if (packageManager != null) {
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                version = "Version: " + packageInfo.versionName + "\n";
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String date = dateFormat.format(new Date());
        log = version + date + " " + log + "\n";
        try {
            File file = new File(Environment.getExternalStorageDirectory().toString()
                    + "/zjuwlan.txt");
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, false)));
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            if (line != null) {
                if (version.compareTo(line) < 0) {
                    writer.print("");
                }
            }
            reader.close();
            writer.close();
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            writer.append(log);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
