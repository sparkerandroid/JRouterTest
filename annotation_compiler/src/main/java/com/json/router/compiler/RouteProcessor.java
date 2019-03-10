package com.json.router.compiler;

import com.google.auto.service.AutoService;
import com.json.router.annotation.Route;
import com.json.router.annotation.meta.RouteMeta;
import com.json.router.annotation.meta.TypeEnum;
import com.json.router.compiler.util.TextUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedOptions({Const.MODULE_NAME})
public class RouteProcessor extends AbstractProcessor {

    private Messager log;
    private Elements elements;
    private Types types;
    private Filer filer;

    private TypeMirror typeMirror_Activity;
    private TypeMirror typeMirror_Fragment_V4;
    private TypeMirror typeMirror_Fragment;
    private TypeMirror typeMirror_Service;

    private String moduleName;

    private Map<String, Set<RouteMeta>> group;//路由分组
    private Map<String, String> entries;//路由入口

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        log = processingEnvironment.getMessager();
        elements = processingEnvironment.getElementUtils();
        types = processingEnvironment.getTypeUtils();
        filer = processingEnvironment.getFiler();

        typeMirror_Activity = elements.getTypeElement(Const.ACTIVITY).asType();
        typeMirror_Fragment_V4 = elements.getTypeElement(Const.FRAGMENT_V4).asType();
        typeMirror_Fragment = elements.getTypeElement(Const.FRAGMENT).asType();
        typeMirror_Service = elements.getTypeElement(Const.ISERVICE).asType();

