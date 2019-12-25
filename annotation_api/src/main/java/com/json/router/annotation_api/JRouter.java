package com.json.router.annotation_api;

import android.app.Application;
import android.util.Log;

import com.json.router.annotation.meta.RouteMeta;
import com.json.router.annotation.util.TextUtil;
import com.json.router.annotation_api.loadutil.ClassUtils;
import com.json.router.annotation.template.IEntry;
import com.json.router.annotation.template.IRouteGroup;
import com.json.router.annotation.template.IService;

import java.util.Set;

public class JRouter {

    private static volatile JRouter instance;
    private static WareHouse wareHouse;
    private PostCard.Builder routeParamBuilder;

    private JRouter() {
    }

    public static JRouter getInstance() {
        if (instance == null) {
            synchronized (JRouter.class) {
                if (instance == null) {
                    instance = new JRouter();
                }
            }
        }
        return instance;
    }

    //初始化路由，加载路由表入口文件至内存，具体路由文件在使用的时候再加载
    public static void init(Application context) {
        wareHouse = new WareHouse();
        loadIntoEntries(context);
    }

    //加载路由表入口文件至内存
    private static void loadIntoEntries(Application context) {
        try {
            Set<String> routeFiles = ClassUtils.getFileNameByPackageName(context, Const.ROUTE_FILE_DIRECTORY_NAME);
            if (routeFiles != null && routeFiles.size() > 0) {
                for (String fileName : routeFiles) {
                    if (fileName.startsWith(Const.ROUTE_FILE_DIRECTORY_NAME + "." + Const.JROUTER + Const.SEPELATOR + Const.ENTRY + Const.SEPELATOR)) {
                        ((IEntry) Class.forName(fileName).newInstance()).loadRouteEntry(wareHouse.entries);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TAG", "loadIntoEntries fail");
        }
    }

    public PostCard route(String path) {
        String groupName = extractGroupFromPath(path);
        routeParamBuilder = new PostCard.Builder();
        routeParamBuilder.setPath(path);
        routeParamBuilder.setGroup(groupName);
        if (TextUtil.isEmpty(groupName)) {
            return routeParamBuilder.setPath(path).build();
        }
        return preparePostCard().build();
    }

    private PostCard.Builder preparePostCard() {
        Class<? extends IRouteGroup> entryClass = wareHouse.entries.get(routeParamBuilder.getGroup());
        if (entryClass != null) {
            try {
                ((IRouteGroup) Class.forName(entryClass.getName()).newInstance()).loadRouteInfo(wareHouse.routes);
                wareHouse.entries.remove(routeParamBuilder.getGroup());
            } catch (Exception e) {
                e.printStackTrace();
                return routeParamBuilder;
            }
            preparePostCard();
        } else {
            RouteMeta routeMeta = wareHouse.routes.get(routeParamBuilder.getPath());
            if (routeMeta != null) {
                routeParamBuilder.setDestination(routeMeta.getDestination());
                routeParamBuilder.setType(routeMeta.getType());
                switch (routeMeta.getType()) {
                    case ISERVICE:
                        Class<?> destination = routeMeta.getDestination();
                        IService service = wareHouse.services.get(destination);
                        if (null == service) {
                            try {
                                service = (IService) destination.getConstructor().newInstance();
                                wareHouse.services.put(destination, service);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        routeParamBuilder.setService(service);
                        break;
                }
            }
            return routeParamBuilder;
        }
        return routeParamBuilder;
    }

    // 从路由中提取分组名
    private String extractGroupFromPath(String path) {
        if (TextUtil.isEmpty(path) || !path.startsWith("/")) {
            return null;
        }
        // 截取group
        String groupName = path.substring(1, path.indexOf("/", 1));
        return TextUtil.isEmpty(groupName) ? "" : groupName;
    }

}
