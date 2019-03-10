package com.json.router.jroutertest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.json.router.annotation.Route;
import com.json.router.annotation_api.JRouter;

@Route(path = "/main/mainactivity")
public class MainActivity extends AppCompatActivity {

    private TextView tv_dispatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_dispatch = findViewById(R.id.tv_dispatch);
        tv_dispatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JRouter.getInstance().route("/test/secondactivity").navigation(MainActivity.this);
            }
        });
    }
}
