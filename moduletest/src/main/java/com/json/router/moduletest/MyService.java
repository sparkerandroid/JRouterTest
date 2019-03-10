package com.json.router.moduletest;

import android.app.Activity;
import android.widget.Toast;

import com.json.router.annotation.Route;
import com.json.router.annotation_api.template.IService;

@Route(path = "/module/myservice")
public class MyService implements IService {

    public void toast(Activity activity) {
        Toast.makeText(activity, "MyService", Toast.LENGTH_SHORT).show();
    }
}
