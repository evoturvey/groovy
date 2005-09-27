/*
 * $Id$
 * 
 * Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.
 * 
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided that the
 * following conditions are met:
 *  1. Redistributions of source code must retain copyright statements and
 * notices. Redistributions must also contain a copy of this document.
 *  2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  3. The name "groovy" must not be used to endorse or promote products
 * derived from this Software without prior written permission of The Codehaus.
 * For written permission, please contact info@codehaus.org.
 *  4. Products derived from this Software may not be called "groovy" nor may
 * "groovy" appear in their names without prior written permission of The
 * Codehaus. "groovy" is a registered trademark of The Codehaus.
 *  5. Due credit should be given to The Codehaus - http://groovy.codehaus.org/
 * 
 * THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *  
 */
package org.codehaus.groovy.ast;

import groovy.lang.GroovyObject;
import groovy.lang.MissingClassException;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.ClassGeneratorException;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a class declaration
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class ClassNode extends AnnotatedNode implements Opcodes {

    private static final String[] defaultImports = {"java.lang", "java.util", "groovy.lang", "groovy.util"};

    private transient Logger log = Logger.getLogger(getClass().getName());

    private Type type;
    private int modifiers;
    private Type superClass;
    private String[] interfaces;
    private MixinNode[] mixins;
    private List constructors = new ArrayList();
    private List methods = new ArrayList();
    private List fields = new ArrayList();
    private List properties = new ArrayList();
    private Map fieldIndex = new HashMap();
    private ModuleNode module;
    private CompileUnit compileUnit;
    private boolean staticClass = false;
    private boolean scriptBody = false;
    private boolean script;
    private ClassNode superClassNode;


    //br added to track the enclosing method for local inner classes
    private MethodNode enclosingMethod = null;

    public MethodNode getEnclosingMethod() {
        return enclosingMethod;
    }

    public void setEnclosingMethod(MethodNode enclosingMethod) {
        this.enclosingMethod = enclosingMethod;
    }


    /**
     * @param name       is the full name of the class
     * @param modifiers  the modifiers,
     * @param superClass the base class name - use "java.lang.Object" if no direct
     *                   base class
     * @see org.objectweb.asm.Opcodes
     */
    public ClassNode(Type type, int modifiers, Type superClass) {
        this(type, modifiers, superClass, EMPTY_STRING_ARRAY, MixinNode.EMPTY_ARRAY);
    }

    /**
     * @param name       is the full name of the class
     * @param modifiers  the modifiers,
     * @param superClass the base class name - use "java.lang.Object" if no direct
     *                   base class
     * @see org.objectweb.asm.Opcodes
     */
    public ClassNode(Type type, int modifiers, Type superClass, String[] interfaces, MixinNode[] mixins) {
        this.type = type;
        this.modifiers = modifiers;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.mixins = mixins;

        //br for better JVM comformance
        /*if ((modifiers & ACC_SUPER) == 0) {
            this.modifiers += ACC_SUPER;
        }*/
    }

    public Type getSuperClass() {
        return superClass;
    }

    public void setSuperClass(Type superClass) {
        this.superClass = superClass;
    }

    public List getFields() {
        return fields;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public MixinNode[] getMixins() {
        return mixins;
    }

    public List getMethods() {
        return methods;
    }

    public List getAbstractMethods() {

        List result = new ArrayList();
        for (Iterator methIt = getAllDeclaredMethods().iterator(); methIt.hasNext();) {
            MethodNode method = (MethodNode) methIt.next();
            if (method.isAbstract()) {
                result.add(method);
            }
        }
        if (result.size() == 0) {
            return null;
        }
        else {
            return result;
        }
    }

    public List getAllDeclaredMethods() {
        return new ArrayList(getDeclaredMethodsMap().values());
    }


    protected Map getDeclaredMethodsMap() {
        // Start off with the methods from the superclass.
        ClassNode parent = getSuperClassNode();
        Map result = null;
        if (parent != null) {
            result = parent.getDeclaredMethodsMap();
        }
        else {
            result = new HashMap();
        }

        // add in unimplemented abstract methods from the interfaces
        for (int i = 0; i < interfaces.length; i++) {
            String interfaceName = interfaces[i];
            ClassNode iface = findClassNode(Type.makeType(interfaceName));
            Map ifaceMethodsMap = iface.getDeclaredMethodsMap();
            for (Iterator iter = ifaceMethodsMap.keySet().iterator(); iter.hasNext();) {
                String methSig = (String) iter.next();
                if (!result.containsKey(methSig)) {
                    MethodNode methNode = (MethodNode) ifaceMethodsMap.get(methSig);
                    result.put(methSig, methNode);
                }
            }
        }

        // And add in the methods implemented in this class.
        for (Iterator iter = getMethods().iterator(); iter.hasNext();) {
            MethodNode method = (MethodNode) iter.next();
            String sig = method.getTypeDescriptor();
            if (result.containsKey(sig)) {
                MethodNode inheritedMethod = (MethodNode) result.get(sig);
                if (inheritedMethod.isAbstract()) {
                    result.put(sig, method);
                }
            }
            else {
                result.put(sig, method);
            }
        }
        return result;
    }

    protected int findMatchingMethodInList(MethodNode method, List methods) {
        for (int i = 0; i < methods.size(); i++) {
            MethodNode someMeth = (MethodNode) methods.get(i);
            if (someMeth.getName().equals(method.getName())
                    && parametersEqual(someMeth.getParameters(), method.getParameters())) {
                return i;
            }
        }
        return -1;
    }

    public Type getType() {
        return type;
    }

    public int getModifiers() {
        return modifiers;
    }

    public List getProperties() {
        return properties;
    }

    public List getDeclaredConstructors() {
        return constructors;
    }

    public ModuleNode getModule() {
        return module;
    }

    public void setModule(ModuleNode module) {
        this.module = module;
        if (module != null) {
            this.compileUnit = module.getUnit();
        }
    }

    public void addField(FieldNode node) {
        node.setDeclaringClass(this);
        node.setOwner(getType());
        fields.add(node);
        fieldIndex.put(node.getName(), node);
    }

    public void addProperty(PropertyNode node) {
        node.setDeclaringClass(this);
        FieldNode field = node.getField();
        addField(field);

        properties.add(node);
    }

    public PropertyNode addProperty(String name,
                                    int modifiers,
                                    Type type,
                                    Expression initialValueExpression,
                                    Statement getterBlock,
                                    Statement setterBlock) {
        PropertyNode node =
                new PropertyNode(name, modifiers, type, getType(), initialValueExpression, getterBlock, setterBlock);
        addProperty(node);
        return node;
    }

    public void addConstructor(ConstructorNode node) {
        node.setDeclaringClass(this);
        constructors.add(node);
    }

    public ConstructorNode addConstructor(int modifiers, Parameter[] parameters, Statement code) {
        ConstructorNode node = new ConstructorNode(modifiers, parameters, code);
        addConstructor(node);
        return node;
    }

    public void addMethod(MethodNode node) {
        node.setDeclaringClass(this);
        methods.add(node);
    }

    /**
     * IF a method with the given name and parameters is already defined then it is returned
     * otherwise the given method is added to this node. This method is useful for
     * default method adding like getProperty() or invokeMethod() where there may already
     * be a method defined in a class and  so the default implementations should not be added
     * if already present.
     */
    public MethodNode addMethod(String name,
                                int modifiers,
                                Type returnType,
                                Parameter[] parameters,
                                Statement code) {
        MethodNode other = getDeclaredMethod(name, parameters);
        // lets not add duplicate methods
        if (other != null) {
            return other;
        }
        MethodNode node = new MethodNode(name, modifiers, returnType, parameters, code);
        addMethod(node);
        return node;
    }

    /**
     * Adds a synthetic method as part of the compilation process
     */
    public MethodNode addSyntheticMethod(String name,
                                         int modifiers,
                                         Type returnType,
                                         Parameter[] parameters,
                                         Statement code) {
        MethodNode answer = addMethod(name, modifiers, returnType, parameters, code);
        answer.setSynthetic(true);
        return answer;
    }

    public FieldNode addField(String name, int modifiers, Type type, Expression initialValue) {
        FieldNode node = new FieldNode(name, modifiers, type, getType(), initialValue);
        addField(node);
        return node;
    }

    public void addInterface(String name) {
        // lets check if it already implements an interface
        boolean skip = false;
        for (int i = 0; i < interfaces.length; i++) {
            if (name.equals(interfaces[i])) {
                skip = true;
            }
        }
        if (!skip) {
            String[] newInterfaces = new String[interfaces.length + 1];
            System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
            newInterfaces[interfaces.length] = name;
            interfaces = newInterfaces;
        }
    }

    public void addMixin(MixinNode mixin) {
        // lets check if it already uses a mixin
        boolean skip = false;
        Type mixinName = mixin.getType();
        for (int i = 0; i < mixins.length; i++) {
            if (mixinName.equals(mixins[i].getType())) {
                skip = true;
            }
        }
        if (!skip) {
            MixinNode[] newMixins = new MixinNode[mixins.length + 1];
            System.arraycopy(mixins, 0, newMixins, 0, mixins.length);
            newMixins[mixins.length] = mixin;
            mixins = newMixins;
        }
    }

    public FieldNode getField(String name) {
        return (FieldNode) fieldIndex.get(name);
    }

    /**
     * @return the field node on the outer class or null if this is not an
     *         inner class
     */
    public FieldNode getOuterField(String name) {
        return null;
    }

    /**
     * Helper method to avoid casting to inner class
     *
     * @return
     */
    public ClassNode getOuterClass() {
        return null;
    }

    public void addStaticInitializerStatements(List staticStatements) {
        MethodNode method = null;
        List declaredMethods = getDeclaredMethods("<clinit>");
        if (declaredMethods.isEmpty()) {
            method =
                    addMethod("<clinit>", ACC_PUBLIC | ACC_STATIC, Type.VOID_TYPE, Parameter.EMPTY_ARRAY, new BlockStatement());
            method.setSynthetic(true);
        }
        else {
            method = (MethodNode) declaredMethods.get(0);
        }
        BlockStatement block = null;
        Statement statement = method.getCode();
        if (statement == null) {
            block = new BlockStatement();
        }
        else if (statement instanceof BlockStatement) {
            block = (BlockStatement) statement;
        }
        else {
            block = new BlockStatement();
            block.addStatement(statement);
        }
        block.addStatements(staticStatements);
    }

    /**
     * @return a list of methods which match the given name
     */
    public List getDeclaredMethods(String name) {
        List answer = new ArrayList();
        for (Iterator iter = methods.iterator(); iter.hasNext();) {
            MethodNode method = (MethodNode) iter.next();
            if (name.equals(method.getName())) {
                answer.add(method);
            }
        }
        return answer;
    }

    /**
     * @return a list of methods which match the given name
     */
    public List getMethods(String name) {
        List answer = new ArrayList();
        ClassNode node = this;
        do {
            for (Iterator iter = node.methods.iterator(); iter.hasNext();) {
                MethodNode method = (MethodNode) iter.next();
                if (name.equals(method.getName())) {
                    answer.add(method);
                }
            }
            node = node.getSuperClassNode();
        }
        while (node != null);
        return answer;
    }

    /**
     * @return the method matching the given name and parameters or null
     */
    public MethodNode getDeclaredMethod(String name, Parameter[] parameters) {
        for (Iterator iter = methods.iterator(); iter.hasNext();) {
            MethodNode method = (MethodNode) iter.next();
            if (name.equals(method.getName()) && parametersEqual(method.getParameters(), parameters)) {
                return method;
            }
        }
        return null;
    }

    /**
     * @return true if this node is derived from the given class node
     */
    public boolean isDerivedFrom(Type type) {
        ClassNode node = getSuperClassNode();
        while (node != null) {
            if (type.equals(node.getType())) {
                return true;
            }
            node = node.getSuperClassNode();
        }
        return false;
    }

    /**
     * @return true if this class is derived from a groovy object
     *         i.e. it implements GroovyObject
     */
    public boolean isDerivedFromGroovyObject() {
        return implementsInteface(GroovyObject.class.getName());
    }

    /**
     * @param name the fully qualified name of the interface
     * @return true if this class or any base class implements the given interface
     */
    public boolean implementsInteface(String name) {
        ClassNode node = this;
        do {
            if (node.declaresInterface(name)) {
                return true;
            }
            node = node.getSuperClassNode();
        }
        while (node != null);
        return false;
    }

    /**
     * @param name the fully qualified name of the interface
     * @return true if this class declares that it implements the given interface
     */
    public boolean declaresInterface(String name) {
        int size = interfaces.length;
        for (int i = 0; i < size; i++) {
            if (name.equals(interfaces[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the ClassNode of the super class of this type
     */
    public ClassNode getSuperClassNode() {
        if (superClass != null && superClassNode == null && getType()!=Type.OBJECT_TYPE) {
            // lets try find the class in the compile unit
            Type temp = resolveClassName(superClass);
            if (temp == null) {
                throw new MissingClassException(superClass.getName(), this, "No such superclass");
            }
            else {
                superClass = temp;
            }
            superClassNode = findClassNode(superClass);
        }
        return superClassNode;
    }

    /**
     * Attempts to lookup the fully qualified class name in the compile unit or classpath
     *
     * @param type fully qulified type name
     * @return the ClassNode for this type or null if it could not be found
     */
    public ClassNode findClassNode(Type type) {
        ClassNode answer = null;
        CompileUnit theCompileUnit = getCompileUnit();
        if (theCompileUnit != null) {
            answer = theCompileUnit.getClass(type.getName());
            if (answer != null) return answer;
            if (type.getTypeClass()!=null) {
                return createClassNode(type.getTypeClass());
            }
            Class theClass;
            try {
                theClass = theCompileUnit.loadClass(type);
                type.setTypeClass(theClass);
                answer = createClassNode(theClass);
            }
            catch (ClassNotFoundException e) {
                // lets ignore class not found exceptions
                log.log(Level.WARNING, "Cannot find class: " + type.getName(), e);
            }
        }
        return answer;
    }

    protected ClassNode createClassNode(Class theClass) {
        Class[] classInterfaces = theClass.getInterfaces();
        int size = classInterfaces.length;
        String[] interfaceNames = new String[size];
        for (int i = 0; i < size; i++) {
            interfaceNames[i] = classInterfaces[i].getName();
        }

        Type superClass = null;
        if (theClass.getSuperclass() != null) {
            superClass = Type.makeType(theClass.getSuperclass());
        }
        ClassNode answer =
                new ClassNode(Type.makeType(theClass),
                        theClass.getModifiers(),
                        superClass,
                        interfaceNames,
                        MixinNode.EMPTY_ARRAY);
        answer.compileUnit = getCompileUnit();
        Method[] declaredMethods = theClass.getDeclaredMethods();
        for (int i = 0; i < declaredMethods.length; i++) {
            answer.addMethod(createMethodNode(declaredMethods[i]));
        }
        Constructor[] declaredConstructors = theClass.getDeclaredConstructors();
        for (int i = 0; i < declaredConstructors.length; i++) {
            answer.addConstructor(createConstructorNode(declaredConstructors[i]));
        }
        return answer;
    }


    /**
     * Factory method to create a new ConstructorNode via reflection
     */
    private ConstructorNode createConstructorNode(Constructor constructor) {
        Parameter[] parameters = createParameters(constructor.getParameterTypes());
        return new ConstructorNode(constructor.getModifiers(), parameters, EmptyStatement.INSTANCE);
    }

    /**
     * Factory method to create a new MethodNode via reflection
     */
    protected MethodNode createMethodNode(Method method) {
        Parameter[] parameters = createParameters(method.getParameterTypes());
        return new MethodNode(method.getName(), method.getModifiers(), Type.makeType(method.getReturnType()), parameters, EmptyStatement.INSTANCE);
    }

    /**
     * @param types
     * @return
     */
    protected Parameter[] createParameters(Class[] types) {
        Parameter[] parameters = Parameter.EMPTY_ARRAY;
        int size = types.length;
        if (size > 0) {
            parameters = new Parameter[size];
            for (int i = 0; i < size; i++) {
                parameters[i] = createParameter(types[i], i);
            }
        }
        return parameters;
    }

    protected Parameter createParameter(Class parameterType, int idx) {
        return new Parameter(Type.makeType(parameterType), "param" + idx);
    }

    public Type resolveClassName(Type type) {
        return resolveClassName(type,null);
    }
    
    public Type resolveClassName(Type type, String message) {
        if (type.getTypeClass()!=null) return type;
        Type answer = null;
        if (type != null) {
            if (getType().equals(type) || getNameWithoutPackage().equals(type.getName())) {
                return getType();
            }
            // try to resolve Class names
            answer = tryResolveClassAndInnerClass(type);

            // try to resolve a public static inner class' name
            String replacedPointType = type.getName();
            while (answer == null && replacedPointType.indexOf('.') > -1) {
                int lastPoint = replacedPointType.lastIndexOf('.');
                replacedPointType = new StringBuffer()
                        .append(replacedPointType.substring(0, lastPoint)).append("$")
                        .append(replacedPointType.substring(lastPoint + 1)).toString();
                answer = tryResolveClassAndInnerClass(Type.makeType(replacedPointType));
            }
        }
        if (answer==null && message!=null) throw new MissingClassException(type,message);
        return answer;
    }

    private Type tryResolveClassAndInnerClass(Type type) {
        if (type.getTypeClass()!=null) return type;
        Type answer = tryResolveClassFromCompileUnit(type);
        if (answer == null) {
            // lets try class in same package
            String packageName = getPackageName();
            if (packageName != null && packageName.length() > 0) {
                answer = tryResolveClassFromCompileUnit(Type.makeType(packageName + "." + type.getName()));
            }
        }
        if (answer == null) {
            // lets try use the packages imported in the module
            if (module != null) {
                //System.out.println("Looking up inside the imported packages: " + module.getImportPackages());

                for (Iterator iter = module.getImportPackages().iterator(); iter.hasNext();) {
                    String packageName = (String) iter.next();
                    answer = tryResolveClassFromCompileUnit(Type.makeType(packageName + type.getName()));
                    if (answer != null) {
                        return answer;
                    }
                }
            }
        }
        if (answer == null) {
            for (int i = 0, size = defaultImports.length; i < size; i++) {
                String packagePrefix = defaultImports[i];
                answer = tryResolveClassFromCompileUnit(Type.makeType(packagePrefix + "." + type.getName()));
                if (answer != null) {
                    return answer;
                }
            }
        }
        return answer;
    }

    /**
     * @param type
     * @return
     */
    protected Type tryResolveClassFromCompileUnit(Type type) {
        if (type.getTypeClass()!=null) return type;
        CompileUnit theCompileUnit = getCompileUnit();
        if (theCompileUnit != null) {
            if (theCompileUnit.getClass(type.getName()) != null) {
                return type;
            }

            try {
                theCompileUnit.loadClass(type);
                return type;
            } catch (AccessControlException ace) {
                //Percolate this for better diagnostic info
                throw ace;
            } catch (ClassGeneratorException cge) {
                throw cge;
            } catch (ClassNotFoundException e) {
                //Fall through
            }
        }
        return null;
    }

    public CompileUnit getCompileUnit() {
        if (compileUnit == null && module != null) {
            compileUnit = module.getUnit();
        }
        return compileUnit;
    }

    /**
     * @return true if the two arrays are of the same size and have the same contents
     */
    protected boolean parametersEqual(Parameter[] a, Parameter[] b) {
        if (a.length == b.length) {
            boolean answer = true;
            for (int i = 0; i < a.length; i++) {
                if (!a[i].getType().equals(b[i].getType())) {
                    answer = false;
                    break;
                }
            }
            return answer;
        }
        return false;
    }

    /**
     * @return the package name of this class
     */
    public String getPackageName() {
        int idx = getType().getName().lastIndexOf('.');
        if (idx > 0) {
            return getType().getName().substring(0, idx);
        }
        return null;
    }

    public String getNameWithoutPackage() {
        int idx = getType().getName().lastIndexOf('.');
        if (idx > 0) {
            return getType().getName().substring(idx + 1);
        }
        return getType().getName();
    }

    public void visitContents(GroovyClassVisitor visitor) {
        
        // now lets visit the contents of the class
        for (Iterator iter = getProperties().iterator(); iter.hasNext();) {
            PropertyNode pn = (PropertyNode) iter.next();
            visitor.visitProperty(pn);
        }

        for (Iterator iter = getFields().iterator(); iter.hasNext();) {
            FieldNode fn = (FieldNode) iter.next();
            visitor.visitField(fn);
        }

        for (Iterator iter = getDeclaredConstructors().iterator(); iter.hasNext();) {
            ConstructorNode cn = (ConstructorNode) iter.next();
            visitor.visitConstructor(cn);
        }

        for (Iterator iter = getMethods().iterator(); iter.hasNext();) {
            MethodNode mn = (MethodNode) iter.next();
            visitor.visitMethod(mn);
        }
    }

    public MethodNode getGetterMethod(String getterName) {
        for (Iterator iter = methods.iterator(); iter.hasNext();) {
            MethodNode method = (MethodNode) iter.next();
            if (getterName.equals(method.getName())
                    && Type.VOID_TYPE!=method.getReturnType()
                    && method.getParameters().length == 0) {
                return method;
            }
        }
        return null;
    }

    public MethodNode getSetterMethod(String getterName) {
        for (Iterator iter = methods.iterator(); iter.hasNext();) {
            MethodNode method = (MethodNode) iter.next();
            if (getterName.equals(method.getName())
                    && Type.VOID_TYPE==method.getReturnType()
                    && method.getParameters().length == 1) {
                return method;
            }
        }
        return null;
    }

    /**
     * Is this class delcared in a static method (such as a closure / inner class declared in a static method)
     *
     * @return
     */
    public boolean isStaticClass() {
        return staticClass;
    }

    public void setStaticClass(boolean staticClass) {
        this.staticClass = staticClass;
    }

    /**
     * @return Returns true if this inner class or closure was declared inside a script body
     */
    public boolean isScriptBody() {
        return scriptBody;
    }

    public void setScriptBody(boolean scriptBody) {
        this.scriptBody = scriptBody;
    }

    public boolean isScript() {
        return script | isDerivedFrom(Type.SCRIPT_TYPE);
    }

    public void setScript(boolean script) {
        this.script = script;
    }

    public String toString() {
        return super.toString() + "[name: " + getType().getName() + "]";
    }

    /**
     * Returns true if the given method has a possibly matching method with the given name and arguments
     */
    public boolean hasPossibleMethod(String name, Expression arguments) {
        int count = 0;

        if (arguments instanceof TupleExpression) {
            TupleExpression tuple = (TupleExpression) arguments;
            // TODO this won't strictly be true when using list expension in argument calls
            count = tuple.getExpressions().size();
        }
        ClassNode node = this;
        do {
            for (Iterator iter = node.methods.iterator(); iter.hasNext();) {
                MethodNode method = (MethodNode) iter.next();
                if (name.equals(method.getName()) && method.getParameters().length == count) {
                    return true;
                }
            }
            node = node.getSuperClassNode();
        }
        while (node != null);
        return false;
    }
    
    public boolean isInterface(){
        return (getModifiers() & Opcodes.ACC_INTERFACE) > 0; 
    }
}
