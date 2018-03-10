package pers.hdh.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)// 指定直接可作用在接口、类、枚举、注解上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DhController {
    /**
     * 给controller起别名
     * @return
     */
    String value() default "";
}
