package com.kangyonggan.methodlogger;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.Set;


/**
 * 监控注解处理器
 *
 * @author kangyonggan
 * @since 9/28/17
 */
@SupportedAnnotationTypes("com.kangyonggan.methodlogger.MethodLogger")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MethodLoggerProcessor extends AbstractProcessor {

    private MethodLoggerHelp methodLoggerHelp;
    private Trees trees;

    /**
     * 初始化，获取编译环境
     *
     * @param env
     */
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        trees = Trees.instance(env);
        Context context = ((JavacProcessingEnvironment) env).getContext();
        methodLoggerHelp = new MethodLoggerHelp(trees, TreeMaker.instance(context), Names.instance(context).table);
    }

    /**
     * 处理注解
     *
     * @param annotations
     * @param env
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        // 处理有@MethodLogger注解的元素
        for (Element element : env.getElementsAnnotatedWith(MethodLogger.class)) {
            // 只处理作用在方法上的注解
            if (element.getKind() == ElementKind.METHOD) {
                // 日志通道
                AnnotationMirror am = element.getAnnotationMirrors().get(0);

                String value = "com.kangyonggan.methodlogger.ConsoleMethodLoggerHandler";
                for (AnnotationValue av : am.getElementValues().values()) {
                   value = av.getValue().toString();
                }

                String channelPackage = value.substring(0, value.lastIndexOf("."));
                String channelName = value.substring(value.lastIndexOf(".") + 1);

                // 导入所需要的包
                methodLoggerHelp.importPackage(element, channelPackage, channelName);

                // 声明成员变量
                String packageName = ((JCTree.JCClassDecl) trees.getTree(element.getEnclosingElement())).sym.toString();
                int mofifiers = Flags.PRIVATE | Flags.STATIC | Flags.FINAL;
                methodLoggerHelp.defineVariable(element, mofifiers, channelName, packageName);

                // 生成增强代码
                methodLoggerHelp.generateCode(element, channelName);
            }
        }

        return true;
    }

}
