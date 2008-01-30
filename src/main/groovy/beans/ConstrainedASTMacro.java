/*
 * Copyright 2008 the original author or authors.
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

package groovy.beans;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.Collection;

/**
 * @author Danno Ferrin (shemnon)
 */
public class ConstrainedASTMacro extends BoundASTMacro {

    FieldNode vcsField;

    public static boolean hasConstrainedAnnotation(AnnotatedNode node) {
        for (AnnotationNode annotation : (Collection<AnnotationNode>) node.getAnnotations().values()) {
            if (Constrained.class.getName().equals(annotation.getClassNode().getName())) {
                return true;
            }
        }
        return false;
    }

    public void visit(AnnotationNode node, AnnotatedNode parent, SourceUnit source, GeneratorContext context) {
        boolean bound = BoundASTMacro.hasBoundAnnotation(parent);

        ClassNode declaringClass = parent.getDeclaringClass();
        FieldNode field = ((FieldNode)parent);
        String fieldName = field.getName();
        for (PropertyNode propertyNode : (Collection<PropertyNode>) declaringClass.getProperties()) {
            if (propertyNode.getName().equals(fieldName)) {

                if (bound && needsPropertyChangeSupport(declaringClass)) {
                    addPropertyChangeSupport(declaringClass);
                }
                if (needsVetoableChangeSupport(declaringClass)) {
                    addVetoableChangeSupport(declaringClass);
                }
                String setterName = "set" + MetaClassHelper.capitalize(propertyNode.getName());
                if (declaringClass.getMethods(setterName).isEmpty()) {
                    Expression fieldExpression = new FieldExpression(field);
                    BlockStatement setterBlock = new BlockStatement();
                    setterBlock.addStatement(createConstrainedStatement(field, fieldExpression));
                    if (bound) {
                        setterBlock.addStatement(createBoundStatement(field, fieldExpression));
                    } else {
                        setterBlock.addStatement(createSetStatement(fieldExpression));
                    }

                    // create method void <setter>(<type> fieldName)
                    createSetterMethod(declaringClass, field, setterName, setterBlock);
                } else {
                    source.getErrorCollector().addErrorAndContinue(
                        new SyntaxErrorMessage(new SyntaxException(
                            "@groovy.beans.Constrained cannot handle user generated setters.",
                            node.getLineNumber(),
                            node.getColumnNumber()),
                            source));
                }
                return;
            }
        }
        source.getErrorCollector().addErrorAndContinue(
            new SyntaxErrorMessage(new SyntaxException(
                "@groovy.beans.Constrained must be on a property, not a field.  Try removing the private, protected, or public modifier.",
                node.getLineNumber(),
                node.getColumnNumber()),
            source));
    }

    protected Statement createConstrainedStatement(FieldNode field, Expression fieldExpression) {
        // create statementBody this$propertyChangeSupport.firePropertyChange("field", field, field = value);
        return new ExpressionStatement(
            new MethodCallExpression(
                new FieldExpression(vcsField),
                    "fireVetoableChange",
                        new ArgumentListExpression(
                            new Expression[] {
                                new ConstantExpression(field.getName()),
                                fieldExpression,
                                new VariableExpression("value")})));
    }

    protected Statement createSetStatement(Expression fieldExpression) {
        return new ExpressionStatement(
            new BinaryExpression(
                fieldExpression,
                Token.newSymbol(Types.EQUAL, 0, 0),
                new VariableExpression("value")));
    }

    protected boolean needsVetoableChangeSupport(ClassNode declaringClass) {
        while (declaringClass != null) {
            for (FieldNode field : (Collection<FieldNode>) declaringClass.getFields()) {
                if (field.getType() == null) {
                    continue;
                }
                if (VetoableChangeSupport.class.getName().equals(field.getType().getName())) {
                    vcsField = field;
                    return false;
                }
            }
            //TODO check add/remove conflicts
            declaringClass = declaringClass.getSuperClass();
        }
        return true;
    }

    protected void createSetterMethod(ClassNode declaringClass, FieldNode field, String setterName, Statement setterBlock) {
        Parameter[] setterParameterTypes = { new Parameter(field.getType(), "value")};
        ClassNode[] exceptions = {new ClassNode(PropertyVetoException.class)};
        MethodNode setter =
            new MethodNode(setterName, field.getModifiers(), ClassHelper.VOID_TYPE, setterParameterTypes, exceptions, setterBlock);
        setter.setSynthetic(true);
        // add it to the class
        declaringClass.addMethod(setter);
    }


