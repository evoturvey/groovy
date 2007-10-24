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

package groovy.util;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Mix of BuilderSupport and SwingBuilder's factory support.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author Andres Almiray <aalmiray@users.sourceforge.com>
 */
public abstract class FactoryBuilderSupport extends GroovyObjectSupport {
    public static final String CURRENT_FACTORY = "_CURRENT_FACTORY_";
    public static final String PARENT_FACTORY = "_PARENT_FACTORY_";
    public static final String PARENT_NODE = "_PARENT_NODE_";
    public static final String CURRENT_NODE = "_CURRENT_NODE_";
    public static final String PARENT_CONTEXT = "_PARENT_CONTEXT_";
    public static final String OWNER = "owner";
    private static final Logger LOG = Logger.getLogger( FactoryBuilderSupport.class.getName() );

    /**
     * Throws an exception if value is null.
     */
    public static void checkValueIsNull( Object value, Object name ) {
        if( value != null ){
            throw new RuntimeException( "'" + name + "' elements do not accept a value argument." );
        }
    }

    /**
     * Returns true if type is assignale to the value's class, false if value is
     * null.
     */
    public static boolean checkValueIsType( Object value, Object name, Class type ) {
        if( value != null ){
            if( type.isAssignableFrom( value.getClass() ) ){
                return true;
            }else{
                throw new RuntimeException( "The value argument of '" + name + "' must be of type "
                        + type.getName() );
            }
        }else{
            return false;
        }
    }

    /**
     * Returns true if type is assignale to the value's class, false if value is
     * null or a String.
     */
    public static boolean checkValueIsTypeNotString( Object value, Object name, Class type ) {
        if( value != null ){
            if( type.isAssignableFrom( value.getClass() ) ){
                return true;
            }else if( value instanceof String ){
                return false;
            }else{
                throw new RuntimeException( "The value argument of '" + name + "' must be of type "
                        + type.getName() + " or a String." );
            }
        }else{
            return false;
        }
    }

    private LinkedList/* <Map<String,Object>> */contexts = new LinkedList/* <Map<String,Object>> */();
    private LinkedList/* <Closure> */attributeDelegates = new LinkedList/* <Closure> */(); //
    private Map/* <String,Factory> */factories = new HashMap/* <String,Factory> */();
    private Closure nameMappingClosure;
    private FactoryBuilderSupport proxyBuilder;
    private LinkedList/* <Closure> */preInstantiateDelegates = new LinkedList/* <Closure> */();
    private LinkedList/* <Closure> */postInstantiateDelegates = new LinkedList/* <Closure> */();
    private LinkedList/* <Closure> */postNodeCompletionDelegates = new LinkedList/* <Closure> */();

    public FactoryBuilderSupport() {
        this.proxyBuilder = this;
    }

    public FactoryBuilderSupport( Closure nameMappingClosure ) {
        this.nameMappingClosure = nameMappingClosure;
        this.proxyBuilder = this;
    }

    /**
     * Returns the factory map.
     */
    public Map getFactories() {
        return Collections.unmodifiableMap( proxyBuilder.factories );
    }

    /**
     * Returns the context of the current node.
     */
    public Map getContext() {
        if( !proxyBuilder.contexts.isEmpty() ){
            return (Map) proxyBuilder.contexts.getFirst();
        }
        return null;
    }

    /**
     * Returns the current node being built.
     */
    public Object getCurrent() {
        if( !proxyBuilder.contexts.isEmpty() ){
            Map context = (Map) proxyBuilder.contexts.getFirst();
            return context.get( CURRENT_NODE );
        }
        return null;
    }

    /**
     * Returns the factory that built the current node.
     */
    public Factory getCurrentFactory() {
        if( !proxyBuilder.contexts.isEmpty() ){
            Map context = (Map) proxyBuilder.contexts.getFirst();
            return (Factory) context.get( CURRENT_FACTORY );
        }
        return null;
    }

    /**
     * Returns the factory of the parent of the current node.
     */
    public Factory getParentFactory() {
        if( !proxyBuilder.contexts.isEmpty() ){
            Map context = (Map) proxyBuilder.contexts.getFirst();
            return (Factory) context.get( PARENT_FACTORY );
        }
        return null;
    }

    /**
     * Returns the parent of the current node.
     */
    public Object getParentNode() {
        if( !proxyBuilder.contexts.isEmpty() ){
            Map context = (Map) proxyBuilder.contexts.getFirst();
            return context.get( PARENT_NODE );
        }
        return null;
    }

    /**
     * Returns the context of the parent of the current node.
     */
    public Map getParentContext() {
        if( !proxyBuilder.contexts.isEmpty() ){
            Map context = (Map) proxyBuilder.contexts.getFirst();
            return (Map) context.get( PARENT_CONTEXT );
        }
        return null;
    }

