package pers.hdh.controller;

import pers.hdh.annotation.DhController;
import pers.hdh.annotation.DhQualifier;
import pers.hdh.annotation.DhRequestMapping;
import pers.hdh.annotation.DhRequestParam;
import pers.hdh.service.TestService;
import pers.hdh.service.TestService2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@DhController
@DhRequestMapping("/test")
public class TestController {

    @DhQualifier("testService")
    private TestService testService;
    // 测试@DhService使用默认值
    @DhQualifier("testService2Impl")
    private TestService2 testService2;

    /**
     * 测试的url：http://localhost:8080/dh-springmvc/test/1?str_param=233&int_param=2&float_param=1.2&double_param=2.5
     * @param request
     * @param response
     * @param strParam
     * @param intParam
     * @param floatParam
     * @param doubleParam
     */
    @DhRequestMapping("/1")
    public void test1(HttpServletRequest request, HttpServletResponse response,
                      @DhRequestParam("str_param") String strParam,
                      @DhRequestParam("int_param") Integer intParam,
                      @DhRequestParam("float_param") Float floatParam,
                      @DhRequestParam("double_param") Double doubleParam) {
        testService.doServiceTest();
        testService2.doServiceTest();
        try {
            response.getWriter().write(
                    "String parameter: " + strParam +
                      "\nInteger parameter: " + intParam +
                      "\nFloat parameter: " + floatParam +
                      "\nDouble parameter: " + doubleParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
