package com.json.router.jroutertest;

import android.app.Application;

import com.json.router.annotation_api.JRouter;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        JRouter.init(this);
    }
}
