package com.json.router.jroutertest;

import android.app.Activity;
import android.os.Bundle;

import com.json.router.annotation.Route;

@Route(path = "/test/secondactivity")
public class SecondActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
}
