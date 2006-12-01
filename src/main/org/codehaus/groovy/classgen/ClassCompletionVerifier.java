/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 * Groovy community - subsequent modifications
 ******************************************************************************/
package org.codehaus.groovy.classgen;

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.objectweb.asm.Opcodes;
import org.codehaus.groovy.syntax.Types;

/**
 * ClassCompletionVerifier
 */
public class ClassCompletionVerifier extends ClassCodeVisitorSupport {

    private ClassNode currentClass;
    private SourceUnit source;

    public ClassCompletionVerifier(SourceUnit source) {
        this.source = source;
    }

    public ClassNode getClassNode() {
        return currentClass;
    }

    public void visitClass(ClassNode node) {
        ClassNode oldClass = currentClass;
        currentClass = node;
        checkImplementsAndExtends(node);
        if (source != null && !source.getErrorCollector().hasErrors()) {
            checkClassForIncorrectModifiers(node);
            checkClassForOverwritingFinal(node);
            checkMethodsForIncorrectModifiers(node);
            checkMethodsForOverwritingFinal(node);
            checkNoAbstractMethodsNonabstractClass(node);
        }
        super.visitClass(node);
        currentClass = oldClass;
    }

    private void checkNoAbstractMethodsNonabstractClass(ClassNode node) {
        if (Modifier.isAbstract(node.getModifiers())) return;
        List abstractMethods = node.getAbstractMethods();
        if (abstractMethods == null) return;
        for (Iterator iter = abstractMethods.iterator(); iter.hasNext();) {
            MethodNode method = (MethodNode) iter.next();
            String methodName = method.getTypeDescriptor();
            addError("Can't have an abstract method in a non-abstract class." +
                    " The class '" + node.getName() + "' must be declared abstract or" +
                    " the method '" + methodName + "' must be implemented.", node);
        }
    }

    private void checkClassForIncorrectModifiers(ClassNode node) {
        checkClassForAbstractAndFinal(node);
        checkClassForOtherModifiers(node);
    }

    private void checkClassForAbstractAndFinal(ClassNode node) {
        if (!Modifier.isAbstract(node.getModifiers())) return;
        if (!Modifier.isFinal(node.getModifiers())) return;
        if (node.isInterface()) {
            addError("The interface '" + node.getName() +"' must not be final. It is by definition abstract.", node);
        } else {
            addError("The class '" + node.getName() + "' must not be both final and abstract.", node);
        }
    }

    private void checkClassForOtherModifiers(ClassNode node) {
        checkClassForModifier(node, Modifier.isTransient(node.getModifiers()), "transient");
        checkClassForModifier(node, Modifier.isVolatile(node.getModifiers()), "volatile");
    }

    private void checkClassForModifier(ClassNode node, boolean condition, String modifierName) {
        if (!condition) return;
        addError("The " + (node.isInterface() ? "interface" : "class") + " '" + node.getName() +
                "' has an incorrect modifier " + modifierName + ".", node);
    }

    private void checkAbstractDeclaration(MethodNode methodNode) {
        if (!Modifier.isAbstract(methodNode.getModifiers())) return;
        if (Modifier.isAbstract(currentClass.getModifiers())) return;
        addError("Can't have an abstract method in a non-abstract class." +
                " The class '" + currentClass.getName() + "' must be declared abstract or the method '" +
                methodNode.getTypeDescriptor() + "' must not be abstract.", methodNode);
    }

    private void checkClassForOverwritingFinal(ClassNode cn) {
        ClassNode superCN = cn.getSuperClass();
        if (superCN == null) return;
        if (!Modifier.isFinal(superCN.getModifiers())) return;
        StringBuffer msg = new StringBuffer();
        msg.append("You are not allowed to overwrite the final class ");
        msg.append(superCN.getName());
        msg.append(".");
        addError(msg.toString(), cn);
    }

