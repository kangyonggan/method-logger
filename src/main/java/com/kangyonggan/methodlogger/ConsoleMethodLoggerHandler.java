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
     * 打印入参
     *
     * @param params
     */
    public void logBefore(Object... params) {
        info("方法入参：" + JSON.toJSONString(params));
    }

    /**
     * 打印出参
     */
    public void logAfter() {
        info("方法出参：[]");
    }

    /**
     * 打印出参
     *
     * @param returnObj
     */
    public void logAfter(Object returnObj) {
        info("方法出参：" + JSON.toJSONString(returnObj));
    }

    /**
     * 打印耗时
     *
     * @param startTime
     * @param endTime
     */
    public void logTime(Long startTime, Long endTime) {
        info("方法耗时：" + (endTime - startTime) + "ms");
    }

    /**
     * 默认打印方式
     * 例：[INFO ] 2017-10-12 11:19:35.632 [com.kangyonggan.app.future.biz.UserService] - 方法入参：[1001]
     *
     * @param msg
     */
    public void info(String msg) {
        System.out.println("[INFO ] " + format.format(new Date()) + " [" + packageName + "] - " + msg);
    }

}

