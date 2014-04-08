package com.jewelzqiu.zjuwlan;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment(this))
                    .commit();
        }

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
    }
}