    private void checkImplementsAndExtends(ClassNode node) {
        ClassNode cn = node.getSuperClass();
        if (cn.isInterface() && !node.isInterface()) {
            addError("You are not allowed to extend the Interface " + cn.getName() + ", use implements instead.", node);
        }
        ClassNode[] interfaces = node.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            cn = interfaces[i];
            if (!cn.isInterface()) {
                addError("You are not allowed to implement the Class " + cn.getName() + ", use extends instead.", node);
            }
        }
    }

    private void checkMethodsForIncorrectModifiers(ClassNode cn) {
        if (!cn.isInterface()) return;
        List methods = cn.getMethods();
        for (Iterator cnIter = methods.iterator(); cnIter.hasNext();) {
            MethodNode method = (MethodNode) cnIter.next();
            if (Modifier.isFinal(method.getModifiers())) {
                addError("Method '" + method.getName() + "' from Interface '" +
                        cn.getName() + "' must not be final. It is by definition abstract.", method);
            }
            if (Modifier.isStatic(method.getModifiers()) && !isConstructor(method)) {
                addError("Method '" + method.getName() + "' from Interface '" +
                        cn.getName() + "' must not be static. Only fields may be static in an interface.", method);
            }
        }
    }

    private boolean isConstructor(MethodNode method) {
        return method.getName().equals("<clinit>");
    }

    private void checkMethodsForOverwritingFinal(ClassNode cn) {
        List methods = cn.getMethods();
        for (Iterator cnIter = methods.iterator(); cnIter.hasNext();) {
            MethodNode method = (MethodNode) cnIter.next();
            Parameter[] params = method.getParameters();
            for (ClassNode superCN = cn.getSuperClass(); superCN != null; superCN = superCN.getSuperClass()) {
                List superMethods = superCN.getMethods(method.getName());
                for (Iterator iter = superMethods.iterator(); iter.hasNext();) {
                    MethodNode superMethod = (MethodNode) iter.next();
                    Parameter[] superParams = superMethod.getParameters();
                    if (!hasEqualParameterTypes(params, superParams)) continue;
                    if (!Modifier.isFinal(superMethod.getModifiers())) return;
                    addInvalidUseOfFinalError(method, params, superCN);
                    return;
                }
            }
        }
    }

    private void addInvalidUseOfFinalError(MethodNode method, Parameter[] parameters, ClassNode superCN) {
        StringBuffer msg = new StringBuffer();
        msg.append("You are not allowed to overwrite the final method ").append(method.getName());
        msg.append("(");
        boolean needsComma = false;
        for (int i = 0; i < parameters.length; i++) {
            if (needsComma) {
                msg.append(",");
            } else {
                needsComma = true;
            }
            msg.append(parameters[i].getType());
        }
        msg.append(") from class ").append(superCN.getName());
        msg.append(".");
        addError(msg.toString(), method);
    }

    private boolean hasEqualParameterTypes(Parameter[] first, Parameter[] second) {
        if (first.length != second.length) return false;
        for (int i = 0; i < first.length; i++) {
            String ft = first[i].getType().getName();
            String st = second[i].getType().getName();
            if (ft.equals(st)) continue;
            return false;
        }
        return true;
    }

    protected SourceUnit getSourceUnit() {
        return source;
    }

    public void visitConstructorCallExpression(ConstructorCallExpression call) {
        ClassNode type = call.getType();
        if (Modifier.isAbstract(type.getModifiers())) {
            addError("You cannot create an instance from the abstract class " + type.getName() + ".", call);
        }
        super.visitConstructorCallExpression(call);
    }

    public void visitMethod(MethodNode node) {
        checkAbstractDeclaration(node);
        checkRepetitiveMethod(node);
        super.visitMethod(node);
    }

    private void checkRepetitiveMethod(MethodNode node) {
        if (isConstructor(node)) return;
        List methods = currentClass.getMethods(node.getName());
        for (Iterator iter = methods.iterator(); iter.hasNext();) {
            MethodNode element = (MethodNode) iter.next();
            if (element == node) continue;
            if (!element.getDeclaringClass().equals(node.getDeclaringClass())) continue;
            Parameter[] p1 = node.getParameters();
            Parameter[] p2 = element.getParameters();
            if (p1.length != p2.length) continue;
            addErrorIfParamsAndReturnTypeEqual(p2, p1, node, element);
        }
    }

    private void addErrorIfParamsAndReturnTypeEqual(Parameter[] p2, Parameter[] p1,
                                                    MethodNode node, MethodNode element) {
        boolean isEqual = true;
        for (int i = 0; i < p2.length; i++) {
            isEqual &= p1[i].equals(p2[i]);
        }
        isEqual &= node.getReturnType().equals(element.getReturnType());
        if (isEqual) {
            addError("Repetitive method name/signature for method " + node.getName() +
                    " in class " + currentClass.getName() + ".", node);
        }
    }

    public void visitField(FieldNode node) {
        if (currentClass.getField(node.getName()) != node) {
            addError("The field " + node.getName() + " is declared multiple times.", node);
        }
        checkInterfaceFieldModifiers(node);
        super.visitField(node);
    }

    private void checkInterfaceFieldModifiers(FieldNode node) {
        if (!currentClass.isInterface()) return;
        if ((node.getModifiers() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == 0) {
            addError("The field " + node.getName() + " is not 'public final static' but part of the interface " +
                    currentClass.getName() + ".", node);
        }
    }

    public void visitBinaryExpression(BinaryExpression expression) {
        if (expression.getOperation().getType() == Types.LEFT_SQUARE_BRACKET &&
                expression.getRightExpression() instanceof MapEntryExpression) {
            addError("You tried to use a map entry for an index operation, this is not allowed. " +
                    "Maybe something should be set in parentheses or a comma is missing?",
                    expression.getRightExpression());
        }
        super.visitBinaryExpression(expression);
    }

    public void visitCatchStatement(CatchStatement cs) {
        if (!(cs.getExceptionType().isDerivedFrom(ClassHelper.make(Throwable.class)))) {
            addError("Catch statement parameter type is not a subclass of Throwable.", cs);
        }
        super.visitCatchStatement(cs);
    }

    public void visitClassExpression(ClassExpression expression) {
        ClassNode type = expression.getType();
        CompileUnit cu = currentClass.getCompileUnit();
        if (!type.isResolved() && cu.hasClassNodeToCompile()) {
            String location = cu.getScriptSourceLocation(type.getName());
            if (location == null) return;
            addError("Expected to find the class " + type.getName() +
                    " in " + location + ", but failed to find it.", expression);
        }
    }
}
