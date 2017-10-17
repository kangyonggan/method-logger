package com.kangyonggan.methodlogger;

import com.alibaba.fastjson.JSON;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author kangyonggan
 * @since 10/11/17
 */
public class ConsoleMethodLoggerHandler {

    protected String packageName;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public ConsoleMethodLoggerHandler(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @param params
     */
    public void logBefore(Object... params) {
        Object methodName = params[0];
        Object[] objs = new Object[params.length - 1];
        for (int i = 0; i < params.length - 1; i++) {
            objs[i] = params[i + 1];
        }

        info("<" + methodName + "> method args：" + JSON.toJSONString(objs));
    }

    /**
     * @param methodName
     */
    public void logAfter(String methodName) {
        info("<" + methodName + "> method return：[]");
    }

    /**
     * @param methodName
     * @param returnObj
     */
    public void logAfter(String methodName, Object returnObj) {
        info("<" + methodName + "> method return：" + JSON.toJSONString(returnObj));
    }

    /**
     * @param methodName
     * @param startTime
     * @param endTime
     */
    public void logTime(String methodName, Long startTime, Long endTime) {
        info("<" + methodName + "> method used time：" + (endTime - startTime) + "ms");
    }

    /**
     * @param msg
     */
    public void info(String msg) {
        System.out.println("[INFO ] " + format.format(new Date()) + " [" + packageName + "] - " + msg);
    }

}