    /**
     * Convenience method when no arguments are required
     *
     * @return the result of the call
     * @param methodName the name of the method to invoke
     */
    public Object invokeMethod( String methodName ) {
        return proxyBuilder.invokeMethod( methodName, null );
    }

    public Object invokeMethod( String methodName, Object args ) {
        Object name = proxyBuilder.getName( methodName );
        Object result = null;
        try{
            result = proxyBuilder.doInvokeMethod( methodName, name, args );
        }catch( RuntimeException e ){
            proxyBuilder.reset();
            throw e;
        }
        return result;
    }

    /**
     * Add an attribute delegate so it can intercept attributes being set.
     * Attribute delegates are fire in a FILO pattern, so that nested delegates
     * get first crack.
     *
     * @param attrDelegate
     */
    public Closure addAttributeDelegate( Closure attrDelegate ) {
        proxyBuilder.attributeDelegates.addFirst( attrDelegate );
        return attrDelegate;
    }

    /**
     * Remove the most recently added instance of the attribute delegate.
     *
     * @param attrDelegate
     */
    public void removeAttributeDelegate( Closure attrDelegate ) {
        proxyBuilder.attributeDelegates.remove( attrDelegate );
    }

    /**
     * Add a preInstantiate delegate so it can intercept nodes before they are
     * created. PreInstantiate delegates are fire in a FILO pattern, so that
     * nested delegates get first crack.
     *
     * @param delegate
     */
    public Closure addPreInstantiateDelegate( Closure delegate ) {
        proxyBuilder.preInstantiateDelegates.addFirst( delegate );
        return delegate;
    }

    /**
     * Remove the most recently added instance of the preInstantiate delegate.
     *
     * @param delegate
     */
    public void removePreInstantiateDelegate( Closure delegate ) {
        proxyBuilder.preInstantiateDelegates.remove( delegate );
    }

    /**
     * Add a postInstantiate delegate so it can intercept nodes after they are
     * created. PostInstantiate delegates are fire in a FILO pattern, so that
     * nested delegates get first crack.
     *
     * @param delegate
     */
    public Closure addPostInstantiateDelegate( Closure delegate ) {
        proxyBuilder.postInstantiateDelegates.addFirst( delegate );
        return delegate;
    }

    /**
     * Remove the most recently added instance of the postInstantiate delegate.
     *
     * @param delegate
     */
    public void removePostInstantiateDelegate( Closure delegate ) {
        proxyBuilder.postInstantiateDelegates.remove( delegate );
    }

    /**
     * Add a nodeCompletion delegate so it can intercept nodes after they done
     * with building. NodeCompletion delegates are fire in a FILO pattern, so
     * that nested delegates get first crack.
     *
     * @param delegate
     */
    public Closure addPostNodeCompletionDelegate( Closure delegate ) {
        proxyBuilder.postNodeCompletionDelegates.addFirst( delegate );
        return delegate;
    }

    /**
     * Remove the most recently added instance of the nodeCompletion delegate.
     *
     * @param delegate
     */
    public void removePostNodeCompletionDelegate( Closure delegate ) {
        proxyBuilder.postNodeCompletionDelegates.remove( delegate );
    }

    /**
     * Registers a factory for a JavaBean.<br>
     * The JavaBean clas should have a no-args constructor.
     */
    public void registerBeanFactory( String theName, final Class beanClass ) {
        proxyBuilder.registerFactory( theName, new AbstractFactory(){
            public Object newInstance( FactoryBuilderSupport builder, Object name, Object value,
                    Map properties ) throws InstantiationException, IllegalAccessException {
                if( checkValueIsTypeNotString( value, name, beanClass ) ){
                    return value;
                }else{
                    return beanClass.newInstance();
                }
            }
        } );
    }

    /**
     * Registers a factory for a node name.
     */
    public void registerFactory( String name, Factory factory ) {
        proxyBuilder.factories.put( name, factory );
    }

    protected Object createNode( Object name, Map attributes, Object value ) {
        Object node = null;

        Factory factory = proxyBuilder.resolveFactory( name, attributes, value );
        if( factory == null ){
            LOG.log( Level.WARNING, "Could not find match for name '" + name + "'" );
            return null;
        }
        proxyBuilder.getContext().put( CURRENT_FACTORY, factory );
        proxyBuilder.preInstantiate( name, attributes, value );
        try{
            node = factory.newInstance( this, name, value, attributes );
            if( node == null ){
                LOG.log( Level.WARNING, "Factory for name '" + name + "' returned null" );
                return null;
            }

            if( LOG.isLoggable( Level.FINE ) ){
                LOG.fine( "For name: " + name + " created node: " + node );
            }
        }catch( Exception e ){
            throw new RuntimeException( "Failed to create component for '" + name + "' reason: "
                    + e, e );
        }
        proxyBuilder.postInstantiate( name, attributes, node );
        proxyBuilder.handleNodeAttributes( node, attributes );
        return node;
    }

