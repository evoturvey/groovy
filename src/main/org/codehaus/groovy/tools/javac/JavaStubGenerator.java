/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.tools.javac;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import groovy.lang.GroovyObjectSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.ResolveVisitor;
import org.objectweb.asm.Opcodes;

public class JavaStubGenerator
{
    private JavaAwareCompilationUnit cu;

    private boolean java5 = false;
    
    private File outputPath;

    private ArrayList toCompile = new ArrayList();

    private ResolveVisitor resolver;

    public JavaStubGenerator(JavaAwareCompilationUnit cu, File outputPath, boolean java5) {
        this.cu = cu;
        this.outputPath = outputPath;
        this.java5 = java5;
    }

    public JavaStubGenerator(JavaAwareCompilationUnit cu, File outputPath) {
        this(cu, outputPath, false);
    }
    
    private void mkdirs(File parent, String relativeFile) {
        int index = relativeFile.lastIndexOf('/');
        if (index==-1) return;
        File dir = new File(parent,relativeFile.substring(0,index));
        dir.mkdirs();
    }
    
    public void generateClass(ClassNode classNode) throws FileNotFoundException {
        // Only attempt to render our self if our super-class is resolved, else wait for it
        if (!classNode.getSuperClass().isResolved()) {
            return;
        }

        String fileName = classNode.getName().replace('.', '/');
        mkdirs(outputPath,fileName);
        toCompile.add(fileName);

        File file = new File(outputPath, fileName + ".java");
        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter out = new PrintWriter(fos);

        try {
            String packageName = classNode.getPackageName();
            if (packageName != null) {
                out.println("package " + packageName + ";\n");
            }

            genImports(classNode, out);

            boolean isInterface = classNode.isInterface();

            printModifiers(out, classNode.getModifiers()
                    & ~(isInterface ? Opcodes.ACC_ABSTRACT : 0));
            out.println((isInterface ? "interface " : "class ")
                    + classNode.getNameWithoutPackage());

            ClassNode superClass = classNode.getSuperClass();

            if (!isInterface) {
                if (superClass.equals(ClassHelper.OBJECT_TYPE))
                    superClass = ClassHelper.make(GroovyObjectSupport.class);
                out.println("  extends " + superClass.getName());
            } else {
                if (!superClass.equals(ClassHelper.OBJECT_TYPE))
                    out.println("  extends " + superClass.getName());
            }

            ClassNode[] interfaces = classNode.getInterfaces();
            if (interfaces != null && interfaces.length > 0) {
                out.println("  implements");
                for (int i = 0; i < interfaces.length - 1; ++i)
                    out.println("    " + interfaces[i].getName() + ",");
                out.println("    "
                        + interfaces[interfaces.length - 1].getName());
            }
            out.println("{");

            genMethods(classNode, out);
            genFields(classNode, out);
            genProps(classNode, out);

            out.println("}");
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                // ignore
            }
            try {
                fos.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void genMethods(ClassNode classNode, PrintWriter out) {
        getContructors(classNode, out);

        List methods = classNode.getMethods();
        if (methods != null)
            for (Iterator it = methods.iterator(); it.hasNext();) {
                MethodNode methodNode = (MethodNode) it.next();
                genMethod(methodNode, out);
            }
    }

    private void getContructors(ClassNode classNode, PrintWriter out) {
        List constrs = classNode.getDeclaredConstructors();
        if (constrs != null)
            for (Iterator it = constrs.iterator(); it.hasNext();) {
                ConstructorNode constrNode = (ConstructorNode) it.next();
                genConstructor(constrNode, out);
            }
    }

    private void genFields(ClassNode classNode, PrintWriter out) {
        List fields = classNode.getFields();
        if (fields != null)
            for (Iterator it = fields.iterator(); it.hasNext();) {
                FieldNode fieldNode = (FieldNode) it.next();
                genField(fieldNode, out);
            }
    }

    private void genProps(ClassNode classNode, PrintWriter out) {
        List props = classNode.getProperties();
        if (props != null)
            for (Iterator it = props.iterator(); it.hasNext();) {
                PropertyNode propNode = (PropertyNode) it.next();
                genProp(propNode, out);
            }
    }

    private void genProp(PropertyNode propNode, PrintWriter out) {
        String name = propNode.getName().substring(0, 1).toUpperCase()
                + propNode.getName().substring(1);

        String getterName = "get" + name;

        boolean skipGetter = false;
        List getterCandidates = propNode.getField().getOwner().getMethods(getterName);
        if (getterCandidates != null)
            for (Iterator it = getterCandidates.iterator(); it.hasNext();) {
                MethodNode method = (MethodNode) it.next();
                if (method.getParameters().length == 0) {
                    skipGetter = true;
                }
            }

        if (!skipGetter) {
            printModifiers(out, propNode.getModifiers());

            printType(propNode.getType(), out);
            out.print(" ");
            out.print(getterName);
            out.print("() { ");

            printReturn(out, propNode.getType());

            out.println(" }");
        }

        String setterName = "set" + name;

        boolean skipSetter = false;
        List setterCandidates = propNode.getField().getOwner().getMethods( setterName);
        if (setterCandidates != null)
            for (Iterator it = setterCandidates.iterator(); it.hasNext();) {
                MethodNode method = (MethodNode) it.next();
                if (method.getParameters().length == 1) {
                    skipSetter = true;
                }
            }

        if (!skipSetter) {
            printModifiers(out, propNode.getModifiers());
            out.print("void ");
            out.print(setterName);
            out.print("(");
            printType(propNode.getType(), out);
            out.println(" value) {}");
        }
    }

    private void genField(FieldNode fieldNode, PrintWriter out) {
        printModifiers(out, fieldNode.getModifiers());

        printType(fieldNode.getType(), out);

        out.print(" ");
        out.print(fieldNode.getName());
        out.println(";");
    }

    private ConstructorCallExpression getConstructorCallExpression(
            ConstructorNode constructorNode) {
        Statement code = constructorNode.getCode();
        if (!(code instanceof BlockStatement))
            return null;

        BlockStatement block = (BlockStatement) code;
        List stats = block.getStatements();
        if (stats == null || stats.size() == 0)
            return null;

        Statement stat = (Statement) stats.get(0);
        if (!(stat instanceof ExpressionStatement))
            return null;

        Expression expr = ((ExpressionStatement) stat).getExpression();
        if (!(expr instanceof ConstructorCallExpression))
            return null;

        return (ConstructorCallExpression) expr;
    }

    private void genConstructor(ConstructorNode constructorNode, PrintWriter out) {
        // printModifiers(out, constructorNode.getModifiers());

        out.print("public "); // temporary hack
        out.print(constructorNode.getDeclaringClass().getNameWithoutPackage());

        printParams(constructorNode, out);

        ConstructorCallExpression constrCall = getConstructorCallExpression(constructorNode);
        if (constrCall == null || !constrCall.isSpecialCall()) {
            out.println(" {}");
        }
        else {
            out.println(" {");

            genSpecialContructorArgs(out, constructorNode, constrCall);

            out.println("}");
        }
    }

    private ConstructorNode selectAccessibleConstructorFromSuper(ConstructorNode node) {
        assert node != null;

        ClassNode type = node.getDeclaringClass();
        ClassNode superType = type.getSuperClass();

        for (Iterator iter = superType.getDeclaredConstructors().iterator(); iter.hasNext();) {
            ConstructorNode c = (ConstructorNode)iter.next();

            // Only look at things we can actually call
            if (c.isPublic() || c.isProtected()) {
                return c;
            }
        }

        if (!superType.isResolved()) {
            throw new Error("Super-class (" + superType.getName() + ")should have been resolved already for type: " + type.getName());
        }

        Constructor[] constructors = superType.getTypeClass().getDeclaredConstructors();

        for (int i=0; i<constructors.length; i++) {
            int mod = constructors[i].getModifiers();

            // Only look at things we can actualy call
            if (Modifier.isPublic(mod) || Modifier.isProtected(mod)) {
                Class[] types = constructors[i].getParameterTypes();
                Parameter[] params = new Parameter[types.length];
                for (int j=0; j<types.length; j++) {
                    ClassNode ptype = new ClassNode(types[i]);
                    params[j] = new Parameter(ptype, types[i].getName());
                }

                return new ConstructorNode(mod, params, null, null);
            }
        }
        
        return null;
    }

    private void genSpecialContructorArgs(PrintWriter out, ConstructorNode node, ConstructorCallExpression constrCall) {
        // Select a constructor from our class, or super-class which is legal to call,
        // then write out an invoke w/nulls using casts to avoid abigous crapo

        ConstructorNode c = selectAccessibleConstructorFromSuper(node);
        if (c != null) {
            out.print("super(");

            Parameter[] params = c.getParameters();
            for (int i=0; i<params.length; i++) {
                out.print("(");
                printType(params[i].getType(), out);
                out.print(")");
                out.print("null");

                if (i + 1 < params.length) {
                    out.print(", ");
                }
            }
            
            out.println(");");
            return;
        }

        // Otherwise try the older method based on the constructor's call expression
        Expression arguments = constrCall.getArguments();

        if (constrCall.isSuperCall()) {
            out.print("super(");
        }
        else {
            out.print("this(");
        }

        // Else try to render some arguments
        if (arguments instanceof ArgumentListExpression) {
            ArgumentListExpression argumentListExpression = (ArgumentListExpression) arguments;
            List args = argumentListExpression.getExpressions();

            for (Iterator it = args.iterator(); it.hasNext();) {
                Expression arg = (Expression) it.next();

                if (arg instanceof ConstantExpression) {
                    ConstantExpression expression = (ConstantExpression) arg;
                    Object o = expression.getValue();

                    if (o instanceof String) {
                        out.print("(String)null");
                    }
                    else {
                        out.print(expression.getText());
                    }
                }
                else {
                    printDefaultValue(out, arg.getType().getName());
                }

                if (arg != args.get(args.size() - 1)) {
                    out.print(", ");
                }
            }
        }

        out.println(");");
    }

    private void genMethod(MethodNode methodNode, PrintWriter out) {
        if (!methodNode.getDeclaringClass().isInterface())
            printModifiers(out, methodNode.getModifiers());

        printType(methodNode.getReturnType(), out);
        out.print(" ");
        out.print(methodNode.getName());

        printParams(methodNode, out);

        if ((methodNode.getModifiers() & Opcodes.ACC_ABSTRACT) != 0) {
            out.println(";");
        } else {
            out.print(" { ");
            ClassNode retType = methodNode.getReturnType();
            printReturn(out, retType);
            out.println("}");
        }

    }

    private void printReturn(PrintWriter out, ClassNode retType) {
        String retName = retType.getName();
        if (!retName.equals("void")) {
            out.print("return ");

            printDefaultValue(out, retName);

            out.print(";");
        }
    }

    private void printDefaultValue(PrintWriter out, String retName) {
        if (retName.equals("int")
            || retName.equals("byte")
            || retName.equals("short")
            || retName.equals("long")
            || retName.equals("float")
            || retName.equals("double")
            || retName.equals("char"))
        {
            //
            // NOTE: Always cast to avoid any abigous muck
            //
            
            out.print("(");
            out.print(retName);
            out.print(")");
            
            out.print("0");
        }
        else if (retName.equals("boolean")) {
            out.print("false");
        }
        else {
            out.print("null");
        }
    }

    private void printType(ClassNode type, PrintWriter out) {
        //
        // NOTE: Only render generics for type if we are allowed to use Java5 stuff
        //
        if (java5 && type.isUsingGenerics()) {
            GenericsType[] types = type.getGenericsTypes();

            out.print(type.getName());
            out.print("<");
            
            for (int i = 0; i < types.length; i++) {
                if (i != 0) {
                    out.print(", ");
                }
                out.print(types[i]);
            }
            
            out.print(">");
        }
        else if (type.isArray()) {
            out.print(type.getComponentType().getName());
            out.print("[]");
        }
        else {
            out.print(type.getName());
        }
    }
    private void printParams(MethodNode methodNode, PrintWriter out) {
        out.print("(");
        Parameter[] parameters = methodNode.getParameters();

        if (parameters != null && parameters.length != 0) {
            for (int i = 0; i != parameters.length; ++i) {
                printType(parameters[i].getType(), out);
                
                out.print(" ");
                out.print(parameters[i].getName());

                if (i + 1 < parameters.length) {
                    out.print(", ");
                }
            }
        }
        
        out.print(")");
    }

    private void printModifiers(PrintWriter out, int modifiers) {
        if ((modifiers & Opcodes.ACC_PUBLIC) != 0)
            out.print("public ");

        if ((modifiers & Opcodes.ACC_PROTECTED) != 0)
            out.print("protected ");

        if ((modifiers & Opcodes.ACC_PRIVATE) != 0)
            out.print("private ");

        if ((modifiers & Opcodes.ACC_STATIC) != 0)
            out.print("static ");

        if ((modifiers & Opcodes.ACC_SYNCHRONIZED) != 0)
            out.print("synchronized ");

        if ((modifiers & Opcodes.ACC_ABSTRACT) != 0)
            out.print("abstract ");
    }

    private void genImports(ClassNode classNode, PrintWriter out) {
        HashSet imports = new HashSet();

        //
        // HACK: Add the default imports... since things like Closure and GroovyObject seem to parse out w/o fully qualified classnames.
        //
        for (int i=0; i<ResolveVisitor.DEFAULT_IMPORTS.length; i++) {
            imports.add(ResolveVisitor.DEFAULT_IMPORTS[i]);
        }
        
        ModuleNode moduleNode = classNode.getModule();
        for (Iterator it = moduleNode.getImportPackages().iterator(); it.hasNext();) {
            imports.add(it.next());
        }

        for (Iterator it = moduleNode.getImports().iterator(); it.hasNext();) {
            ImportNode imp = (ImportNode) it.next();
            String name = imp.getType().getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot != -1)
                imports.add(name.substring(0, lastDot + 1));
        }

        for (Iterator it = imports.iterator(); it.hasNext();) {
            String imp = (String) it.next();
            out.print("import ");
            out.print(imp);
            out.println("*;");
        }
        out.println();
    }

    public void clean() {
        for (Iterator it = toCompile.iterator(); it.hasNext();) {
            String path = (String) it.next();
            new File(outputPath, path + ".java").delete();
        }
    }
}
