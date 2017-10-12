package com.kangyonggan.methodlogger;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import javax.lang.model.element.Element;

/**
 * @author kangyonggan
 * @since 10/12/17
 */
public class MethodLoggerHelp {

    private Trees trees;
    private TreeMaker treeMaker;
    private Name.Table names;

    public MethodLoggerHelp(Trees trees, TreeMaker treeMaker, Name.Table names) {
        this.trees = trees;
        this.treeMaker = treeMaker;
        this.names = names;
    }

    /**
     * 导包, 如：import com.kangyonggan.methodlogger.ConsoleMethodLoggerHandler;
     *
     * @param element
     * @param className 类名，如：ConsoleMethodLoggerHandler
     */
    public void importPackage(Element element, String packageName, String className) {
        JCTree.JCCompilationUnit cu = (JCTree.JCCompilationUnit) trees.getPath(element.getEnclosingElement()).getCompilationUnit();
        JCTree.JCFieldAccess fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString(packageName)), names.fromString(className));
        JCTree.JCImport jcImport = treeMaker.Import(fieldAccess, false);

        ListBuffer<JCTree> imports = new ListBuffer();
        imports.append(jcImport);

        for (int i = 0; i < cu.defs.size(); i++) {
            imports.append(cu.defs.get(i));
        }

        cu.defs = imports.toList();
    }

    /**
     * 声明一个成员变量，如：private static final ConsoleMethodLoggerHandler consoleMethodLoggerHandler = new ConsoleMethodLoggerHandler("包名");
     *
     * @param element
     * @param modifiers 修饰符, 如：private static final
     * @param className 类名，如：ConsoleMethodLoggerHandler
     * @param args      参数，如："包名"
     */
    public void defineVariable(Element element, int modifiers, String className, Object... args) {
        JCTree tree = (JCTree) trees.getTree(element.getEnclosingElement());
        String varName = className.substring(0, 1).toLowerCase() + className.substring(1);

        tree.accept(new TreeTranslator() {
            @Override
            public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                ListBuffer<JCTree> statements = new ListBuffer();
                List<JCTree> oldList = this.translate(jcClassDecl.defs);
                boolean hasField = false;

                for (JCTree jcTree : oldList) {
                    if (jcTree.getKind() == Tree.Kind.VARIABLE) {
                        JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) jcTree;

                        if (varName.equals(variableDecl.name.toString()) && className.equals(variableDecl.vartype.toString())) {
                            hasField = true;
                        }
                    }

                    statements.append(jcTree);
                }

                if (!hasField) {
                    ListBuffer<JCTree.JCExpression> argList = new ListBuffer();

                    for (Object arg : args) {
                        JCTree.JCExpression argsExpr = treeMaker.Literal(arg);
                        argList.append(argsExpr);
                    }

                    JCTree.JCExpression typeExpr = treeMaker.Ident(names.fromString(className));
                    JCTree.JCNewClass newClassExpr = treeMaker.NewClass(null, List.nil(), typeExpr, argList.toList(), null);
                    JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(treeMaker.Modifiers(modifiers), names.fromString(varName), typeExpr, newClassExpr);
                    statements.append(variableDecl);

                    jcClassDecl.defs = statements.toList();
                }

                super.visitClassDef(jcClassDecl);
            }
        });
    }

    /**
     * 生成增强代码
     * <p>
     * consoleMethodLoggerHandler.logBefore(入参);
     * Long methodLoggerStartTime = System.currentTimeMillis();
     * <p>
     * ...
     * 原来的代码块
     * ...
     * <p>
     * Long methodLoggerEndTime = System.currentTimeMillis();
     * consoleMethodLoggerHandler.logAfter(出参);
     * consoleMethodLoggerHandler.logTime(methodLoggerStartTime, methodLoggerEndTime);
     *
     * @param element
     */
    public void generateCode(Element element, String className) {
        JCTree tree = (JCTree) trees.getTree(element);
        String varName = className.substring(0, 1).toLowerCase() + className.substring(1);

        tree.accept(new TreeTranslator() {
            /**
             * 参数
             */
            List<JCTree.JCExpression> params = List.nil();

            @Override
            public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
                params = List.nil();
                params = params.append(treeMaker.Literal(jcMethodDecl.getName().toString()));
                for (JCTree.JCVariableDecl decl : jcMethodDecl.getParameters()) {
                    params = params.append(treeMaker.Ident(decl));
                }

                super.visitMethodDef(jcMethodDecl);

            }

            @Override
            public void visitBlock(JCTree.JCBlock tree) {
                ListBuffer<JCTree.JCStatement> statements = new ListBuffer();

                /**
                 * 创建代码（打印入参）：consoleMethodLoggerHandler.logBefore(params);
                 */
                JCTree.JCFieldAccess fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString(varName)), names.fromString("logBefore"));
                JCTree.JCMethodInvocation methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, params);
                JCTree.JCExpressionStatement code = treeMaker.Exec(methodInvocation);
                statements.append(code);

                /**
                 * 创建代码（开始时间）：Long methodLoggerStartTime = System.currentTimeMillis();
                 */
                JCTree.JCExpression typeExpr = treeMaker.Ident(names.fromString("Long"));
                fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString("System")), names.fromString("currentTimeMillis"));
                methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.nil());
                JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(treeMaker.Modifiers(0), names.fromString("methodLoggerStartTime"), typeExpr, methodInvocation);
                statements.append(variableDecl);

                // 把原来的方法体写回去
                for (int i = 0; i < tree.getStatements().size(); i++) {

                    if (i == tree.getStatements().size() - 1) {
                        /**
                         * 创建代码（结束时间）：Long methodLoggerEndTime = System.currentTimeMillis();
                         */
                        typeExpr = treeMaker.Ident(names.fromString("Long"));
                        fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString("System")), names.fromString("currentTimeMillis"));
                        methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.nil());
                        variableDecl = treeMaker.VarDef(treeMaker.Modifiers(0), names.fromString("methodLoggerEndTime"), typeExpr, methodInvocation);
                        statements.append(variableDecl);

                        /**
                         * 创建代码（打印出参）：consoleMethodLoggerHandler.logAfter(returnObj);
                         */
                        fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString(varName)), names.fromString("logAfter"));
                        try {
                            JCTree.JCReturn jcReturn = (JCTree.JCReturn) tree.getStatements().get(i);
                            methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.of(params.get(0), jcReturn.getExpression()));
                        } catch (Exception e) {
                            // 无返回值
                            methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.of(params.get(0)));
                        }
                        code = treeMaker.Exec(methodInvocation);
                        statements.append(code);

                        /**
                         * 创建代码（打印耗时）：consoleMethodLoggerHandler.logTime(methodLoggerStartTime, methodLoggerEndTime);
                         */
                        fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString(varName)), names.fromString("logTime"));
                        methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.of(params.get(0), treeMaker.Ident(names.fromString("methodLoggerStartTime")), treeMaker.Ident(names.fromString("methodLoggerEndTime"))));
                        code = treeMaker.Exec(methodInvocation);
                        statements.append(code);
                    }

                    statements.append(tree.getStatements().get(i));
                }

                result = treeMaker.Block(0, statements.toList());
            }
        });
    }

}
