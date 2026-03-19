package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标识方法需要进行公共字段的处理
 */
@Target(ElementType.METHOD)//表明注解只能加在方法上
@Retention(RetentionPolicy.RUNTIME)//表示注解在运行时保留
public @interface AutoFill {
    //指定数据库操作类型 UPDATE INSERT
    OperationType value();
}
