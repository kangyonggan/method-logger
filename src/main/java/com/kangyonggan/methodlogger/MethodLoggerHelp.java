package com.kangyonggan.methodlogger;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import javax.lang.model.element.AnnotationMirror;
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
     * @param element
     * @param className
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
     * @param element
     * @param className
     * @param args
     */
    public void defineVariable(Element element, String className, Object... args) {
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

                    int modifiers = Flags.PRIVATE;
                    if (jcClassDecl.sym != null) {
                        if (jcClassDecl.sym.flatname.toString().equals(jcClassDecl.sym.fullname.toString())) {
                            modifiers = modifiers | Flags.STATIC;
                        }

                        JCTree.JCExpression typeExpr = treeMaker.Ident(names.fromString(className));
                        JCTree.JCNewClass newClassExpr = treeMaker.NewClass(null, List.nil(), typeExpr, argList.toList(), null);
                        JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(treeMaker.Modifiers(modifiers), names.fromString(varName), typeExpr, newClassExpr);
                        statements.append(variableDecl);

                        jcClassDecl.defs = statements.toList();
                    }
                }

                super.visitClassDef(jcClassDecl);
            }
        });
    }

    /**
     * @param element
     */
    public void generateCode(Element element, String className) {
        JCTree tree = (JCTree) trees.getTree(element);
        String varName = className.substring(0, 1).toLowerCase() + className.substring(1);

        tree.accept(new TreeTranslator() {
            private List<JCTree.JCExpression> params = List.nil();

            private JCTree.JCExpression returnType;

            @Override
            public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
                params = List.nil();
                params = params.append(treeMaker.Literal(jcMethodDecl.getName().toString()));
                for (JCTree.JCVariableDecl decl : jcMethodDecl.getParameters()) {
                    params = params.append(treeMaker.Ident(decl));
                }
                returnType = jcMethodDecl.restype;

                super.visitMethodDef(jcMethodDecl);

            }

            @Override
            public void visitBlock(JCTree.JCBlock tree) {
                ListBuffer<JCTree.JCStatement> statements = new ListBuffer();

                JCTree.JCFieldAccess fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString(varName)), names.fromString("logBefore"));
                JCTree.JCMethodInvocation methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, params);
                JCTree.JCExpressionStatement code = treeMaker.Exec(methodInvocation);
                statements.append(code);

                JCTree.JCExpression typeExpr = treeMaker.Ident(names.fromString("Long"));
                fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString("System")), names.fromString("currentTimeMillis"));
                methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.nil());
                JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(treeMaker.Modifiers(0), names.fromString("methodLoggerStartTime"), typeExpr, methodInvocation);
                statements.append(variableDecl);

                for (int i = 0; i < tree.getStatements().size(); i++) {
                    JCTree.JCStatement jcStatement = tree.getStatements().get(i);
                    boolean hasReturnValue = false;

                    if (jcStatement instanceof JCTree.JCReturn) {
                        JCTree.JCReturn jcReturn = (JCTree.JCReturn) jcStatement;
                        typeExpr = treeMaker.Ident(names.fromString("Object"));
                        variableDecl = treeMaker.VarDef(treeMaker.Modifiers(0), names.fromString("monitorReturnValue"), typeExpr, jcReturn.getExpression());
                        statements.append(variableDecl);
                        hasReturnValue = true;
                    }

                    if (i == tree.getStatements().size() - 1) {
                        if (hasReturnValue) {
                            process(statements, hasReturnValue);

                            JCTree.JCReturn jcReturn = (JCTree.JCReturn) jcStatement;
                            JCTree.JCTypeCast jcTypeCast = treeMaker.TypeCast(returnType, treeMaker.Ident(names.fromString("monitorReturnValue")));
                            jcReturn.expr = jcTypeCast;
                            statements.append(jcReturn);
                        } else {
                            process(statements, hasReturnValue);
                            statements.append(tree.getStatements().get(i));
                        }
                    } else {
                        statements.append(tree.getStatements().get(i));
                    }
                }

                if (tree.getStatements().size() <= 0) {
                    process(statements, false);
                }

                result = treeMaker.Block(0, statements.toList());
            }

            private void process(ListBuffer<JCTree.JCStatement> statements, boolean hasReturnValue) {
                JCTree.JCExpression typeExpr;
                JCTree.JCFieldAccess fieldAccess;
                JCTree.JCMethodInvocation methodInvocation;
                JCTree.JCVariableDecl variableDecl;
                JCTree.JCExpressionStatement code;
                typeExpr = treeMaker.Ident(names.fromString("Long"));
                fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString("System")), names.fromString("currentTimeMillis"));
                methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.nil());
                variableDecl = treeMaker.VarDef(treeMaker.Modifiers(0), names.fromString("methodLoggerEndTime"), typeExpr, methodInvocation);
                statements.append(variableDecl);

                fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString(varName)), names.fromString("logTime"));
                methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.of(params.get(0), treeMaker.Ident(names.fromString("methodLoggerStartTime")), treeMaker.Ident(names.fromString("methodLoggerEndTime"))));
                code = treeMaker.Exec(methodInvocation);
                statements.append(code);

                if (hasReturnValue) {
                    fieldAccess = treeMaker.Select(treeMaker.Ident(names.fromString(varName)), names.fromString("logAfter"));
                    methodInvocation = treeMaker.Apply(List.nil(), fieldAccess, List.of(params.get(0), treeMaker.Ident(names.fromString("monitorReturnValue"))));
                    code = treeMaker.Exec(methodInvocation);
                    statements.append(code);
                }
            }
        });
    }

    public AnnotationMirror getAnnotationMirror(java.util.List<? extends AnnotationMirror> annotationMirrors) {
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            if ("com.kangyonggan.methodlogger.MethodLogger".equals(annotationMirror.getAnnotationType().toString())) {
                return annotationMirror;
            }

        }

        return null;
    }

}
