package com.jewelzqiu.zjuwlan;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.SyncStateContract;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jewelzqiu on 11/7/13.
 */
public class MainFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MainFragment";
    private static final long LOGIN_TIMEOUT = 4000;
    private static final long LOGOUT_TIMEOUT = 4000;
    private static final long FIRST_TEST_NETWORK_TIMEOUT = 3000;
    private static final long SECOND_TEST_NETWORK_TIMEOUT = 3000;

    private Context mContext;

    private Preference ssidPref;
    private Preference loginPref;
    private EditTextPreference usernamePref;
    private EditTextPreference passwordPref;
    private SwitchPreference autoLoginPref;

    private String username;
    private String password;
    private String ssid;

    private WifiReceiver wifiReceiver;

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;

    private final int MSG_TO_LOGIN = 0;
    private final int MSG_LOGOUT_RESULT = 1;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TO_LOGIN:
                    Log.d(TAG, "username: " + username);
                    Log.d(TAG, "password: " + password);
                    new LoginAsyncTask().execute(ssid, username, password);
                    break;
                case MSG_LOGOUT_RESULT:
                    String res = (String) msg.obj;
                    Log.d(TAG, "logout result: " + res);
                    if (null != res) {
                        if (res.equals("logout_ok")) {
                            res = getString(R.string.login_ok);
                        }
                    } else {
                        res = getString(R.string.login_fail);
                    }
                    Toast.makeText(mContext, res, Toast.LENGTH_SHORT).show();
                    onLogout();
                    break;
            }
        }
    };

    public MainFragment() {

    }

    public MainFragment(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_main);
        ssidPref = findPreference(getResources().getString(R.string.key_ssid));
        loginPref = findPreference(getResources().getString(R.string.key_login_logout));
        usernamePref = (EditTextPreference) findPreference(
                getResources().getString(R.string.key_username)
        );
        passwordPref = (EditTextPreference) findPreference(
                getResources().getString(R.string.key_passowrd)
        );
        autoLoginPref = (SwitchPreference) findPreference(
                getResources().getString(R.string.key_autologin)
        );

        ssidPref.setOnPreferenceClickListener(new SSIDListener());
        loginPref.setOnPreferenceClickListener(new LoginLogoutListener());
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        connectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        update(true);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiReceiver = new WifiReceiver();
        mContext.registerReceiver(wifiReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        mContext.unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        update(true);
    }

    private void update(boolean connect) {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        // auto-login
        boolean autoLogin = sharedPreferences.getBoolean(
                getResources().getString(R.string.key_autologin), true
        );
        autoLoginPref.setChecked(autoLogin);

        // username
        username = sharedPreferences.getString(
                getResources().getString(R.string.key_username), ""
        );
        usernamePref.setSummary(username);

        //password
        password = sharedPreferences.getString(
                getResources().getString(R.string.key_passowrd), ""
        );
        char[] charArray = password.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            charArray[i] = '*';
        }
        passwordPref.setSummary(new String(charArray));

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (null == networkInfo ||
                networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            ssidPref.setTitle(R.string.no_ssid);
            loginPref.setEnabled(false);
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        ssid = wifiInfo.getSSID();
        if (null == ssid || ssid.length() < 1) {
            return;
        }

        // if SSID is surrounded by double quotation marks, remote them to
        // simplify the processing logic
        if (ssid.charAt(0) == '\"' && ssid.charAt(ssid.length() - 1) == '\"') {
            ssid = ssid.substring(1, ssid.length() - 1);
            if (ssid.length() < 1) {
                return;
            }
        }
        Log.v(TAG, "connect to: " + ssid);
        ssidPref.setTitle(getResources().getString(R.string.ssid) + ssid);

        if (!needAuthorization(ssid)) {
            loginPref.setEnabled(false);
            return;
        }

        loginPref.setEnabled(true);

        if (connect) {
            new TestWifiAsyncTask().execute(ssid);
        }
    }

    private boolean needAuthorization(String ssid) {
        String[] authorizationWifis = this.getResources().getStringArray(
                R.array.authorization_wifis);
        for (String wifi : authorizationWifis) {
            if (wifi.equalsIgnoreCase(ssid)) {
                return true;
            }
        }
        return false;
    }

    private void connect(String ssid) {
        SharedPreferences pref = getPreferenceManager().getSharedPreferences();
        boolean loginOnConnect =
                pref.getBoolean(getResources().getString(R.string.key_autologin), true);
        if (!loginOnConnect) {
            return;
        }

        requestAuthorization(ssid);
    }

    private void requestAuthorization(String ssid) {
        if (username == null || username.equals("")) {
            Toast.makeText(mContext, "请先输入帐号密码", Toast.LENGTH_SHORT);
            return;
        }
        new LoginAsyncTask().execute(ssid, username, password);
    }

    private void onLogin() {
        Log.d(TAG, "login ok");
        Toast.makeText(mContext, R.string.login_ok, Toast.LENGTH_SHORT);
        loginPref.setTitle(getResources().getString(R.string.logout));
        loginPref.setEnabled(true);
    }

    private void logout() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String urlStr =
                        "http://10.50.200.245/cgi-bin/srun_portal";
                String content = "action=logout&type=2";
                String response
                        = NetworkUtil.getHttpResponse(urlStr, "POST", content, 2000, 2000);
                Message msg = new Message();
                msg.what = MSG_LOGOUT_RESULT;
                msg.obj = response;
                handler.sendMessage(msg);
            }
        }).start();
    }

    private void onLogout() {
        loginPref.setTitle(getString(R.string.login));
        loginPref.setEnabled(true);
    }

    private class SSIDListener implements Preference.OnPreferenceClickListener {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            mContext.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            return true;
        }
    }

    private class LoginLogoutListener implements Preference.OnPreferenceClickListener {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String text = preference.getTitle().toString();
            if (text.equals(getString(R.string.login))) {
                new TestWifiAsyncTask().execute(ssid);
            } else if (text.equals(getString(R.string.logout))) {
                logout();
            }
            return true;
        }
    }

    private class TestWifiAsyncTask extends AsyncTask<String, Void, Boolean> {

        private String ssid;

        @Override
        protected Boolean doInBackground(String... params) {
            ssid = params[0];
            return NetworkUtil.connectedToInternet(FIRST_TEST_NETWORK_TIMEOUT);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "Already connected: " + result);
            if (result) {
                onLogin();
            } else {
                connect(ssid);
            }
        }
    }

    class LoginAsyncTask extends AsyncTask<String, Integer, String> {
        private String ssid;

//        @Override
//        protected void onPreExecute() {
//            if (mDialog == null) {
//                mDialog = new ProgressDialog(WifiLoginActivity.this, 0);
//            }
//            mDialog.setMessage(WifiLoginActivity.this.getString(
//                    R.string.logining));
//            mDialog.setIndeterminate(true);
//            if (null != WifiLoginActivity.this
//                    && !WifiLoginActivity.this.isFinishing()) {
//                if (!mDialog.isShowing()) {
//                    mDialog.show();
//                }
//            }
//        }

        @Override
        protected String doInBackground(String... params) {
            ssid = params[0];
            String username = params[1];
            String password = params[2];

            String urlStr = "http://10.50.200.245/cgi-bin/srun_portal";
            String content = "action=login&username=" + username +
                    "&password="  + password +
                    "&ac_id=3&is_ldap=1&type=2&local_auth=1";

            Log.d(TAG, "login url: " + urlStr);
            String loginResult = login(urlStr, content);

            if ("online_num_error".equals(loginResult)) {
                if (forceLogout(username, password)) {
                    // try again
                    loginResult = login(urlStr, content);
                    Log.d(TAG, "try again result: " + loginResult);
                }
            }

            return loginResult;
        }

        private String login(String urlStr, String content) {
            String loginResult = NetworkUtil.getHttpResponse(
                    urlStr, "POST", content, 3000, 3000, LOGIN_TIMEOUT);
            if (null == loginResult)
                return null;

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

        private boolean forceLogout(String username, String password) {
            String urlStr =
                    "http://10.50.200.245/cgi-bin/srun_portal";
            String content = "action=logout&uid=-1&username=" + username +
                    "&password=" + password + "&force=1&type=2";
            String result = NetworkUtil.getHttpResponse(urlStr, "POST", content, 5000, 3000,
                    LOGOUT_TIMEOUT);
            Log.d(TAG, "force logout res: " + result);
            return "logout_ok".equals(result);
        }

//        @Override
//        protected void onProgressUpdate(Integer... progress) {
//            mDialog.setMessage(
//                    WifiLoginActivity.this.getString(R.string.testing_wifi));
//        }

        @Override
        protected void onPostExecute(String result) {
            if (null != result && result.equals("login_ok")) {
                onLogin();
            } else {
                Toast.makeText(mContext, result, Toast.LENGTH_SHORT);
            }
//            if (null != WifiLoginActivity.this
//                    && !WifiLoginActivity.this.isFinishing()) {
//                mDialog.dismiss();
//            }
        }
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Handler handler = new Handler();
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        update(false);
                    }
                }, 200);
            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        update(true);
                    }
                }, 200);
            }
        }
    }
}
