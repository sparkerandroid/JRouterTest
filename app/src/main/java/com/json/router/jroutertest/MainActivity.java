package com.json.router.jroutertest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.json.router.annotation.Route;

@Route(path = "/main/mainactivity")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
