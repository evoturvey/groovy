/*
 $Id$

 Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.

 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.

 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.

 3. The name "groovy" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Codehaus.  For written permission,
    please contact info@codehaus.org.

 4. Products derived from this Software may not be called "groovy"
    nor may "groovy" appear in their names without prior written
    permission of The Codehaus. "groovy" is a registered
    trademark of The Codehaus.

 5. Due credit should be given to The Codehaus -
    http://groovy.codehaus.org/

 THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package org.codehaus.groovy.runtime;

import groovy.lang.*;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.runtime.wrappers.GroovyObjectWrapper;
import org.codehaus.groovy.runtime.wrappers.PojoWrapper;
import org.codehaus.groovy.runtime.wrappers.Wrapper;

/**
 * A static helper class to interface bytecode and runtime 
 *
 * @author Jochen Theodorou
 * @version $Revision$
 */
public class ScriptBytecodeAdapter {
    public static final Object[] EMPTY_ARGS = {};
    private static final Integer ZERO = new Integer(0);
    private static final Integer MINUS_ONE = new Integer(-1);
    private static final Integer ONE = new Integer(1);

    //  --------------------------------------------------------
    //                   exception handling
    //  --------------------------------------------------------
    private static Object unwrap(GroovyRuntimeException gre) throws Throwable{
        Throwable th = gre;
        if (th.getCause()!=null && th.getCause()!=gre) th=th.getCause();
        if (th!=gre && (th instanceof GroovyRuntimeException)) unwrap((GroovyRuntimeException) th);
        throw th;
    }
    
