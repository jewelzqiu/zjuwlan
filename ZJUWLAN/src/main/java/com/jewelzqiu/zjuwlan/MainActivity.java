package com.jewelzqiu.zjuwlan;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    public static final String ACTION_LOGIN_SUCCESS = "com.jewelzqiu.zjuwlan.loginsuccess";

    public static final String ACTION_LOGIN_FAILED = "com.jewelzqiu.zjuwlan.loginfailed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }

//        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
    }
}
