package com.json.router.annotation.template;

import com.json.router.annotation.meta.RouteMeta;

import java.util.Map;

public interface IRouteGroup {
    // 加载对应的分组路由信息
    // 同一分组group下的所有路由
    // routes.put("/main/mainactivity", RouteMeta.(path, group, destination, rawType, type))
    // routes.put("/main/testactivity", RouteMeta.(path, group, destination, rawType, type))
    void loadRouteInfo(Map<String, RouteMeta> routes);
}