    //  --------------------------------------------------------
    //                       methods for this
    //  --------------------------------------------------------
    public static Object invokeMethodOnCurrentN(Class senderClass, GroovyObject receiver, String messageName, Object[] messageArguments) throws Throwable{
        try {
            return InvokerHelper.invokeMethod(receiver, messageName, messageArguments);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object invokeMethodOnCurrentNSafe(Class senderClass, GroovyObject receiver, String messageName, Object[] messageArguments) throws Throwable{
        return invokeMethodOnCurrentN(senderClass,receiver,messageName,messageArguments);
    }
    
    public static Object invokeMethodOnCurrentNSpreadSafe(Class senderClass, GroovyObject receiver, String messageName, Object[] messageArguments) throws Throwable{
        if (! (receiver instanceof List)) return invokeMethodOnCurrentN(senderClass,receiver,messageName, messageArguments);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(invokeMethodNSafe(senderClass, it.next(), messageName, messageArguments));
        }
        return answer;
    }
    
    public static Object invokeMethodOnCurrent0(Class senderClass, GroovyObject receiver, String messageName)  throws Throwable{
        return invokeMethodOnCurrentN(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    public static Object invokeMethodOnCurrent0Safe(Class senderClass, GroovyObject receiver, String messageName, Object[] messageArguments) throws Throwable{
        return invokeMethodOnCurrentNSafe(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    public static Object invokeMethodOnCurrent0SpreadSafe(Class senderClass, GroovyObject receiver, String messageName, Object[] messageArguments) throws Throwable{
        return invokeMethodOnCurrentNSpreadSafe(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    //  --------------------------------------------------------
    //                       methods for super
    //  --------------------------------------------------------
    public static Object invokeMethodOnSuperN(Class senderClass, Object receiver, String messageName, Object[] messageArguments) throws Throwable{
        try {
            return InvokerHelper.invokeMethod(receiver, messageName, messageArguments);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object invokeMethodOnSuperNSafe(Class senderClass, Object receiver, String messageName, Object[] messageArguments) throws Throwable{
        return invokeMethodOnSuperN(senderClass,receiver,messageName,messageArguments);
    }
    
    public static Object invokeMethodOnSuperNSpreadSafe(Class senderClass, Object receiver, String messageName, Object[] messageArguments) throws Throwable{
        if (! (receiver instanceof List)) return invokeMethodOnSuperN(senderClass,receiver,messageName, messageArguments);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(invokeMethodNSafe(senderClass,it.next(), messageName, messageArguments));
        }
        return answer;
    }
    
    public static Object invokeMethodOnSuper0(Class senderClass, Object receiver, String messageName)  throws Throwable{
        return invokeMethodOnSuperN(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    public static Object invokeMethodOnSuper0Safe(Class senderClass, Object receiver, String messageName, Object[] messageArguments) throws Throwable{
        return invokeMethodOnSuperNSafe(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    public static Object invokeMethodOnSuper0SpreadSafe(Class senderClass, Object receiver, String messageName, Object[] messageArguments) throws Throwable{
        return invokeMethodOnSuperNSpreadSafe(senderClass,receiver,messageName,EMPTY_ARGS);
    }

    //  --------------------------------------------------------
    //              normal method invocation
    //  --------------------------------------------------------       
    public static Object invokeMethodN(Class senderClass, Object receiver, String messageName, Object[] messageArguments) throws Throwable{
        try {
            return InvokerHelper.invokeMethod(receiver, messageName, messageArguments);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object invokeMethodNSafe(Class senderClass, Object receiver, String messageName, Object[] messageArguments) throws Throwable{
        if (receiver==null) return null;
        return invokeMethodN(senderClass,receiver,messageName,messageArguments);
    }
    
    public static Object invokeMethodNSpreadSafe(Class senderClass, Object receiver, String messageName, Object[] messageArguments) throws Throwable{
        if (receiver==null) return null;
        if (! (receiver instanceof List)) return invokeMethodN(senderClass,receiver,messageName, messageArguments);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(invokeMethodNSafe(senderClass, it.next(), messageName, messageArguments));
        }
        return answer;
    }
    
    public static Object invokeMethod0(Class senderClass, Object receiver, String messageName)  throws Throwable{
        return invokeMethodN(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    public static Object invokeMethod0Safe(Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        return invokeMethodNSafe(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    public static Object invokeMethod0SpreadSafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        return invokeMethodNSpreadSafe(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    //  --------------------------------------------------------
    //                static normal method invocation
    //  --------------------------------------------------------       
    public static Object invokeStaticMethodN(Class senderClass, Class receiver, String messageName, Object[] messageArguments) throws Throwable{
        try {
            return InvokerHelper.invokeStaticMethod(receiver, messageName, messageArguments);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object invokeStaticMethod0(Class senderClass, Class receiver, String messageName)  throws Throwable{
        return invokeStaticMethodN(senderClass,receiver,messageName,EMPTY_ARGS);
    }
    
    //  --------------------------------------------------------
    //              normal constructor invocation (via new)
    //  --------------------------------------------------------       
    public static Object invokeNewN(Class senderClass, Class receiver, Object arguments) throws Throwable{
        try {
            return InvokerHelper.invokeConstructorAt(senderClass,receiver, arguments);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }  
    }
    
    public static Object invokeNew0(Class senderClass, Class receiver) throws Throwable {
        return invokeNewN(senderClass, receiver, EMPTY_ARGS);
    }

    //  --------------------------------------------------------
    //       special constructor invocation (via this/super)
    //  --------------------------------------------------------       

    public static int selectConstructorAndTransformArguments(Object[] arguments, int numberOfCosntructors, Class which) {
        MetaClassImpl metaClass = (MetaClassImpl) InvokerHelper.getInstance().getMetaRegistry().getMetaClass(which);
        return metaClass.selectConstructorAndTransformArguments(numberOfCosntructors, arguments);
    }


    //  --------------------------------------------------------
    //               field handling this: get
    //  --------------------------------------------------------       

    public static Object getFieldOnCurrent(Class senderClass, Object receiver, String messageName) throws Throwable{
        try {
            return InvokerHelper.getAttribute(receiver, messageName);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object getFieldOnCurrentSafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        return getFieldOnCurrent(senderClass,receiver,messageName);
    }
    
    public static Object getFieldOnCurrentSpreadSafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        if (! (receiver instanceof List)) return getFieldOnCurrent(senderClass,receiver,messageName);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(getFieldOnCurrent(senderClass, it.next(), messageName));
        }
        return answer;
    }

    //  --------------------------------------------------------
    //              field handling super: get
    //  --------------------------------------------------------       

    public static Object getFieldOnSuper(Class senderClass, Object receiver, String messageName) throws Throwable{
        try {
            return InvokerHelper.getAttribute(receiver, messageName);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object getFieldOnSuperSafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        return getFieldOnSuper(senderClass,receiver,messageName);
    }
    
    public static Object getFieldOnSuperSpreadSafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        if (! (receiver instanceof List)) return getFieldOnSuper(senderClass,receiver,messageName);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(getFieldOnSuper(senderClass, it.next(), messageName));
        }
        return answer;
    }

    //  --------------------------------------------------------
    //              field handling super: set
    //  --------------------------------------------------------       

    public static void setFieldOnSuper(Object messageArgument,Class senderClass, Object receiver, String messageName) throws Throwable{
        try {
            InvokerHelper.setAttribute(receiver, messageName,messageArgument);
        } catch (GroovyRuntimeException gre) {
            unwrap(gre);
        }
    }
    
    public static void setFieldOnSuperSafe(Object messageArgument,Class senderClass, Object receiver, String messageName) throws Throwable{
        setFieldOnSuper(messageArgument,senderClass,receiver,messageName);
    }
    
    public static void setFieldOnSuperSpreadSafe(Object messageArgument,Class senderClass, Object receiver, String messageName) throws Throwable{
        if (! (receiver instanceof List)) {
            setFieldOnSuper(messageArgument,senderClass,receiver,messageName);
            return;
        }
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            setFieldOnSuper(messageArgument,senderClass, it.next(), messageName);
        }
    }

    
    //  --------------------------------------------------------
    //              normal field handling : get
    //  --------------------------------------------------------       

    public static Object getField(Class senderClass, Object receiver, String messageName) throws Throwable{
        try {
            return InvokerHelper.getAttribute(receiver, messageName);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object getFieldSafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        return getField(senderClass,receiver,messageName);
    }
    
    public static Object getFieldSpreadSafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        if (! (receiver instanceof List)) return getField(senderClass,receiver,messageName);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(getFieldSafe(senderClass, it.next(), messageName));
        }
        return answer;
    }

    //  --------------------------------------------------------
    //              normal field handling : set
    //  --------------------------------------------------------       

    public static void setField(Object messageArgument, Class senderClass, Object receiver, String messageName) throws Throwable{
        try {
            InvokerHelper.setAttribute(receiver, messageName,messageArgument);
        } catch (GroovyRuntimeException gre) {
            unwrap(gre);
        }
    }
    
    public static void setFieldSafe(Object messageArgument, Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return;
        setField(messageArgument,senderClass,receiver,messageName);
    }
    
    public static void setFieldSpreadSafe(Object messageArgument, Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return;
        if (! (receiver instanceof List)) {
            setField(messageArgument,senderClass,receiver,messageName);
            return;
        }
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            setFieldSafe(messageArgument,senderClass, it.next(), messageName);
        }
    }
    
    //  --------------------------------------------------------
    //              normal GroovyObject field handling : get
    //  --------------------------------------------------------       

    public static Object getGroovyObjectField(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        return receiver.getMetaClass().getAttribute(receiver,messageName);
    }
    
    public static Object getGroovyObjectFieldSafe(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        return receiver.getMetaClass().getAttribute(receiver,messageName);
    }
    
    public static Object getGroovyObjectFieldSpreadSafe(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        if (! (receiver instanceof List)) return getGroovyObjectField(senderClass,receiver,messageName);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(getFieldSafe(senderClass, it.next(), messageName));
        }
        return answer;
    }

    //  --------------------------------------------------------
    //              normal field handling : set
    //  --------------------------------------------------------       

    public static void setGroovyObjectField(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        receiver.getMetaClass().setAttribute(receiver,messageName,messageArgument);
    }
    
    public static void setGroovyObjectFieldSafe(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (receiver==null) return;
        receiver.getMetaClass().setAttribute(receiver,messageName,messageArgument);
    }
    
    public static void setGroovyObjectFieldSpreadSafe(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (receiver==null) return;
        if (! (receiver instanceof List)) {
            setGroovyObjectField(messageArgument,senderClass,receiver,messageName);
            return;
        }
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            setFieldSafe(messageArgument,senderClass, it.next(), messageName);
        }
    }

    //  --------------------------------------------------------
    //              Property handling super: get
    //  --------------------------------------------------------       

    public static Object getPropertyOnSuper(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        try {
            return InvokerHelper.getAttribute(receiver, messageName);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object getPropertyOnSuperSafe(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        return getPropertyOnSuper(senderClass,receiver,messageName);
    }
    
    public static Object getPropertyOnSuperSpreadSafe(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (! (receiver instanceof List)) return getPropertyOnSuper(senderClass,receiver,messageName);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(getPropertySafe(senderClass, it.next(), messageName));
        }
        return answer;
    }

    //  --------------------------------------------------------
    //              Property handling super: set
    //  --------------------------------------------------------       

    public static void setPropertyOnSuper(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        try {
            InvokerHelper.setAttribute(receiver, messageName,messageArgument);
        } catch (GroovyRuntimeException gre) {
            unwrap(gre);
        }
    }
    
    public static void setPropertyOnSuperSafe(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        setPropertyOnSuper(messageArgument, senderClass,receiver,messageName);
    }
    
    public static void setPropertyOnSuperSpreadSafe(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (! (receiver instanceof List)) {
            setPropertyOnSuper(messageArgument, senderClass,receiver,messageName);
            return;
        }
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            setPropertySafe(messageArgument, senderClass, it.next(), messageName);
        }
    }

    
    //  --------------------------------------------------------
    //              normal Property handling : get
    //  --------------------------------------------------------       

    public static Object getProperty(Class senderClass, Object receiver, String messageName) throws Throwable{
        try {
            return InvokerHelper.getProperty(receiver, messageName);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object getPropertySafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        return getProperty(senderClass,receiver,messageName);
    }
    
    public static Object getPropertySpreadSafe(Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        if (! (receiver instanceof List)) return getProperty(senderClass,receiver,messageName);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(getPropertySafe(senderClass, it.next(), messageName));
        }
        return answer;
    }

    //  --------------------------------------------------------
    //              normal Property handling : set
    //  --------------------------------------------------------       

    public static void setProperty(Object messageArgument, Class senderClass, Object receiver, String messageName) throws Throwable{
        try {
            InvokerHelper.setProperty(receiver, messageName,messageArgument);
        } catch (GroovyRuntimeException gre) {
            unwrap(gre);
        }
    }
    
    public static void setPropertySafe(Object messageArgument, Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return;
        setProperty(messageArgument,senderClass,receiver,messageName);
    }
    
    public static void setPropertySpreadSafe(Object messageArgument, Class senderClass, Object receiver, String messageName) throws Throwable{
        if (receiver==null) return;
        if (! (receiver instanceof List)) {
            setProperty(messageArgument, senderClass, receiver, messageName);
            return;
        }
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            setPropertySafe(messageArgument, senderClass, it.next(), messageName);
        }
    }
    
    
    //  --------------------------------------------------------
    //              normal GroovyObject Property handling : get
    //  --------------------------------------------------------       

    public static Object getGroovyObjectProperty(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        return receiver.getProperty(messageName);
    }
    
    public static Object getGroovyObjectPropertySafe(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        return receiver.getProperty(messageName);
    }
    
    public static Object getGroovyObjectPropertySpreadSafe(Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (receiver==null) return null;
        if (! (receiver instanceof List)) return getGroovyObjectProperty(senderClass,receiver,messageName);
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            answer.add(getPropertySafe(senderClass, it.next(), messageName));
        }
        return answer;
    }

    //  --------------------------------------------------------
    //              normal GroovyObject Property handling : set
    //  --------------------------------------------------------       

    public static void setGroovyObjectProperty(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        receiver.setProperty(messageName,messageArgument);
    }
    
    public static void setGroovyObjectPropertySafe(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (receiver==null) return;
        receiver.setProperty(messageName,messageArgument);
    }
    
    public static void setGroovyObjectPropertySpreadSafe(Object messageArgument, Class senderClass, GroovyObject receiver, String messageName) throws Throwable{
        if (receiver==null) return;
        if (! (receiver instanceof List)) {
            setProperty(messageArgument, senderClass, receiver, messageName);
            return;
        }
        
        List list = (List) receiver;
        List answer = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            setPropertySafe(messageArgument, senderClass, it.next(), messageName);
        }
    }
    
    //  **********************************************************************************
    //  **********************************************************************************
    //  **************          methods not covered by the new MOP          **************
    //  **********************************************************************************
    //  **********************************************************************************
    
    //  --------------------------------------------------------
    //                     Closures
    //  --------------------------------------------------------           
    
    /**
     * Returns the method pointer for the given object name
     */
    public static Closure getMethodPointer(Object object, String methodName) {
        return InvokerHelper.getMethodPointer(object, methodName);
    }
    
    // TODO: set sender class
    public static Object invokeClosure(Object closure, Object[] arguments) throws Throwable {
        return invokeMethodN(closure.getClass(), closure, "doCall", arguments);
    } 
        

    //  --------------------------------------------------------
    //                     type conversion
    //  --------------------------------------------------------           
          
    /**
     * Provides a hook for type coercion of the given object to the required type
     *
     * @param type   of object to convert the given object to
     * @param object the object to be converted
     * @return the original object or a new converted value
     * @throws Throwable 
     */
    public static Object asType(Object object, Class type) throws Throwable {
        return invokeMethodN(object.getClass(),object,"asType",new Object[]{type});
    }
    
    /**
     * Provides a hook for type casting of the given object to the required type
     * 
     * @param type   of object to convert the given object to
     * @param object the object to be converted
     * @return the original object or a new converted value
     * @throws Throwable 
     */
    public static Object castToType(Object object, Class type) throws Throwable{
        try {
            return DefaultTypeTransformation.castToType(object,type);
        } catch (GroovyRuntimeException gre) {
            return (Matcher) unwrap(gre);
        }
    }    

    public static Tuple createTuple(Object[] array) {
        return new Tuple(array);
    }

    public static List createList(Object[] values) {
        return InvokerHelper.createList(values);
    }
    
    public static Wrapper createPojoWrapper(Object val, Class clazz) {
        return new PojoWrapper(val,clazz);
    }

    public static Wrapper createGroovyObjectWrapper(GroovyObject val, Class clazz) {
        return new GroovyObjectWrapper(val,clazz);
    }
    
    public static Map createMap(Object[] values) {
        return InvokerHelper.createMap(values);
    }
    

    //TODO: refactor
    public static List createRange(Object from, Object to, boolean inclusive) throws Throwable {
        if (!inclusive) {
            if (compareEqual(from,to)){
                return new EmptyRange((Comparable)from);
            }
            if (compareGreaterThan(from, to)) {
                to = invokeMethod0(ScriptBytecodeAdapter.class, to, "next");
            }
            else {
                to = invokeMethod0(ScriptBytecodeAdapter.class, to, "previous");
            }
        }
        if (from instanceof Integer && to instanceof Integer) {
            return new IntRange(DefaultTypeTransformation.intUnbox(from), DefaultTypeTransformation.intUnbox(to));
        }
        else {
            return new ObjectRange((Comparable) from, (Comparable) to);
        }
    }
    
    //assert
    public static void assertFailed(Object expression, Object message) {
        InvokerHelper.assertFailed(expression,message);
    }

    //isCase
    //TODO: set sender class
    public static boolean isCase(Object switchValue, Object caseExpression) throws Throwable{
        if (caseExpression == null) {
            return switchValue == null;
        }
        return DefaultTypeTransformation.castToBoolean(invokeMethodN(caseExpression.getClass(), caseExpression, "isCase", new Object[]{switchValue}));
    }
    
    //compare
    public static boolean compareIdentical(Object left, Object right) {
        return left == right;
    }
    
    public static boolean compareEqual(Object left, Object right) {
        return DefaultTypeTransformation.compareEqual(left, right);
    }
    
    public static boolean compareNotEqual(Object left, Object right) {
        return !compareEqual(left, right);
    }
    
    public static Integer compareTo(Object left, Object right) {
        int answer = DefaultTypeTransformation.compareTo(left, right);
        if (answer == 0) {
            return ZERO;
        }
        else {
            return answer > 0 ? ONE : MINUS_ONE;
        }
    }    

    public static boolean compareLessThan(Object left, Object right) {
        return compareTo(left, right).intValue() < 0;
    }
    
    public static boolean compareLessThanEqual(Object left, Object right){
        return compareTo(left, right).intValue() <= 0;
    }
    
    public static boolean compareGreaterThan(Object left, Object right){
        return compareTo(left, right).intValue() > 0;
    }

    public static boolean compareGreaterThanEqual(Object left, Object right){
        return compareTo(left, right).intValue() >= 0;
    }

    //regexpr
    public static Pattern regexPattern(Object regex) {
        return DefaultGroovyMethods.negate(regex.toString());
    }
    
    public static Matcher findRegex(Object left, Object right) throws Throwable{
        try {
            return InvokerHelper.findRegex(left, right);
        } catch (GroovyRuntimeException gre) {
            return (Matcher) unwrap(gre);
        }
    }
    
    public static boolean matchRegex(Object left, Object right) {
        return InvokerHelper.matchRegex(left, right);
    }
    
    
    //spread expressions
    public static Object spreadList(Object value) {
        return InvokerHelper.spreadList(value);
    }

    public static Object spreadMap(Object value) {
        return InvokerHelper.spreadMap(value);
    }
    
    //negation
    public static Object negate(Object value) throws Throwable {
        try {
            return InvokerHelper.negate(value);
        } catch (GroovyRuntimeException gre) {
            return unwrap(gre);
        }
    }
    
    public static Object bitNegate(Object value) {
        return InvokerHelper.bitNegate(value);
    }

    public static MetaClass initMetaClass(Object object) {
        return InvokerHelper.getMetaClass(object);
    }
    
    private static MetaClass getMetaClassObjectNotNull(Object object) {
        if (!(object instanceof GroovyObject)) {
            return initMetaClass(object);
        } else {
            return ((GroovyObject) object).getMetaClass();
        }        
    }
    
    

}
