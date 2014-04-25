package com.jewelzqiu.zjuwlan;

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
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jewelzqiu on 11/7/13.
 */
public class MainFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MainFragment";

    private Context mContext;

    private Preference ssidPref;

    private Preference loginPref;

    private EditTextPreference usernamePref;

    private EditTextPreference passwordPref;

    private SwitchPreference autoLoginPref;

    private String username;

    private String password;

    private String ssid;

    private boolean autoLogin;

    private WifiReceiver wifiReceiver;

    private WifiManager wifiManager;

    private ConnectivityManager connectivityManager;

    private TestWifiAsyncTask testWifiTask = new TestWifiAsyncTask();

    private LoginAsyncTask loginTask = new LoginAsyncTask();

    private Handler handler = new Handler();

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
        update();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiReceiver = new WifiReceiver();
        mContext.registerReceiver(wifiReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        mContext.unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        update();
    }

    private void update() {
        PreferenceManager preferenceManager = getPreferenceManager();
        if (preferenceManager == null) {
            return;
        }
        SharedPreferences sharedPreferences = preferenceManager.getSharedPreferences();
        if (sharedPreferences == null) {
            return;
        }

        // auto-login
        autoLogin = sharedPreferences.getBoolean(getString(R.string.key_autologin), true);
        autoLoginPref.setChecked(autoLogin);

        // username
        username = sharedPreferences.getString(getString(R.string.key_username), "");
        usernamePref.setSummary(username);

        //password
        password = sharedPreferences.getString(getString(R.string.key_passowrd), "");
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

        if (!NetworkUtil.needAuthorization(mContext, ssid)) {
            loginPref.setEnabled(false);
            return;
        }

        loginPref.setEnabled(true);

        if (testWifiTask.getStatus() == AsyncTask.Status.FINISHED) {
            testWifiTask = new TestWifiAsyncTask();
            testWifiTask.execute(false);
        } else if (testWifiTask.getStatus() == AsyncTask.Status.PENDING) {
            testWifiTask.execute(false);
        }
    }

    private void onLogin() {
        Log.d(TAG, "login ok");
        Toast.makeText(mContext, R.string.login_ok, Toast.LENGTH_SHORT).show();
        loginPref.setTitle(R.string.logout);
        loginPref.setEnabled(true);
    }

    private void onLogout() {
        loginPref.setTitle(R.string.login);
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
            String text = (String) loginPref.getTitle();
            if (text == null) {
                return true;
            }
            if (text.equals(getString(R.string.login))) {
                if (testWifiTask.getStatus() == AsyncTask.Status.FINISHED) {
                    testWifiTask = new TestWifiAsyncTask();
                    testWifiTask.execute(true);
                } else if (testWifiTask.getStatus() == AsyncTask.Status.PENDING) {
                    testWifiTask.execute(true);
                }
            } else if (text.equals(getString(R.string.logout))) {
                new LogoutAsyncTask().execute();
            }
            return true;
        }
    }

    private class TestWifiAsyncTask extends AsyncTask<Boolean, Void, Boolean> {

        private boolean connect;

        @Override
        protected Boolean doInBackground(Boolean... params) {
            connect = params[0];
            return NetworkUtil.connectedToInternet(NetworkUtil.FIRST_TEST_NETWORK_TIMEOUT);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "Already connected: " + result);
            if (result) {
                onLogin();
            } else {
                loginPref.setTitle(R.string.login);
                if (autoLogin || connect) {
                    if (username == null || username.equals("")) {
                        Toast.makeText(mContext, "请先输入帐号密码", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (loginTask.getStatus() == AsyncTask.Status.FINISHED) {
                        loginTask = new LoginAsyncTask();
                        loginTask.execute();
                    } else if (loginTask.getStatus() == AsyncTask.Status.PENDING) {
                        loginTask.execute();
                    }
                }
            }
        }
    }

    private class LoginAsyncTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {

            String content = "action=login&username=" + username +
                    "&password=" + password +
                    "&ac_id=3&is_ldap=1&type=2&local_auth=1";

            Log.d(TAG, "login url: " + NetworkUtil.LOGIN_LOGOUT_URL);
            String loginResult = NetworkUtil.login(NetworkUtil.LOGIN_LOGOUT_URL, content);

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
            if (null != result && result.equals("login_ok")) {
                onLogin();
            } else {
                Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class LogoutAsyncTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String content = "action=logout&type=2";
            return NetworkUtil
                    .getHttpResponse(NetworkUtil.LOGIN_LOGOUT_URL, "POST", content, 2000, 2000);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "logout result: " + result);
            if (null != result) {
                if (result.equals("logout_ok")) {
                    result = getString(R.string.logout_ok);
                }
            } else {
                result = getString(R.string.logout_fail);
            }
            Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
            onLogout();
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
                        update();
                    }
                }, 200);
            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                }, 200);
            }
        }
    }
}
