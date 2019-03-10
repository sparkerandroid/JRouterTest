package com.json.router.jroutertest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.json.router.annotation.Route;
import com.json.router.annotation_api.JRouter;
import com.json.router.moduletest.MyService;

@Route(path = "/test/secondactivity")
public class SecondActivity extends Activity {
    private TextView tv_dispatch_outter;
    private TextView tv_dispatch_service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        tv_dispatch_outter = findViewById(R.id.tv_dispatch_outter);
        tv_dispatch_service = findViewById(R.id.tv_dispatch_service);
        tv_dispatch_outter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JRouter.getInstance().route("/module/moduleactivity").navigation(SecondActivity.this);
            }
        });
        tv_dispatch_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyService service = (MyService) JRouter.getInstance().route("/module/myservice").navigation(SecondActivity.this);
                if (service != null) {
                    service.toast(SecondActivity.this);
                }
            }
        });
    }
}
