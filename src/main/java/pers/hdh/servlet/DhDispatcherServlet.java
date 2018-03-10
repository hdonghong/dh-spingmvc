package pers.hdh.servlet;

import pers.hdh.annotation.*;
import pers.hdh.commons.CommonUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hdonghong
 * dh-springmvc的前端控制器
 */
//@WebServlet("/*")
public class DhDispatcherServlet extends HttpServlet {

    // 集合全自动扫描基础包下面的类限定名
    private List<String> beanNames = new ArrayList<>();

    // 缓存 key/value: 类注解参数/类实例对象，存储controller和service实例
    private Map<String, Object> instanceMaps = new HashMap<>();

    // key/value: 请求url/handler的method
    private Map<String, Method> handlerMaps = new HashMap<>();

    // 再维护一个map，存储controller实例
    private Map<String, Object> controllerMaps = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            // 1.通过web.xml拿到基本包信息 读外部配置文件
            String mvcConfig = config.getInitParameter("contextConfigLocation")
                                     .replace("*", "")
                                     .replace("classpath:", "");
            String basePackName = CommonUtils.getBasePackName(mvcConfig);
            System.out.println("扫描的基包是：" + basePackName);

            // 2.全自动扫描基本包下的bean，加载Spring容器
            scanPack(basePackName);

            // 3.通过注解对象，找到每个bean，反射获取实例
            reflectBeansInstance();

            // 4.依赖注入，实现ioc机制
            doIoc();