    /**
     * Returns the Factory associated with name.<br>
     * This is a hook for subclasses to plugin a custom strategy for mapping
     * names to factories.
     */
    protected Factory resolveFactory( Object name, Map attributes, Object value ) {
        return (Factory) proxyBuilder.factories.get( name );
    }

    protected Object doInvokeMethod( String methodName, Object name, Object args ) {
        Object node = null;
        Closure closure = null;
        List list = InvokerHelper.asList( args );

        if( proxyBuilder.getContexts().isEmpty() ){
            // should be called on first build method only
            proxyBuilder.newContext();
        }
        switch( list.size() ){
            case 0:
                node = proxyBuilder.createNode( name, Collections.EMPTY_MAP, null );
                break;
            case 1: {
                Object object = list.get( 0 );
                if( object instanceof Map ){
                    node = proxyBuilder.createNode( name, (Map) object, null );
                }else if( object instanceof Closure ){
                    closure = (Closure) object;
                    node = proxyBuilder.createNode( name, Collections.EMPTY_MAP, null );
                }else{
                    node = proxyBuilder.createNode( name, Collections.EMPTY_MAP, object );
                }
            }
                break;
            case 2: {
                Object object1 = list.get( 0 );
                Object object2 = list.get( 1 );
                if( object1 instanceof Map ){
                    if( object2 instanceof Closure ){
                        closure = (Closure) object2;
                        node = proxyBuilder.createNode( name, (Map) object1, null );
                    }else{
                        node = proxyBuilder.createNode( name, (Map) object1, object2 );
                    }
                }else{
                    if( object2 instanceof Closure ){
                        closure = (Closure) object2;
                        node = proxyBuilder.createNode( name, Collections.EMPTY_MAP, object1 );
                    }else if( object2 instanceof Map ){
                        node = proxyBuilder.createNode( name, (Map) object2, object1 );
                    }else{
                        throw new MissingMethodException( name.toString(), getClass(),
                                list.toArray(), false );
                    }
                }
            }
                break;
            case 3: {
                Object arg0 = list.get( 0 );
                Object arg1 = list.get( 1 );
                Object arg2 = list.get( 2 );
                if( arg0 instanceof Map && arg2 instanceof Closure ){
                    closure = (Closure) arg2;
                    node = proxyBuilder.createNode( name, (Map) arg0, arg1 );
                }else if( arg1 instanceof Map && arg2 instanceof Closure ){
                    closure = (Closure) arg2;
                    node = proxyBuilder.createNode( name, (Map) arg1, arg0 );
                }else{
                    throw new MissingMethodException( name.toString(), getClass(), list.toArray(),
                            false );
                }
            }
                break;
            default: {
                throw new MissingMethodException( name.toString(), getClass(), list.toArray(),
                        false );
            }

        }

        if( node == null ){
            if( proxyBuilder.getContexts().size() == 1 ){
                // pop the first context
                proxyBuilder.popContext();
            }
            return node;
        }

        Object current = proxyBuilder.getCurrent();
        if( current != null ){
            proxyBuilder.setParent( current, node );
        }

        if( closure != null ){
            if( proxyBuilder.getCurrentFactory().isLeaf() ){
                throw new RuntimeException( "'" + name + "' doesn't support nesting." );
            }
            // push new node on stack
            Object parentFactory = proxyBuilder.getCurrentFactory();
            Map parentContext = proxyBuilder.getContext();
            proxyBuilder.newContext();
            proxyBuilder.getContext().put( OWNER, closure.getOwner() );
            proxyBuilder.getContext().put( CURRENT_NODE, node );
            proxyBuilder.getContext().put( PARENT_FACTORY, parentFactory );
            proxyBuilder.getContext().put( PARENT_NODE, current );
            proxyBuilder.getContext().put( PARENT_CONTEXT, parentContext );
            // lets register the builder as the delegate
            proxyBuilder.setClosureDelegate( closure, node );
            closure.call();
            proxyBuilder.popContext();
        }

        proxyBuilder.nodeCompleted( current, node );
        node = proxyBuilder.postNodeCompletion( current, node );
        if( proxyBuilder.getContexts()
                .size() == 1 ){
            // pop the first context
            proxyBuilder.popContext();
        }
        return node;
    }

