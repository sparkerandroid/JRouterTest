package com.json.router.annotation_api;

import com.json.router.annotation.meta.RouteMeta;
import com.json.router.annotation_api.template.IRouteGroup;
import com.json.router.annotation_api.template.IService;

import java.util.HashMap;
import java.util.Map;

//该类用于将路由入口及路由表文件加载到内存中
public class WareHouse {

    public Map<String, Class<? extends IRouteGroup>> entries;

    public Map<String, RouteMeta> routes;

    public Map<Class, IService> services;

    public WareHouse() {
        entries = new HashMap<>();
        routes = new HashMap<>();
        services = new HashMap<>();
    }
}
