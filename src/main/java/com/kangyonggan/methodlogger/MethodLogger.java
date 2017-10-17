package com.kangyonggan.methodlogger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author kangyonggan
 * @since 9/28/17
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface MethodLogger {

    /**
     * @return
     */
    Class<? extends ConsoleMethodLoggerHandler> value() default ConsoleMethodLoggerHandler.class;

}
