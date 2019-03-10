package com.json.router.annotation_api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.json.router.annotation.meta.TypeEnum;

public class PostCard {

    private String path;
    private String group;
    private Class<?> destination;
    private TypeEnum type;

    private int requestCode;
    private Bundle extra;

    public PostCard(Builder builder) {
        if (builder != null) {
            this.path = builder.path;
            this.group = builder.group;
            this.requestCode = builder.requestCode;
            this.destination = builder.destination;
            this.extra = builder.extra;
            this.type = builder.type;
        }
    }

    public static class Builder {

        private String path;

        public String getPath() {
            return path;
        }

        public String getGroup() {
            return group;
        }

        private String group;
        private Class<?> destination;
        private TypeEnum type;

        private int requestCode;
        private Bundle extra;

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setGroup(String group) {
            this.group = group;
            return this;
        }

        public Builder setRequestCode(int requestCode) {
            this.requestCode = requestCode;
            return this;
        }

        public Builder setDestination(Class<?> destination) {
            this.destination = destination;
            return this;
        }

        public Builder setType(TypeEnum type) {
            this.type = type;
            return this;
        }

        public Builder setExtra(Bundle extra) {
            if (extra != null && extra.size() > 0) {
                this.extra = extra;
            }
            return this;
        }

        public PostCard build() {
            return new PostCard(this);
        }

    }

    public void navigation(Activity activity) {
        if (activity != null) {
            switch (type) {
                case ACTIVITY:
                    Intent intent = new Intent(activity, destination);
                    activity.startActivity(intent);
                    break;
            }
        }
    }

}
