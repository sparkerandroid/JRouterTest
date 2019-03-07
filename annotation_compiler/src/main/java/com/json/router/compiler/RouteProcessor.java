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

import java.lang.reflect.ParameterizedType;
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
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedOptions({Const.MODULE_NAME})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({Const.ANNOTATION_PACKAGE + Const.ROUTE_META})
public class RouteProcessor extends AbstractProcessor {

    private Messager log;
    private Elements elements;
    private Types types;
    private Filer filer;
    private String moduleName;

    private Map<String, Set<RouteMeta>> group;//路由分组
    private Map<String, String> entry;//路由入口

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        log = processingEnvironment.getMessager();
        elements = processingEnvironment.getElementUtils();
        types = processingEnvironment.getTypeUtils();
        filer = processingEnvironment.getFiler();
        Map<String, String> options = processingEnvironment.getOptions();
        if (options != null && !options.isEmpty()) {
            moduleName = options.get(Const.MODULE_NAME);
        }
        if (moduleName == null || moduleName.length() == 0) {
            log.printMessage(Diagnostic.Kind.ERROR, "moduleName can't be null or empty");
            throw new RuntimeException("moduleName can't be null or empty");
        }
        group = new HashMap<>();
        entry = new HashMap<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set == null || set.isEmpty()) {
            log.printMessage(Diagnostic.Kind.ERROR, "process set is empty");
            return false;
        }
        Set<? extends Element> routeElements = roundEnvironment.getElementsAnnotatedWith(Route.class);
        if (routeElements == null || routeElements.isEmpty()) {
            log.printMessage(Diagnostic.Kind.ERROR, "process roundEnvironment is empty");
            return false;
        }

        TypeElement type_activity = elements.getTypeElement(Const.ACTIVITY);

        TypeMirror typeMirror_activity = type_activity.asType();

        log.printMessage(Diagnostic.Kind.ERROR, "start to classify route info");
        for (Element ele : routeElements) { //遍历&路由分组
            TypeMirror anno_ele = ele.asType();
            if (types.isSubtype(typeMirror_activity, anno_ele)) {// 是Activity的子类
                Route route = anno_ele.getAnnotation(Route.class);
                String path = route.path();//获取路由
                String groupName = extractGroupFromPath(path);//提取分组
                RouteMeta routeMeta = new RouteMeta(path, groupName, ele, TypeEnum.ACTIVITY);//封装路由信息
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
        log.printMessage(Diagnostic.Kind.ERROR, "classify route info  -- finish -- group size is " + group.size());
        log.printMessage(Diagnostic.Kind.ERROR, "start to generate route group file");
        // 生成路由表文件
        if (group == null || group.size() == 0) {//无路由表信息
            return false;
        }
        //1、生成参数类型:Map<String, RouteMeta>
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouteMeta.class)
        );

        //2、生成参数：routes
        ParameterSpec parameterSpec = ParameterSpec.builder(parameterizedTypeName,
                "routes",
                Modifier.PUBLIC)
                .build();

        //3、遍历路由分组（Map<String, Set<RouteMeta>> group），生成路由表文件
        // String是分组名，每一个Set<RouteMeta>都是一个路由表文件
        for (Map.Entry<String, Set<RouteMeta>> routeEntry : group.entrySet()) {
            String groupName = routeEntry.getKey();
            MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(Const.LOAD_ROUTE_INFO)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(parameterSpec);
            for (RouteMeta routeMeta : routeEntry.getValue()) {//遍历&添加统一分组下的路由信息
                // routes.put("/main/mainactivity", RouteMeta.build(path, group, destination, rawType, type))
                methodSpec.addStatement("routes.put($S,$T.build($S,$S,$S.class," + routeMeta.getRawType() + "," + routeMeta.getType() + ",)",
                        routeMeta.getPath(),
                        ClassName.get(RouteMeta.class),
                        routeMeta.getPath(),
                        routeMeta.getGroup(),
                        ClassName.get(routeMeta.getRawType().asType())
                );
            }
            //路由文件名
            String groupFileName = moduleName + "_Group_" + groupName;
            TypeSpec typeSpec = TypeSpec.classBuilder(groupFileName)
                    .addSuperinterface(ClassName.get(Const.TEMPLATE, Const.IROUTEGROUP))
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(methodSpec.build())
                    .build();

            JavaFile javaFile = JavaFile.builder(Const.ROUTE_FILE_DIRECTORY_NAME, typeSpec).build();
            try {
                javaFile.writeTo(filer);
            } catch (Exception e) {
                log.printMessage(Diagnostic.Kind.ERROR, "generate route group file exception");
            }

            if (entry == null) {
                entry = new HashMap<>();
            }
            entry.put(groupName, groupFileName);//保存生成的路由文件信息，作为之后的查找入口

        }
        return true;
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
