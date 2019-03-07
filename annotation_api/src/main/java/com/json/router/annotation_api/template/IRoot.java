package com.json.router.annotation_api.template;

import java.util.Map;

public interface IRoot {
    // 加载路由入口文件
    // 比如：entries.put("main", JRouterTest_Group_main.class);其中JRouterTest_Group_main是group为main的路由表文件
    void loadRouteEntry(Map<String, Class<? extends IRouteGroup>> entries);
}
