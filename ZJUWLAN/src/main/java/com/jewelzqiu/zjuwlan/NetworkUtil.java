package com.jewelzqiu.zjuwlan;

import android.content.Context;
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

    public static final long LOGIN_TIMEOUT = 4000;
    public static final long LOGOUT_TIMEOUT = 4000;
    public static final long FIRST_TEST_NETWORK_TIMEOUT = 3000;
    public static final long SECOND_TEST_NETWORK_TIMEOUT = 3000;
    public static final String LoginURL = "http://10.50.200.245/cgi-bin/srun_portal";
    public static final String TestURL = "http://www.apple.com/library/test/success.html";

    private static String HttpResponse;

    public static boolean needAuthorization(Context context, String ssid) {
        String[] authorizationWifis = context.getResources().getStringArray(
                R.array.authorization_wifis);
        for (String wifi : authorizationWifis) {
            if (wifi.equalsIgnoreCase(ssid)) {
                return true;
            }
        }
        return false;
    }

    public static String login(String urlStr, String content) {
        String loginResult = getHttpResponse(urlStr, "POST", content, 3000, 3000, LOGIN_TIMEOUT);
        if (null == loginResult) {
            return null;
        }

        if (loginResult.contains("login_ok")) {
            loginResult = "login_ok";
        }

        if (!loginResult.equals("login_ok")) {
            // Maybe there are bugs in the system of  wireless network
            // authorization, that is, we can connect to Internet when login
            // result is not OK.
            if (NetworkUtil.connectedToInternet(SECOND_TEST_NETWORK_TIMEOUT)) {
                loginResult = "login_ok";
            }
        }
        return loginResult;
    }

    public static boolean forceLogout(String username, String password) {
        String content = "action=logout&uid=-1&username=" + username +
                "&password=" + password + "&force=1&type=2";
        String result = getHttpResponse(LoginURL, "POST", content, 5000, 3000, LOGOUT_TIMEOUT);
        Log.d(TAG, "force logout res: " + result);
        return "logout_ok".equals(result);
    }

    public static boolean connectedToInternet(long timeout) {
        boolean networkAvailable = false;
        String responeText = getHttpResponse(TestURL, "GET", null, 5000, 3000, timeout);
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