    protected void addVetoableChangeSupport(ClassNode declaringClass) {
        ClassNode vcsClassNode = ClassHelper.make(VetoableChangeSupport.class);
        ClassNode vclClassNode = ClassHelper.make(VetoableChangeListener.class);

        // add field protected static VetoableChangeSupport this$vetoableChangeSupport = new java.beans.VetoableChangeSupport(this)
        vcsField = declaringClass.addField(
            "this$vetoableChangeSupport",
            ACC_FINAL | ACC_PROTECTED | ACC_SYNTHETIC,
            vcsClassNode,
            new ConstructorCallExpression(vcsClassNode,
                new ArgumentListExpression(new Expression[] {new VariableExpression("this")})));

        // add method void addVetoableChangeListener(listner) {
        //     this$vetoableChangeSupport.addVetoableChangeListner(listener)
        //  }
        declaringClass.addMethod(
            new MethodNode(
                "addVetoableChangeListener",
                ACC_PUBLIC | ACC_SYNTHETIC,
                ClassHelper.VOID_TYPE,
                new Parameter[] {new Parameter(vclClassNode, "listener")},
                ClassNode.EMPTY_ARRAY,
                new ExpressionStatement(
                    new MethodCallExpression(
                        new FieldExpression(vcsField),
                        "addVetoableChangeListener",
                        new ArgumentListExpression(
                            new Expression[] {new VariableExpression("listener")})))));
        // add method void addVetoableChangeListener(name, listner) {
        //     this$vetoableChangeSupport.addVetoableChangeListner(name, listener)
        //  }
        declaringClass.addMethod(
            new MethodNode(
                "addVetoableChangeListener",
                ACC_PUBLIC | ACC_SYNTHETIC,
                ClassHelper.VOID_TYPE,
                new Parameter[] {new Parameter(ClassHelper.STRING_TYPE, "name"), new Parameter(vclClassNode, "listener")},
                ClassNode.EMPTY_ARRAY,
                new ExpressionStatement(
                    new MethodCallExpression(
                        new FieldExpression(vcsField),
                        "addVetoableChangeListener",
                        new ArgumentListExpression(
                            new Expression[] {new VariableExpression("name"), new VariableExpression("listener")})))));

        // add method boolean removeVetoableChangeListener(listner) {
        //    return this$vetoableChangeSupport.removeVetoableChangeListener(listener);
        // }
        declaringClass.addMethod(
            new MethodNode(
                "removeVetoableChangeListener",
                ACC_PUBLIC | ACC_SYNTHETIC,
                ClassHelper.VOID_TYPE,
                new Parameter[] {new Parameter(vclClassNode, "listener")},
                ClassNode.EMPTY_ARRAY,
                new ExpressionStatement(
                    new MethodCallExpression(
                        new FieldExpression(vcsField),
                        "removeVetoableChangeListener",
                        new ArgumentListExpression(
                            new Expression[] {new VariableExpression("listener")})))));
        // add method void removeVetoableChangeListener(name, listner)
        declaringClass.addMethod(
            new MethodNode(
                "removeVetoableChangeListener",
                ACC_PUBLIC | ACC_SYNTHETIC,
                ClassHelper.VOID_TYPE,
                new Parameter[] {new Parameter(ClassHelper.STRING_TYPE, "name"), new Parameter(vclClassNode, "listener")},
                ClassNode.EMPTY_ARRAY,
                new ExpressionStatement(
                    new MethodCallExpression(
                        new FieldExpression(vcsField),
                        "removeVetoableChangeListener",
                        new ArgumentListExpression(
                            new Expression[] {new VariableExpression("name"), new VariableExpression("listener")})))));
        // add VetoableChangeSupport[] getVetoableChangeListeners() {
        //   return this$vetoableChangeSupport.getVetoableChangeListeners
        // }
        declaringClass.addMethod(
            new MethodNode(
                "getVetoableChangeListeners",
                ACC_PUBLIC | ACC_SYNTHETIC,
                vclClassNode.makeArray(),
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                new ReturnStatement(
                    new ExpressionStatement(
                        new MethodCallExpression(
                            new FieldExpression(vcsField),
                            "getVetoableChangeListeners",
                            ArgumentListExpression.EMPTY_ARGUMENTS)))));
    }

}