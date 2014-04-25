package com.jewelzqiu.zjuwlan;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class LoginService extends Service {

    public static final String FORCE_LOGIN = "force_login";

    private static final String TAG = "LoginService";

    private String username;

    private String password;

    private CheckInfoTask mCheckInfoTask = new CheckInfoTask(this);

    private TestWifiAsyncTask mTestWifiTask = new TestWifiAsyncTask();

    private LoginAsyncTask mLoginTask = new LoginAsyncTask();

    public LoginService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mCheckInfoTask.getStatus() == AsyncTask.Status.RUNNING) {
            return START_STICKY;
        }
        if (mCheckInfoTask.getStatus() == AsyncTask.Status.FINISHED) {
            mCheckInfoTask = new CheckInfoTask(this);
        }
        boolean forceLogin = intent.getBooleanExtra(FORCE_LOGIN, false);
        mCheckInfoTask.execute(forceLogin);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class CheckInfoTask extends AsyncTask<Boolean, Void, Boolean> {

        Context mContext;

        public CheckInfoTask(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(Boolean... forceLogin) {
            // check auto login
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (!forceLogin[0]) {
                boolean autoLogin = preferences.getBoolean(getString(R.string.key_autologin), true);
                if (!autoLogin) {
                    stopSelf();
                    return false;
                }
            }

            // check connection info
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (null == networkInfo || networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                return false;
            }

            // get SSID
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            if (null == ssid || ssid.length() < 1) {
                return false;
            }
            if (ssid.charAt(0) == '\"' && ssid.charAt(ssid.length() - 1) == '\"') {
                ssid = ssid.substring(1, ssid.length() - 1);
                if (ssid.length() < 1) {
                    return false;
                }
            }
            if (!NetworkUtil.needAuthorization(mContext, ssid)) {
                return false;
            }

            // username and password
            username = preferences.getString(getString(R.string.key_username), "");
            if (username == null || username.equals("")) {
                return false;
            }
            password = preferences.getString(getString(R.string.key_passowrd), "");
            if (password == null || password.equals("")) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean successful) {
            if (successful) {
                switch (mTestWifiTask.getStatus()) {
                    case FINISHED:
                        mTestWifiTask = new TestWifiAsyncTask();
                    case PENDING:
                        mTestWifiTask.execute();
                        break;
                }
            } else {
                stopSelf();
            }
        }
    }

    private class TestWifiAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            return NetworkUtil.connectedToInternet(NetworkUtil.FIRST_TEST_NETWORK_TIMEOUT);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "Already connected: " + result);
            if (result) {
                System.out.println("already connected");
            } else {
                if (mLoginTask.getStatus() == Status.FINISHED) {
                    mLoginTask = new LoginAsyncTask();
                    mLoginTask.execute();
                } else if (mLoginTask.getStatus() == Status.PENDING) {
                    mLoginTask.execute();
                } else {
                    stopSelf();
                }
            }
        }
    }

    private class LoginAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {

            String content = "action=login&username=" + username +
                    "&password=" + password +
                    "&ac_id=3&is_ldap=1&type=2&local_auth=1";

            Log.d(TAG, "login url: " + NetworkUtil.LOGIN_LOGOUT_URL);
            String loginResult = NetworkUtil.login(NetworkUtil.LOGIN_LOGOUT_URL, content);
            Log.d(TAG, "login result: " + loginResult);

            if ("online_num_error".equals(loginResult)) {
                if (NetworkUtil.forceLogout(username, password)) {
                    // try again
                    loginResult = NetworkUtil.login(NetworkUtil.LOGIN_LOGOUT_URL, content);
                    Log.d(TAG, "try again result: " + loginResult);
                }
            }

            return loginResult;
        }

        @Override
        protected void onPostExecute(String result) {
//            Intent intent;
//            if (result != null && result.equals("login_ok")) {
//                intent = new Intent(MainActivity.ACTION_LOGIN_SUCCESS);
//            } else {
//                intent = new Intent(MainActivity.ACTION_LOGIN_FAILED);
//            }
//            sendBroadcast(intent);
            Toast.makeText(LoginService.this, result, Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }
}
