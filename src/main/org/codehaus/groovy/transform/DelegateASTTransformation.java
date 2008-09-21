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

package org.codehaus.groovy.transform;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.objectweb.asm.Opcodes;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

/**
 * Handles generation of code for the @Delegate annotation
 *
 * @author Alex Tkachman
 */
@GroovyASTTransformation(phase= CompilePhase.CANONICALIZATION)
public class DelegateASTTransformation implements ASTTransformation, Opcodes {

    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (!(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode node = (AnnotationNode) nodes[0];

        if (parent instanceof FieldNode) {
            FieldNode fieldNode = (FieldNode) parent;
            final ClassNode type = fieldNode.getType();
            final Map fieldMethods = type.getDeclaredMethodsMap();
            final ClassNode owner = fieldNode.getOwner();

            final Map ownMethods = owner.getDeclaredMethodsMap();
            for (Iterator it = fieldMethods.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry e = (Map.Entry) it.next();

                addDelegateMethod(fieldNode, owner, ownMethods, e);
            }

            final Expression member = node.getMember("interfaces");

            if(member instanceof ConstantExpression && ((ConstantExpression)member).getValue().equals(false))
              return;

            final Set allInterfaces = type.getAllInterfaces();
            final Set ownerIfaces = owner.getAllInterfaces();
            for (Iterator it = allInterfaces.iterator(); it.hasNext(); ) {
                ClassNode iface = (ClassNode) it.next();
                if (!ownerIfaces.contains(iface)) {
                    final ClassNode[] ifaces = owner.getInterfaces();
                    final ClassNode[] newIfaces = new ClassNode[ifaces.length + 1];
                    System.arraycopy(ifaces, 0, newIfaces, 0, ifaces.length);
                    newIfaces[ifaces.length] = iface;
                    owner.setInterfaces(newIfaces);
                }
            }
        }
    }

    private void addDelegateMethod(FieldNode fieldNode, ClassNode owner, Map ownMethods, Map.Entry e) {
        MethodNode method = (MethodNode) e.getValue();
        if (!method.isPublic())
            return;

        if (!ownMethods.containsKey(e.getKey())) {
            final ArgumentListExpression args = new ArgumentListExpression();
            final Parameter[] params = method.getParameters();
            final Parameter[] newParams = new Parameter[params.length];
            for (int i = 0; i < newParams.length; i++) {
                Parameter newParam = new Parameter(params[i].getType(), params[i].getName());
                newParams[i] = newParam;
                args.addExpression(new VariableExpression(newParam));
            }
            owner.addMethod(method.getName(),
                    method.getModifiers() & (~ACC_ABSTRACT),
                    method.getReturnType(),
                    newParams,
                    method.getExceptions(),
                    new ExpressionStatement(
                            new MethodCallExpression(
                                    new FieldExpression(fieldNode),
                                    method.getName(),
                                    args)));
        }
    }
}