    /**
     * A hook to allow names to be converted into some other object such as a
     * QName in XML or ObjectName in JMX.
     *
     * @param methodName the name of the desired method
     * @return the object representing the name
     */
    protected Object getName( String methodName ) {
        if( proxyBuilder.nameMappingClosure != null ){
            return proxyBuilder.nameMappingClosure.call( methodName );
        }
        return methodName;
    }

    protected FactoryBuilderSupport getProxyBuilder() {
        return proxyBuilder;
    }

    protected void handleNodeAttributes( Object node, Map attributes ) {
        // first, short circuit
        if( node == null ){
            return;
        }

        for( Iterator iter = proxyBuilder.attributeDelegates.iterator(); iter.hasNext(); ){
            ((Closure) iter.next()).call( new Object[] { this, node, attributes } );
        }

        if( proxyBuilder.getCurrentFactory().onHandleNodeAttributes( this, node, attributes ) ){
            proxyBuilder.setNodeAttributes( node, attributes );
        }
    }

    protected void newContext() {
        proxyBuilder.contexts.addFirst( new HashMap() );
    }

    /**
     * A hook to allow nodes to be processed once they have had all of their
     * children applied.
     *
     * @param node the current node being processed
     * @param parent the parent of the node being processed
     */
    protected void nodeCompleted( Object parent, Object node ) {
        proxyBuilder.getCurrentFactory().onNodeCompleted( this, parent, node );
    }

    protected Map popContext() {
        if( !proxyBuilder.contexts.isEmpty() ){
            return (Map) proxyBuilder.contexts.removeFirst();
        }
        return null;
    }

    /**
     * A hook after the factory creates the node and before attributes are set.<br>
     * It will call any registered postInstantiateDelegates, if you override
     * this method be sure to call this impl somewhere in your code.
     */
    protected void postInstantiate( Object name, Map attributes, Object node ) {
        for( Iterator iter = proxyBuilder.postInstantiateDelegates.iterator(); iter.hasNext(); ){
            ((Closure) iter.next()).call( new Object[] { this, node, attributes } );
        }
    }

    /**
     * A hook to allow nodes to be processed once they have had all of their
     * children applied and allows the actual node object that represents the
     * Markup element to be changed.<br>
     * It will call any registered postNodeCompletionDelegates, if you override
     * this method be sure to call this impl at the end of your code.
     *
     * @param node the current node being processed
     * @param parent the parent of the node being processed
     * @return the node, possibly new, that represents the markup element
     */
    protected Object postNodeCompletion( Object parent, Object node ) {
        for( Iterator iter = proxyBuilder.postNodeCompletionDelegates.iterator(); iter.hasNext(); ){
            ((Closure) iter.next()).call( new Object[] { this, parent, node } );
        }

        return node;
    }

    /**
     * A hook before the factory creates the node.<br>
     * It will call any registered preInstantiateDelegates, if you override this
     * method be sure to call this impl somewhere in your code.
     */
    protected void preInstantiate( Object name, Map attributes, Object value ) {
        for( Iterator iter = proxyBuilder.preInstantiateDelegates.iterator(); iter.hasNext(); ){
            ((Closure) iter.next()).call( new Object[] { this, value, attributes } );
        }
    }

    /**
     * Clears the context stack
     */
    protected void reset() {
        proxyBuilder.contexts.clear();
    }

    /**
     * A strategy method to allow derived builders to use builder-trees and
     * switch in different kinds of builders. This method should call the
     * setDelegate() method on the closure which by default passes in this but
     * if node is-a builder we could pass that in instead (or do something wacky
     * too)
     *
     * @param closure the closure on which to call setDelegate()
     * @param node the node value that we've just created, which could be a
     *        builder
     */
    protected void setClosureDelegate( Closure closure, Object node ) {
        closure.setDelegate( this );
    }

    /**
     * Maps attributes key/values to properties on node.
     */
    protected void setNodeAttributes( Object node, Map attributes ) {
        // set the properties
        for( Iterator iter = attributes.entrySet()
                .iterator(); iter.hasNext(); ){
            Map.Entry entry = (Map.Entry) iter.next();
            String property = entry.getKey().toString();
            Object value = entry.getValue();
            InvokerHelper.setProperty( node, property, value );
        }
    }

    protected void setParent( Object parent, Object child ) {
        proxyBuilder.getCurrentFactory().setParent( this, parent, child );
        Factory parentFactory = proxyBuilder.getParentFactory();
        if( parentFactory != null ){
            parentFactory.setChild( this, parent, child );
        }
    }

    protected void setProxyBuilder( FactoryBuilderSupport proxyBuilder ) {
        this.proxyBuilder = proxyBuilder;
    }

    protected LinkedList getContexts() {
        return proxyBuilder.contexts;
    }
}
