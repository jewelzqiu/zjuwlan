package com.jewelzqiu.zjuwlan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jewelzqiu on 11/18/13.
 */
public class WiFiReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiReceiver";

    private static String ssid;
    private static String username;
    private static String password;

    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent.getAction());
        // autologin
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        boolean autoLogin = sharedPreferences.getBoolean(
                context.getString(R.string.key_autologin), true
        );
        if (!autoLogin) {
            return;
        }

        // username and password
        username = sharedPreferences.getString(context.getString(R.string.key_username), "");
        if (username == null || username.equals("")) {
            return;
        }
        password = sharedPreferences.getString(context.getString(R.string.key_passowrd), "");

        // check connection info
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (null == networkInfo ||
                networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            return;
        }

        // get SSID
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        ssid = wifiInfo.getSSID();
        if (null == ssid || ssid.length() < 1) {
            return;
        }
        if (ssid.charAt(0) == '\"' && ssid.charAt(ssid.length() - 1) == '\"') {
            ssid = ssid.substring(1, ssid.length() - 1);
            if (ssid.length() < 1) {
                return;
            }
        }
        if (!NetworkUtil.needAuthorization(context, ssid)) {
            return;
        }

        new TestWifiAsyncTask().execute();
    }

    private class TestWifiAsyncTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            return NetworkUtil.connectedToInternet(NetworkUtil.FIRST_TEST_NETWORK_TIMEOUT);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "Already connected: " + result);
            if (result) {
                System.out.println("already connected");
            } else {
                new LoginAsyncTask().execute();
            }
        }
    }

    class LoginAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {

            String content = "action=login&username=" + username +
                    "&password="  + password +
                    "&ac_id=3&is_ldap=1&type=2&local_auth=1";

            Log.d(TAG, "login url: " + NetworkUtil.LoginURL);
            String loginResult = NetworkUtil.login(NetworkUtil.LoginURL, content);

            if ("online_num_error".equals(loginResult)) {
                if (NetworkUtil.forceLogout(username, password)) {
                    // try again
                    loginResult = NetworkUtil.login(NetworkUtil.LoginURL, content);
                    Log.d(TAG, "try again result: " + loginResult);
                }
            }

            return loginResult;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(result);
        }
    }
}
