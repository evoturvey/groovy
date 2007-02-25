/*
 * Copyright 2005 John G. Wilson
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
 *
 */

package groovy.lang;

import java.lang.reflect.Method;
import java.util.List;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.runtime.metaclass.MetaBeanProperty;

/**
 * @author John Wilson
 *
 */

public class DelegatingMetaClass implements MetaClass, MutableMetaClass {
    protected MetaClass delegate;
    
    public DelegatingMetaClass(final MetaClass delegate) {
        this.delegate = delegate;
    }
   
    public DelegatingMetaClass(final Class theClass) {
        this(GroovySystem.getMetaClassRegistry().getMetaClass(theClass.getSuperclass() == null ? Object.class : theClass.getSuperclass()).createMetaClass(theClass, GroovySystem.getMetaClassRegistry()));
    }
    
    public MetaClass createMetaClass(Class theClass, MetaClassRegistry registry) {
        return this.delegate.createMetaClass(theClass, registry);
    }

    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#addNewInstanceMethod(java.lang.reflect.Method)
     */
    public void addNewInstanceMethod(Method method) {
        if(delegate instanceof MutableMetaClass)
            ((MutableMetaClass)delegate).addNewInstanceMethod(method);
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#addNewStaticMethod(java.lang.reflect.Method)
     */
    public void addNewStaticMethod(Method method) {
        if(delegate instanceof MutableMetaClass)
            ((MutableMetaClass)delegate).addNewStaticMethod(method);
    }

    public void addMetaMethod(MetaMethod metaMethod) {
        if(delegate instanceof MutableMetaClass)
            ((MutableMetaClass)delegate).addMetaMethod(metaMethod);
    }

    public void addMetaBeanProperty(MetaBeanProperty metaBeanProperty) {
        if(delegate instanceof MutableMetaClass)
            ((MutableMetaClass)delegate).addMetaBeanProperty(metaBeanProperty);
    }

    /* (non-Javadoc)
    * @see groovy.lang.MetaClass#initialize()
    */
    public void initialize() {
        delegate.initialize();
    }

    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#getAttribute(java.lang.Object, java.lang.String)
     */
    public Object getAttribute(Object object, String attribute) {
        return delegate.getAttribute(object, attribute);
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#getClassNode()
     */
    public ClassNode getClassNode() {
         return delegate.getClassNode();
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#getMetaMethods()
     */
    public List getMetaMethods() {
        return delegate.getMetaMethods();
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#getMethods()
     */
    public List getMethods() {
        return delegate.getMethods();
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#getProperties()
     */
    public List getProperties() {
        return delegate.getProperties();
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#getProperty(java.lang.Object, java.lang.String)
     */
    public Object getProperty(Object object, String property) {
        return delegate.getProperty(object, property);
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#invokeConstructor(java.lang.Object[])
     */
    public Object invokeConstructor(Object[] arguments) {
        return delegate.invokeConstructor(arguments);
    }

    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#invokeMethod(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public Object invokeMethod(Object object, String methodName, Object arguments) {
        return delegate.invokeMethod(object, methodName, arguments);
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#invokeMethod(java.lang.Object, java.lang.String, java.lang.Object[])
     */
    public Object invokeMethod(Object object, String methodName, Object[] arguments) {
        return delegate.invokeMethod(object, methodName, arguments);
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#invokeStaticMethod(java.lang.Object, java.lang.String, java.lang.Object[])
     */
    public Object invokeStaticMethod(Object object, String methodName, Object[] arguments) {
        return delegate.invokeStaticMethod(object, methodName, arguments);
    }

    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#setAttribute(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public void setAttribute(Object object, String attribute, Object newValue) {
        delegate.setAttribute(object, attribute, newValue);
    }
    /* (non-Javadoc)
     * @see groovy.lang.MetaClass#setProperty(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public void setProperty(Object object, String property, Object newValue) {
        delegate.setProperty(object, property, newValue);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return delegate.hashCode();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return delegate.toString();
    }
    /**
     * @deprecated
     */
    public MetaMethod pickMethod(String methodName, Class[] arguments) {
        return delegate.pickMethod(methodName,arguments);
    }

    public Object getAttribute(Class sender, Object receiver, String messageName, boolean useSuper) {
        return this.delegate.getAttribute(sender, receiver, messageName, useSuper);
    }

    public Object getProperty(Class sender, Object receiver, String messageName, boolean useSuper, boolean fromInsideClass) {
        return this.delegate.getProperty(sender, receiver, messageName, useSuper, fromInsideClass);
    }

    public Class getTheClass() {
        return this.delegate.getTheClass();
    }

    public Object invokeConstructorAt(Class at, Object[] arguments) {
        return this.delegate.invokeConstructorAt(at, arguments);
    }

    public Object invokeMethod(Class sender, Object receiver, String methodName, Object[] arguments, boolean isCallToSuper, boolean fromInsideClass) {
        return this.delegate.invokeMethod(sender, receiver, methodName, arguments, isCallToSuper, fromInsideClass);
    }

    public Object invokeMissingMethod(Object instance, String methodName, Object[] arguments) {
        return this.delegate.invokeMissingMethod(instance, methodName, arguments);
    }

    public boolean isGroovyObject() {
        return GroovyObject.class.isAssignableFrom(this.delegate.getTheClass());
    }

    public void setAttribute(Class sender, Object receiver, String messageName, Object messageValue, boolean useSuper, boolean fromInsideClass) {
        this.delegate.setAttribute(sender, receiver, messageName, messageValue, useSuper, fromInsideClass);
    }

    public void setProperty(Class sender, Object receiver, String messageName, Object messageValue, boolean useSuper, boolean fromInsideClass) {
        this.delegate.setProperty(sender, receiver, messageName, messageValue, useSuper, fromInsideClass);
    }

    public int selectConstructorAndTransformArguments(int numberOfCosntructors, Object[] arguments) {
        return this.delegate.selectConstructorAndTransformArguments(numberOfCosntructors, arguments);
    }

	public void setAdaptee(MetaClass adaptee) {
		this.delegate = adaptee; 
	}
}
