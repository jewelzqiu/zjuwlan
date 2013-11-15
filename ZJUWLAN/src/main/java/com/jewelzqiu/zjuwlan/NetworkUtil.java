package com.jewelzqiu.zjuwlan;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jewelzqiu on 11/7/13.
 */
public class NetworkUtil {

    private static final String TAG = "MainFragment";
    private static String HttpResponse;

    public static boolean connectedToInternet(long timeout) {
        boolean networkAvailable = false;
        String url = "http://www.apple.com/library/test/success.html";
        String responeText = getHttpResponse(url, "GET", null, 5000, 3000, timeout);
        Log.d(TAG, "test network: " + responeText);
        if (null != responeText && responeText.contains("Success")) {
            networkAvailable = true;
        }
        return networkAvailable;
    }

    public synchronized static String getHttpResponse(final String urlStr,
                                                      final String method, final String content,
                                                      final int connectTimeout, final int readTimeout,
                                                      final long timeout) {
        HttpResponse = null;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                HttpResponse = getHttpResponse(
                        urlStr, method, content, connectTimeout, readTimeout);
            }
        });
        thread.start();
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        thread.interrupt();

        return HttpResponse;
    }

    public static String getHttpResponse(String urlStr, String method, String content,
                                          int connectTimeout, int readTimeout) {
        HttpURLConnection conn = null;
        InputStream in = null;
        String res = "";
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setReadTimeout(readTimeout);
            conn.connect();
            if ("POST".equals(method)) {
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(content);
                dos.flush();
                dos.close();
            }

            in = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while (null != (line = br.readLine())) {
                res += line;
            }

            Log.v(TAG, "res: " + res);
        } catch (MalformedURLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            res = null;
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            res = null;
        } finally {
            if (null != conn)
                conn.disconnect();
            if (null != in)
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
        }

        return res;
    }

}