        Map<String, String> options = processingEnvironment.getOptions();
        if (options != null && !options.isEmpty()) {
            moduleName = options.get(Const.MODULE_NAME);
        }
        if (moduleName == null || moduleName.length() == 0) {
            log.printMessage(Diagnostic.Kind.NOTE, "moduleName can't be null or empty");
            throw new RuntimeException("moduleName can't be null or empty");
        }
        group = new HashMap<>();
        entries = new HashMap<>();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Route.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set == null || set.isEmpty()) {
            log.printMessage(Diagnostic.Kind.NOTE, "process set is empty");
            return false;
        }
        Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
        if (routeElements == null || routeElements.isEmpty()) {
            log.printMessage(Diagnostic.Kind.NOTE, "process roundEnvironment is empty");
            return false;
        }

        extractAndGroupRouteInfo(routeElements);    //提取&分组路由信息

        // 生成路由表文件
        if (group == null || group.size() == 0) {//无路由表信息
            return false;
        }
        generateRouteGroupFile();

        //生成路由入口文件
        if (entries == null || entries.size() <= 0) {
            return false;
        }
        generateRouteEntryFile();
        return true;
    }

    //提取&分组路由信息
    private void extractAndGroupRouteInfo(Set<? extends Element> routeElements) {
        log.printMessage(Diagnostic.Kind.NOTE, "start to classify route info");
        for (Element ele : routeElements) { //遍历&路由分组
            TypeMirror annoEle = ele.asType();
            if (ele.getKind() == ElementKind.CLASS) {// Route注解只能使用在Class类上，如interface等类型不可使用
                Route route = ele.getAnnotation(Route.class);
                String path = route.path();//获取路由
                String groupName = extractGroupFromPath(path);//提取分组
                RouteMeta routeMeta = new RouteMeta(path, groupName, ele, getElementKind(annoEle));//封装路由信息
                Set<RouteMeta> groupRoutes = group.get(groupName);
                if (groupRoutes == null) {
                    groupRoutes = new HashSet<>();
                    groupRoutes.add(routeMeta);
                    group.put(groupName, groupRoutes); // 路由分组
                } else {
                    groupRoutes.add(routeMeta);
                }
            }
        }
        log.printMessage(Diagnostic.Kind.NOTE, "classify route info  -- finish -- group size is " + group.size());
        log.printMessage(Diagnostic.Kind.NOTE, "start to generate route group file");
    }

    //生成路由表文件
    private void generateRouteGroupFile() {
        //1、生成参数类型:Map<String, RouteMeta>
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouteMeta.class)
        );

        //2、生成参数：routes
        ParameterSpec parameterSpec = ParameterSpec.builder(parameterizedTypeName,
                "routes")
                .build();

        //3、遍历路由分组（Map<String, Set<RouteMeta>> group），生成路由表文件
        // String是分组名，每一个Set<RouteMeta>都是一个路由表文件
        //------------------------------------------------------------
        //public class app_Group_main implements IRouteGroup {
        //  public void loadRouteInfo(Map<String, RouteMeta> routes) {
        //    routes.put("/main/mainactivity",RouteMeta.build("/main/mainactivity","main",MainActivity.class,TypeEnum.ACTIVITY));
        //  }
        //}
        for (Map.Entry<String, Set<RouteMeta>> routeEntry : group.entrySet()) {
            String groupName = routeEntry.getKey();
            MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(Const.LOAD_ROUTE_INFO)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(parameterSpec);

            for (RouteMeta routeMeta : routeEntry.getValue()) {//遍历&添加统一分组下的路由信息
                // routes.put("/main/mainactivity", RouteMeta.build(path, group, destination, rawType, type))
                methodSpec.addStatement("routes.put($S,$T.build($S,$S,$T.class,$T." + routeMeta.getType() + "))",
                        routeMeta.getPath(),
                        ClassName.get(RouteMeta.class),
                        routeMeta.getPath(),
                        routeMeta.getGroup(),
                        ClassName.get(routeMeta.getRawType().asType()),
                        ClassName.get(TypeEnum.class));
            }
            //路由文件名
            String groupFileName = moduleName + "_Group_" + groupName;
            TypeSpec typeSpec = TypeSpec.classBuilder(groupFileName)
                    .addSuperinterface(ClassName.get(Const.TEMPLATE, Const.IROUTEGROUP))
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc(Const.WARN)
                    .addMethod(methodSpec.build())
                    .build();

            JavaFile javaFile = JavaFile.builder(Const.ROUTE_FILE_DIRECTORY_NAME, typeSpec).build();
            try {
                javaFile.writeTo(filer);
            } catch (Exception e) {
                log.printMessage(Diagnostic.Kind.ERROR, "generate route group file exception");
            }

            if (entries == null) {
                entries = new HashMap<>();
            }
            entries.put(groupName, groupFileName);//保存生成的路由文件信息，作为之后的查找入口
        }
    }

    //生成路由入口文件
    private void generateRouteEntryFile() {
        //4、生成路由入口文件:void loadRouteEntry(Map<String, Class<? extends IRouteGroup>> entries);
        //4.1、生成参数类型
        ParameterizedTypeName parameterizedTypeName1 = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(Const.TEMPLATE, Const.IROUTEGROUP))
                )
        );
        //4.2、生成参数
        ParameterSpec parameterSpec1 = ParameterSpec.builder(parameterizedTypeName1, "entries").build();

        //4.3、生成方法
        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(Const.LOADROUTEENTRY)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameterSpec1);

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            //entries.put("main", JRouterTest_Group_main.class);
            methodSpec.addStatement("entries.put($S,$T.class)",
                    entry.getKey(),
                    ClassName.get(Const.ROUTE_FILE_DIRECTORY_NAME, entry.getValue()));
        }
        // 4.5、生成入口文件

        TypeSpec typeSpec = TypeSpec.classBuilder(Const.JROUTER + Const.SEPELATOR + Const.ENTRY + Const.SEPELATOR + moduleName)
                .addMethod(methodSpec.build())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(Const.WARN)
                .addSuperinterface(ClassName.get(Const.TEMPLATE, Const.IENTRY))
                .build();

        JavaFile javaFile = JavaFile.builder(Const.ROUTE_FILE_DIRECTORY_NAME, typeSpec)
                .build();

        try {
            javaFile.writeTo(filer);
        } catch (Exception e) {
            log.printMessage(Diagnostic.Kind.ERROR, "generate route entry file exception");
            e.printStackTrace();
        }
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

    // 获取被注解的元素的类型
    private TypeEnum getElementKind(TypeMirror typeMirror) {
        if (types.isSubtype(typeMirror, typeMirror_Activity)) {
            return TypeEnum.ACTIVITY;
        } else if (types.isSubtype(typeMirror, typeMirror_Fragment)) {
            return TypeEnum.FRAGMENT;
        } else if (types.isSubtype(typeMirror, typeMirror_Fragment_V4)) {
            return TypeEnum.FRAGMENTV4;
        }
        if (types.isSubtype(typeMirror, typeMirror_Service)) {
            return TypeEnum.ISERVICE;
        }
        return TypeEnum.DEFAULT;
    }
}
