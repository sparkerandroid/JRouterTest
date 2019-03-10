package com.json.router.moduletest;

import android.app.Activity;
import android.os.Bundle;

import com.json.router.annotation.Route;

@Route(path = "/module/moduleactivity")
public class ModuleActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module);
    }
}
