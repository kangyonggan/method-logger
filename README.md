# 编译时方法日志注解

### 是什么
它是一个编译时注解@MethodLogger，作用于方法上，只存在于源代码中（.class文件和运行时是没有此注解的）。  
可以打印方法的入参、出参和耗时。也可以自定义日志输出方式。

> * 优点1: 相对于运行时注解打印入参出参，此注解是在编译时把代码注入到方法中，运行时不用反射了，性能明显提升。
> * 优点2: 以前的运行时注解打印入参出参，是依赖于spring的，而此注解即使只是一个main方法都可以使用。
 
### 使用方法

```
package com.kangyonggan.app.monitor.test;

import com.kangyonggan.methodlogger.MethodLogger;

/**
 * @author kangyonggan
 * @since 10/12/17
 */
public class MethodLoggerTest {

    @MethodLogger
    public int add(int a, int b) {
        int c = a + b;
        System.out.println("a + b = " + c);
        return c;
    }

    public static void main(String[] args) {
        new MethodLoggerTest().add(1, 2);
    }
}
```

控制台输出：

```
[INFO ] 2017-10-12 15:46:10.013 [com.kangyonggan.app.monitor.test.MethodLoggerTest] - <add> method args：[1,2]
a + b = 3
[INFO ] 2017-10-12 15:46:10.014 [com.kangyonggan.app.monitor.test.MethodLoggerTest] - <add> method return：3
[INFO ] 2017-10-12 15:46:10.014 [com.kangyonggan.app.monitor.test.MethodLoggerTest] - <add> method used time：0ms
```

### 自定义日志输出

```
@MethodLogger(MyMethodLoggerHandler.class)
public static int add(int a, int b) {
    int c = a + b;
    System.out.println("a + b = " + c);
    return c;
}
```

MyMethodLoggerHandler.java

```
package com.kangyonggan.app.monitor.test;

import com.kangyonggan.methodlogger.ConsoleMethodLoggerHandler;

/**
 * @author kangyonggan
 * @since 10/12/17
 */
public class MyMethodLoggerHandler extends ConsoleMethodLoggerHandler {

    public MyMethodLoggerHandler(String packageName) {
        super(packageName);
    }

    @Override
    public void info(String msg) {
        System.out.println("自定义输出日志：" + packageName + " - " + msg);
    }
}
```

> * 提示：你可以在覆写的info方法中使用log4j、logback、log4j2输出日志。

### 待完善
1. 如果是内部类，又是静态方法，可能会出问题，所有我暂时不支持static方法了，后面再研究。