            // 5.handlerMapping通过基部署 和 基于类的url找到相应的处理器
            initHandlerMapping();

        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof ServletException) {
                new ServletException(e);
            }
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理业务
     * @param req
     * @param resp
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (handlerMaps.isEmpty()) {
            return;
        }
        String uri = req.getRequestURI();// 如：/project_name/classURI/methodURI
        String contextPath = req.getContextPath();// 如：/project_name
        String url = uri.replace(contextPath, "").replaceAll("/+", "/");

        // 获取到请求执行的方法
        Method handlerMethod = handlerMaps.get(url);
        PrintWriter out =  resp.getWriter();
        if (handlerMethod == null) {
            out.print("404！！！您访问的资源不存在");
            return;
        }

        // 获取方法的参数列表
        Parameter methodParameters[] = handlerMethod.getParameters();
        // 调用方法需要传递的形参
        Object paramValues[] = new Object[methodParameters.length];
        for (int i = 0; i < methodParameters.length; i++) {

            if (ServletRequest.class.isAssignableFrom(methodParameters[i].getType())) {
                paramValues[i] = req;
            } else if (ServletResponse.class.isAssignableFrom(methodParameters[i].getType())) {
                paramValues[i] = resp;
            } else {// 其它参数，目前只支持String，Integer，Float，Double
                // 参数绑定的名称，默认为方法形参名
                String bindingValue = methodParameters[i].getName();
                if (methodParameters[i].isAnnotationPresent(DhRequestParam.class)) {
                    bindingValue = methodParameters[i].getAnnotation(DhRequestParam.class).value();
                }
                // 从请求中获取参数的值
                String paramValue = req.getParameter(bindingValue);
                paramValues[i] = paramValue;
                if (paramValue != null) {
                    if (Integer.class.isAssignableFrom(methodParameters[i].getType())) {
                        paramValues[i] = Integer.parseInt(paramValue);
                    } else if (Float.class.isAssignableFrom(methodParameters[i].getType())) {
                        paramValues[i] = Float.parseFloat(paramValue);
                    } else if (Double.class.isAssignableFrom(methodParameters[i].getType())) {
                        paramValues[i] = Double.parseDouble(paramValue);
                    }
                }
            }
        }

        try {
            handlerMethod.invoke(controllerMaps.get(url), paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    /**
     * 通过对请求url分析之后拿到响应的handler实例里面的method处理
     */
    private void initHandlerMapping() throws Exception {
        if (instanceMaps.isEmpty()) {
            throw new Exception("没有发现handler对象");
        }
        for (Map.Entry<String, Object> entry : instanceMaps.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();
            // 通过实例区分Controller层对象
            if (aClass.isAnnotationPresent(DhController.class)) {
                // 实现注解映射请求路径，允许当controller类没有使用@DhRequestMapping注解时，
                // 可使用@DhController注解的value作为请求路径
                String classURI = "";
                if (aClass.isAnnotationPresent(DhRequestMapping.class)) {
                    classURI = aClass.getAnnotation(DhRequestMapping.class).value();
                } else {
                    classURI = aClass.getAnnotation(DhController.class).value();
                }
                // 遍历controller类中每个使用@DhRquestMapping的方法，细化请求路径
                Method[] methods = aClass.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(DhRequestMapping.class)) {
                        String methodURI = method.getAnnotation(DhRequestMapping.class).value();

                        // 存入handlerMaps
                        String url = ("/" + classURI + "/" + methodURI).replaceAll("/+", "/");
                        handlerMaps.put(url, method);
                        // 再维护一个只存储controller实例的map
                        controllerMaps.put(url, entry.getValue());
                    }
                }

            }
        }
    }

    /**
     * 依赖注入，实现ioc机制
     * @throws Exception
     */
    private void doIoc() throws Exception {
        if (instanceMaps.isEmpty()) {
            throw new Exception("没有发现可注入的实例");
        }
        for (Map.Entry<String, Object> entry : instanceMaps.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            // 遍历bean对象的字段
            for (Field field : fields) {
                if (field.isAnnotationPresent(DhQualifier.class)) {
                    // 通过bean字段对象上面的注解参数来注入实例
                    String insMapKey = field.getAnnotation(DhQualifier.class).value();
                    if (insMapKey.equals("")) {// 如果使用@DhController，@DhService没有配置value的值，默认使用类名 首字母小写
                        insMapKey = CommonUtils.toLowerFirstWord(field.getType().getSimpleName());
                    }
                    field.setAccessible(true);
                    // 注入实例
                    field.set(entry.getValue(), instanceMaps.get(insMapKey));
                }
            }
        }

    }

    /**
     * 通过注解对象，找到每个bean，反射获取实例
     */
    private void reflectBeansInstance() throws Exception {
        if (beanNames.isEmpty()) {
            return;
        }
        for (String className : beanNames) {
            Class<?> aClass = Class.forName(className);

            if (aClass.isAnnotationPresent(DhController.class)) {// 操作控制层的实例
                Object controllerInstance = aClass.newInstance();
                // 进一步对这个控制层实例对象打标签，维护到缓存中
                DhController controllerAnnotation = aClass.getAnnotation(DhController.class);
                String insMapKey = controllerAnnotation.value();
                if ("".equals(insMapKey)) {// 如果使用@DhController，@DhService没有配置value的值，默认使用类名 首字母小写
                    insMapKey = CommonUtils.toLowerFirstWord(aClass.getSimpleName());
                }
                instanceMaps.put(insMapKey, controllerInstance);
            } else if (aClass.isAnnotationPresent(DhService.class)) {// 操作业务层的实例
                Object serviceInstance = aClass.newInstance();
                // 进一步对这个业务层实例对象打标签，维护到缓存中
                DhService serviceAnnotation = aClass.getAnnotation(DhService.class);
                String insMapKey = serviceAnnotation.value();
                if ("".equals(insMapKey)) {// 如果使用@DhController，@DhService没有配置value的值，默认使用类名 首字母小写
                    insMapKey = CommonUtils.toLowerFirstWord(aClass.getSimpleName());
                }
                instanceMaps.put(insMapKey, serviceInstance);
            }
        }
    }

    /**
     * 扫描基本包
     * @param basePackName
     */
    private void scanPack(String basePackName) throws Exception {
        URL url = this.getClass()
                      .getClassLoader()
                      .getResource("/" + CommonUtils.transferQualifiedToPath(basePackName));
        // 读取到扫描包
        File dir = new File(url.getFile());
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                // 遇到目录递归读取文件
                scanPack(basePackName + "." + file.getName());
            } else if (file.isFile()) {
                // 将形如pers.hdh.controller.Xxx的类限定名字符串加入beanNames中
                beanNames.add(basePackName + "." + file.getName().replace(".class", ""));
                System.out.println("扫描到的类有：" + basePackName + "." + file.getName().replace(".class", ""));
            }
        }

    }

}
