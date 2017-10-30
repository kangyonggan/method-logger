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
 * @author kangyonggan
 * @since 9/28/17
 */
@SupportedAnnotationTypes("com.kangyonggan.methodlogger.MethodLogger")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MethodLoggerProcessor extends AbstractProcessor {

    private MethodLoggerHelp methodLoggerHelp;
    private Trees trees;

    /**
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
     * @param annotations
     * @param env
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(MethodLogger.class)) {
            if (element.getKind() == ElementKind.METHOD) {
                AnnotationMirror am = methodLoggerHelp.getAnnotationMirror(element.getAnnotationMirrors());

                String value = "com.kangyonggan.methodlogger.ConsoleMethodLoggerHandler";

                for (ExecutableElement ee : am.getElementValues().keySet()) {
                    if (ee.getSimpleName().toString().equals("value")) {
                        value = am.getElementValues().get(ee).getValue().toString();
                        break;
                    }
                }

                String channelPackage = value.substring(0, value.lastIndexOf("."));
                String channelName = value.substring(value.lastIndexOf(".") + 1);

                methodLoggerHelp.importPackage(element, channelPackage, channelName);

                String packageName = ((JCTree.JCClassDecl) trees.getTree(element.getEnclosingElement())).sym.toString();
                methodLoggerHelp.defineVariable(element, channelName, packageName);

                methodLoggerHelp.generateCode(element, channelName);
            }
        }

        return true;
    }

}
