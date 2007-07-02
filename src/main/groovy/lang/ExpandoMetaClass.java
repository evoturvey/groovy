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
package groovy.lang;

import org.codehaus.groovy.runtime.*;
import org.codehaus.groovy.runtime.metaclass.ClosureMetaMethod;
import org.codehaus.groovy.runtime.metaclass.ThreadManagedMetaBeanProperty;
import org.codehaus.groovy.runtime.metaclass.ClosureStaticMetaMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A MetaClass that implements GroovyObject and behaves like an Expando, allowing the addition of new methods on the fly
 *
 * <code><pre>
 * // defines or replaces instance method:
 * metaClass.myMethod = { args -> }
 *
 * // defines a new instance method
 * metaClass.myMethod << { args -> }
 *
 * // creates multiple overloaded methods of the same name
 * metaClass.myMethod << { String s -> } << { Integer i -> }
 *
 * // defines or replaces a static method with the 'static' qualifier
 * metaClass.'static'.myMethod = { args ->  }
 *
 * // defines a new static method with the 'static' qualifier
 * metaClass.'static'.myMethod << { args ->  }
 *
 * // defines a new contructor
 * metaClass.constructor << { String arg -> }
 *
 * // defines or replaces a constructor
 * metaClass.constructor = { String arg -> }
 *
 * // defines a new property with an initial value of "blah"
 * metaClass.myProperty = "blah"
 *
 * </code></pre>
 *
 * By default methods are only allowed to be added before initialize() is called. In other words you create a new
 * ExpandoMetaClass, add some methods and then call initialize(). If you attempt to add new methods after initialize()
 * has been called an error will be thrown.
 *
 * This is to ensure that the MetaClass can operate appropriately in multi threaded environments as it forces you
 * to do all method additions at the beginning, before using the MetaClass.
 *
 * If you need more fine grained control of how a method is matched you can use DynamicMethodsMetaClass
 *
 * WARNING: This MetaClass uses a thread-bound ThreadLocal instance to store and retrieve properties.
 * In addition properties stored use soft references so they are both bound by the life of the Thread and by the soft
 * references. The implication here is you should NEVER use dynamic properties if you want their values to stick around
 * for long periods because as soon as the JVM is running low on memory or the thread dies they will be garbage collected.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class  ExpandoMetaClass extends MetaClassImpl implements GroovyObject {

	private static final String META_CLASS = "metaClass";
	private static final String CLASS = "class";
	private static final String META_METHODS = "metaMethods";
	private static final String METHODS = "methods";
	private static final String PROPERTIES = "properties";
	public static final String STATIC_QUALIFIER = "static";
	private static final Class[] ZERO_ARGUMENTS = new Class[0];
	private static final String CONSTRUCTOR = "constructor";
    private static final String GET_PROPERTY_METHOD = "getProperty";
    private static final String SET_PROPERTY_METHOD = "setProperty";

    private static final String INVOKE_METHOD_METHOD = "invokeMethod";
    private static final String CLASS_PROPERTY = "class";
    private static final String META_CLASS_PROPERTY = "metaClass";
    private static final String GROOVY_CONSTRUCTOR = "<init>";

    // These two properties are used when no ExpandoMetaClassCreationHandle is present
    private static final Map classInheritanceMapping = Collections.synchronizedMap(new HashMap());
    private boolean hasCreationHandle = false;
    private MetaClass myMetaClass;
    private boolean allowChangesAfterInit = false;

    private boolean initialized;
    private boolean initCalled = false;
    private boolean modified = false;
    private boolean inRegistry;
    private final Set inheritedMetaMethods = new HashSet();
    private final Map beanPropertyCache = new HashMap();
    private final Set expandoMethods = new HashSet();
    private final Map expandoProperties = new LinkedHashMap();
    private ClosureMetaMethod getPropertyMethod = null;
    private ClosureMetaMethod invokeMethodMethod = null;
    private ClosureMetaMethod setPropertyMethod = null;

    /**
     * For simulating closures in Java
     */
    private interface Callable {
		void call();
	}

    /**
     * Call to enable global use of global use of ExpandoMetaClass within the registry. This has the advantage that
     * inheritance will function correctly, but has a higher memory usage on the JVM than normal Groovy
     */
    public static void enableGlobally() {
        ExpandoMetaClassCreationHandle.enable();
    }

    /**
     * Call to disable the global use of ExpandoMetaClass
     */
    public static void disableGlobally() {
        GroovySystem.getMetaClassRegistry().setMetaClassCreationHandle( new MetaClassRegistry.MetaClassCreationHandle() );
    }


    /**
	 * @param allowChangesAfterInit the allowChangesAfterInit to set
	 */
	public void setAllowChangesAfterInit(boolean allowChangesAfterInit) {
		this.allowChangesAfterInit = allowChangesAfterInit;
	}


	/* (non-Javadoc)
	 * @see groovy.lang.MetaClassImpl#initialize()
	 */
	public synchronized void initialize() {
        if (!this.initialized) {
            inheritSelfTrackedExpandoMethods();
            super.initialize();
            this.initialized = true;
            this.initCalled = true;
        }
    }


	/* (non-Javadoc)
	 * @see groovy.lang.MetaClassImpl#isInitialized()
	 */
	protected boolean isInitialized() {
		return this.initialized;
	}

	/**
	 * If their is no ExpandoMetaClassCreationHandle the EMC will attempt to track inheritance
	 * methods itself. In this case inherited methods will only work if both the parent and the child
	 * have an EMC. The best way to get method inheritance working properly is to register a
	 * ExpandoMetaClassCreationHandle, which may have a performance drawback (Disclaimer: not measured)
	 */
	private void inheritSelfTrackedExpandoMethods() {
        if(!(GroovySystem.getMetaClassRegistry().getMetaClassCreationHandler() instanceof ExpandoMetaClassCreationHandle)) {
            List superClasses = getSuperClasses();
            for (Iterator i = superClasses.iterator(); i.hasNext();) {
                Class c = (Class) i.next();
                Map methodMap = (Map)classInheritanceMapping.get(c);
                if(methodMap!=null) {
                    for (Iterator j = methodMap.values().iterator(); j.hasNext();) {
                        List methods = (List) j.next();
                        for (Iterator k = methods.iterator(); k.hasNext();) {
                            MetaMethod metaMethodFromSuper = (MetaMethod) k.next();
                            if(!metaMethodFromSuper.isStatic()) {
                                addSuperMethodIfNotOverriden(metaMethodFromSuper);
                            }
                        }
                    }
                }
            }
        }
	}



	private void addSuperMethodIfNotOverriden(final MetaMethod metaMethodFromSuper) {
		performOperationOnMetaClass(new Callable() {
			public void call() {

				MetaMethod existing = pickMethod(metaMethodFromSuper.getName(), metaMethodFromSuper.getParameterTypes());

				if(existing == null) {
                        addMethodWithKey(metaMethodFromSuper);
				}
				else {
                    boolean isGroovyMethod = getMetaMethods().contains(existing);
                    if(isGroovyMethod) {
                        addMethodWithKey(metaMethodFromSuper);
                    }
                    else if(inheritedMetaMethods.contains(existing)) {
                        inheritedMetaMethods.remove(existing);

                        addMethodWithKey(metaMethodFromSuper);
                    }
				}

			}

			private void addMethodWithKey(final MetaMethod metaMethodFromSuper) {
                inheritedMetaMethods.add(metaMethodFromSuper);
                if(metaMethodFromSuper instanceof ClosureMetaMethod) {
                    ClosureMetaMethod closureMethod = (ClosureMetaMethod)metaMethodFromSuper;
                    Closure cloned = (Closure)closureMethod.getClosure().clone();
                    String name = metaMethodFromSuper.getName();
                    ClosureMetaMethod localMethod = new ClosureMetaMethod(name, getJavaClass(), cloned);
                    addMetaMethod(localMethod);
                    MethodKey key = new DefaultMethodKey(getJavaClass(),name, localMethod.getParameterTypes(),false );
                    cacheInstanceMethod(key, localMethod);

                    checkIfGroovyObjectMethod(localMethod, name, cloned);
                    expandoMethods.add(localMethod);

                }
            }
		});
	}

    /**
	 * Constructs a new ExpandoMetaClass instance for the given class
	 *
	 * @param theClass The class that the MetaClass applies to
	 */
	public ExpandoMetaClass(Class theClass) {
		super(InvokerHelper.getInstance().getMetaRegistry(), theClass);
		this.myMetaClass = InvokerHelper.getMetaClass(this);

	}

	/**
	 * Constructs a new ExpandoMetaClass instance for the given class optionally placing the MetaClass
	 * in the MetaClassRegistry automatically
	 *
	 * @param theClass The class that the MetaClass applies to
	 * @param register True if the MetaClass should be registered inside the MetaClassRegistry
	 */
	public ExpandoMetaClass(Class theClass, boolean register) {
		this(theClass);

		if(register) {
			super.registry.setMetaClass(theClass, this);
			this.inRegistry = true;
		}
	}

	/**
	 * Instances of this class are returned when using the << left shift operator.
	 *
	 * Example:
	 *
	 * metaClass.myMethod << { String args -> }
	 *
	 * This allows callbacks to the ExpandoMetaClass for registering appending methods
	 *
	 * @author Graeme Rocher
	 *
	 */
	protected class ExpandoMetaProperty extends GroovyObjectSupport {

		String propertyName;
		boolean isStatic = false;
		protected ExpandoMetaProperty(String name) {
			this(name, false);
		}
		protected ExpandoMetaProperty(String name, boolean isStatic) {
			this.propertyName = name;
			this.isStatic = isStatic;
		}

		public Object leftShift(Object arg) {
			registerIfClosure(arg, false);
			return this;
		}
		private void registerIfClosure(Object arg, boolean replace) {
			if(arg instanceof Closure) {
				Closure callable = (Closure)arg;
				Class[] paramTypes = callable.getParameterTypes();
				if(paramTypes == null)paramTypes = ZERO_ARGUMENTS;
				if(!this.isStatic) {
					Method foundMethod = checkIfMethodExists(theClass, propertyName, paramTypes, false);

					if(foundMethod != null && !replace) throw new GroovyRuntimeException("Cannot add new method ["+propertyName+"] for arguments ["+DefaultGroovyMethods.inspect(paramTypes)+"]. It already exists!");

					registerInstanceMethod(propertyName, callable);
				}
				else {
					Method foundMethod = checkIfMethodExists(theClass, propertyName, paramTypes, true);
					if(foundMethod != null && !replace) throw new GroovyRuntimeException("Cannot add new static method ["+propertyName+"] for arguments ["+DefaultGroovyMethods.inspect(paramTypes)+"]. It already exists!");

					registerStaticMethod(propertyName, callable);
				}
			}
		}
		private Method checkIfMethodExists(Class methodClass, String methodName, Class[] paramTypes, boolean staticMethod) {
			Method foundMethod = null;
			Method[] methods = methodClass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				if(methods[i].getName().equals(methodName) && Modifier.isStatic(methods[i].getModifiers()) == staticMethod) {
					if(MetaClassHelper.parametersAreCompatible( paramTypes, methods[i].getParameterTypes() )) {
						foundMethod = methods[i];
						break;
					}
				}
			}
			return foundMethod;
		}
		/* (non-Javadoc)
		 * @see groovy.lang.GroovyObjectSupport#getProperty(java.lang.String)
		 */
		public Object getProperty(String property) {
			this.propertyName = property;
			return this;
		}
		/* (non-Javadoc)
		 * @see groovy.lang.GroovyObjectSupport#setProperty(java.lang.String, java.lang.Object)
		 */
		public void setProperty(String property, Object newValue) {
			this.propertyName = property;
			registerIfClosure(newValue, true);
		}


	}


	/* (non-Javadoc)
	 * @see groovy.lang.MetaClassImpl#invokeConstructor(java.lang.Object[])
	 */
	public Object invokeConstructor(Object[] arguments) {

		// TODO This is the only area where this MetaClass needs to do some interception because Groovy's current
		// MetaClass uses hard coded references to the java.lang.reflect.Constructor class so you can't simply
		// inject Constructor like you can do properties, methods and fields. When Groovy's MetaClassImpl is
		// refactored we can fix this
		Class[] argClasses = MetaClassHelper.convertToTypeArray(arguments);
		MetaMethod method = pickMethod(GROOVY_CONSTRUCTOR, argClasses);
		if(method!=null && method.getParameterTypes().length == arguments.length) {
			return method.invoke(theClass, arguments);
		}
		return super.invokeConstructor(arguments);
	}


	/**
	 * Retrieves a list of super classes. Taken from MetaClassImpl. Ideally this method should be protected
	 *
	 * @return A list of super classes
	 */
   protected LinkedList getSuperClasses() {
       LinkedList superClasses = new LinkedList();
       for (Class c = theClass; c!= null; c = c.getSuperclass()) {
           superClasses.addFirst(c);
       }
       if (getJavaClass().isArray() && getJavaClass()!=Object[].class && !getJavaClass().getComponentType().isPrimitive()) {
           superClasses.addFirst(Object[].class);
       }
       return superClasses;
   }
	/**
	 * Handles the ability to use the left shift operator to append new constructors
	 *
	 * @author Graeme Rocher
	 *
	 */
	protected class ExpandoMetaConstructor extends GroovyObjectSupport {
		public Object leftShift(Closure c) {
			if(c != null) {
				Class[] paramTypes = c.getParameterTypes();
				if(paramTypes == null)paramTypes = ZERO_ARGUMENTS;

				Constructor ctor = retrieveConstructor(paramTypes);
				if(ctor != null) throw new GroovyRuntimeException("Cannot add new constructor for arguments ["+DefaultGroovyMethods.inspect(paramTypes)+"]. It already exists!");

				registerInstanceMethod(GROOVY_CONSTRUCTOR, c);
			}

			return this;
		}
	}

	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#getMetaClass()
	 */
	public MetaClass getMetaClass() {
		return myMetaClass;
	}



	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#getProperty(java.lang.String)
	 */
	public Object getProperty(String property) {
		if(isValidExpandoProperty(property)) {
			if(property.equals(STATIC_QUALIFIER)) {
				return new ExpandoMetaProperty(property, true);
			}
			else if(property.equals(CONSTRUCTOR)) {
				return new ExpandoMetaConstructor();
			}
			else {
				return new ExpandoMetaProperty(property);
			}
		}
		else {
			return myMetaClass.getProperty(this, property);
		}
	}

	private boolean isValidExpandoProperty(String property) {
        return !property.equals(META_CLASS) &&
                !property.equals(CLASS) &&
                !property.equals(META_METHODS) &&
                !property.equals(METHODS) &&
                !property.equals(PROPERTIES);
    }

	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#invokeMethod(java.lang.String, java.lang.Object)
	 */
	public Object invokeMethod(String name, Object args) {
		return myMetaClass.invokeMethod(this, name, args);
	}

	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#setMetaClass(groovy.lang.MetaClass)
	 */
	public void setMetaClass(MetaClass metaClass) {
		this.myMetaClass = metaClass;
	}

	/* (non-Javadoc)
	 * @see groovy.lang.GroovyObject#setProperty(java.lang.String, java.lang.Object)
	 */
	public void setProperty(String property, Object newValue) {
		if(newValue instanceof Closure) {
			if(property.equals(CONSTRUCTOR)) {
				property = GROOVY_CONSTRUCTOR;
			}
			Closure callable = (Closure)newValue;
			// here we don't care if the method exists or not we assume the
			// developer is responsible and wants to override methods where necessary
			registerInstanceMethod(property, callable);

		}
		else if(property.equals("allowChangesAfterInit")) {
			this.allowChangesAfterInit = ((Boolean)newValue).booleanValue();
		}
		else {
			registerBeanProperty(property, newValue);
		}
	}


	protected void performOperationOnMetaClass(Callable c) {
        synchronized(this) {
            try {
                if(allowChangesAfterInit) {
                    this.initialized = false;
                }

                c.call();
            }
            finally {
                if(initCalled)this.initialized = true;
            }
        }
	}

	/**
	 * Registers a new bean property
	 *
	 * @param property The property name
	 * @param newValue The properties initial value
	 */
	protected void registerBeanProperty(final String property, final Object newValue) {
			performOperationOnMetaClass(new Callable() {
				public void call() {
					Class type = newValue == null ? Object.class : newValue.getClass();

					MetaBeanProperty mbp = new ThreadManagedMetaBeanProperty(theClass,property,type,newValue);

					addMetaMethod(mbp.getGetter());
					addMetaMethod(mbp.getSetter());
                    expandoMethods.add(mbp.getSetter());
                    expandoMethods.add(mbp.getGetter());
                    expandoProperties.put(mbp.getName(),mbp);

					addMetaBeanProperty(mbp);
				}

			});
	}

	/**
	 * Registers a new instance method for the given method name and closure on this MetaClass
	 *
	 * @param methodName The method name
	 * @param callable The callable Closure
	 */
	protected void registerInstanceMethod(final String methodName, final Closure callable) {
			final boolean inited = this.initCalled;
			performOperationOnMetaClass(new Callable() {
				public void call() {
					ClosureMetaMethod metaMethod = new ClosureMetaMethod(methodName, theClass,callable);
                    checkIfGroovyObjectMethod(metaMethod, methodName, callable);
                    MethodKey key = new DefaultMethodKey(theClass,methodName, metaMethod.getParameterTypes(),false );


					addMetaMethod(metaMethod);
                    synchronized(expandoMethods) {
                        expandoMethods.add(metaMethod);
                    }
					cacheInstanceMethod(key, metaMethod);
					if(inited && isGetter(methodName, metaMethod.getParameterTypes())) {
						String propertyName = getPropertyForGetter(methodName);
						registerBeanPropertyForMethod(metaMethod, propertyName, true);

					}
					else if(inited && isSetter(methodName, metaMethod.getParameterTypes())) {
						String propertyName = getPropertyForSetter(methodName);
						registerBeanPropertyForMethod(metaMethod, propertyName, false);
					}
					performRegistryCallbacks();
					if(!hasCreationHandle) {
						registerWithInheritenceManager(theClass, metaMethod);
					}
				}

			});
	}

    /**
     * Checks if the metaMethod is a method from the GroovyObject interface such as setProperty, getProperty and invokeMethod
     *
     * @param metaMethod The metaMethod instance
     * @param methodName The method name
     * @param callable The closure from the meta method
     *
     * @see groovy.lang.GroovyObject
     */
    private void checkIfGroovyObjectMethod(ClosureMetaMethod metaMethod, String methodName, Closure callable) {
        if(isGetPropertyMethod(methodName)) {
            getPropertyMethod = metaMethod;
        }
        else if(isInvokeMethod(methodName, callable)) {
            invokeMethodMethod = metaMethod;
        }
        else if(isSetPropertyMethod(methodName, metaMethod)) {
            setPropertyMethod = metaMethod;
        }
    }

    private boolean isSetPropertyMethod(String methodName, ClosureMetaMethod metaMethod) {
        return SET_PROPERTY_METHOD.equals(methodName)  && metaMethod.getParameterTypes().length == 2;
    }

    private boolean isGetPropertyMethod(String methodName) {
        return GET_PROPERTY_METHOD.equals(methodName);
    }

    private boolean isInvokeMethod(String methodName, Closure metaMethod) {
        return INVOKE_METHOD_METHOD.equals(methodName) && metaMethod.getParameterTypes().length == 2;
    }


    private void performRegistryCallbacks() {
		MetaClassRegistry registry =  InvokerHelper.getInstance().getMetaRegistry();
		if(!modified && !inRegistry) {
			modified = true;
			// Implementation note: By default Groovy uses soft references to store MetaClass
			// this insures the registry doesn't grow and get out of hand. By doing this we're
			// saying this this EMC will be a hard reference in the registry. As we're only
			// going have a small number of classes that have modified EMC this is ok
            MetaClass currMetaClass = registry.getMetaClass(theClass);
            if(!(currMetaClass instanceof ExpandoMetaClass) && currMetaClass instanceof AdaptingMetaClass) {
                ((AdaptingMetaClass)currMetaClass).setAdaptee(this);
            } else {
                registry.setMetaClass(theClass, this);
            }

			this.inRegistry = true;
		}
		// Implementation note: EMC handles most cases by itself except for the case where yuou
		// want to call a dynamically injected method registered with a parent on a child class
		// For this to work the MetaClassRegistry needs to have an ExpandoMetaClassCreationHandle
		// What this does is ensure that EVERY class created in the registry uses an EMC
		// Then when an EMC changes it reports back to the EMCCreationHandle which will
		// tell child classes of this class to re-inherit their methods
		if(registry.getMetaClassCreationHandler() instanceof ExpandoMetaClassCreationHandle) {
			ExpandoMetaClassCreationHandle creationHandler = (ExpandoMetaClassCreationHandle)registry.getMetaClassCreationHandler();
			hasCreationHandle  = true;
			if(!creationHandler.hasModifiedMetaClass(this))
				creationHandler.registerModifiedMetaClass(this);
			creationHandler.notifyOfMetaClassChange(this);
		}
	}


	private static void registerWithInheritenceManager(Class theClass, ClosureMetaMethod metaMethod) {
		Map methodMap = (Map)classInheritanceMapping .get(theClass);
		if(methodMap == null) {
			methodMap = new HashMap();
			classInheritanceMapping.put(theClass, methodMap);
		}
		List methodList = (List)methodMap.get(metaMethod.getName());
		if(methodList == null) {
			methodList = new LinkedList();
			methodMap.put(metaMethod.getName(), methodList);
		}
		methodList.add(metaMethod);
	}


	private void registerBeanPropertyForMethod(ClosureMetaMethod metaMethod, String propertyName, boolean getter) {
        synchronized(beanPropertyCache) {
            MetaBeanProperty beanProperty = (MetaBeanProperty)beanPropertyCache.get(propertyName);
            if(beanProperty == null) {
                if(getter)
                    beanProperty = new MetaBeanProperty(propertyName,Object.class,metaMethod,null);
                else
                    beanProperty = new MetaBeanProperty(propertyName,Object.class,null,metaMethod);

                beanPropertyCache.put(propertyName, beanProperty);
                synchronized(expandoProperties) {
                    expandoProperties.put(beanProperty.getName(),beanProperty);
                }
            }
            else {
                if(getter) {
                    MetaMethod setterMethod = beanProperty.getSetter();
                    Class type = setterMethod != null ? setterMethod.getParameterTypes()[0] : Object.class;
                    beanProperty = new MetaBeanProperty(propertyName,type,metaMethod,setterMethod);
                    beanPropertyCache.put(propertyName, beanProperty);
                }else {
                    MetaMethod getterMethod = beanProperty.getGetter();
                    beanProperty = new MetaBeanProperty(propertyName,metaMethod.getParameterTypes()[0],getterMethod,metaMethod);
                    beanPropertyCache .put(propertyName, beanProperty);
                }
            }
		    addMetaBeanProperty(beanProperty);
        }
	}

	/**
	 * Registers a new static method for the given method name and closure on this MetaClass
	 *
	 * @param methodName The method name
	 * @param callable The callable Closure
	 */
	protected void registerStaticMethod(final String methodName, final Closure callable) {
		performOperationOnMetaClass(new Callable() {
			public void call() {
				ClosureStaticMetaMethod metaMethod = new ClosureStaticMetaMethod(methodName, theClass,callable);
				MethodKey key = new DefaultMethodKey(theClass,methodName, metaMethod.getParameterTypes(), false );

				addMetaMethod(metaMethod);
                synchronized(expandoMethods) {
                    expandoMethods.add(metaMethod);
                }
				cacheStaticMethod(key, metaMethod);
			}

		});
	}

	/**
	 * @return The Java class enhanced by this MetaClass
	 */
	public Class getJavaClass() {
		return theClass;
	}

	/**
	 * Called from ExpandoMetaClassCreationHandle in the registry if it exists to setup inheritance
	 * handling
	 *
	 * @param modifiedSuperExpandos A list of modified super ExpandoMetaClass
	 */
	public void refreshInheritedMethods(Set modifiedSuperExpandos) {
		for (Iterator i = modifiedSuperExpandos.iterator(); i.hasNext();) {

			ExpandoMetaClass superExpando = (ExpandoMetaClass) i.next();
            if(superExpando != this) {
                List metaMethods = superExpando.getExpandoMethods();
                for (Iterator j = metaMethods.iterator(); j.hasNext();) {
                    MetaMethod metaMethod = (MetaMethod) j.next();
                    addSuperMethodIfNotOverriden(metaMethod);
                }
                Collection metaProperties = superExpando.getExpandoProperties();
                for (Iterator j = metaProperties.iterator(); j.hasNext();) {
                    MetaBeanProperty property = (MetaBeanProperty) j.next();
                    synchronized(expandoProperties) {
                        expandoProperties.put(property.getName(),property);
                    }
                    addMetaBeanProperty(property);
                }
            }

        }
	}


	/**
	 * Returns a list of expando MetaMethod instances added to this ExpandoMetaClass
	 *
	 * @return the expandoMethods
	 */
	public List getExpandoMethods() {
        synchronized (expandoMethods) {
            return Collections.unmodifiableList(DefaultGroovyMethods.toList(expandoMethods));
        }
    }


	/**
	 * Returns a list of MetaBeanProperty instances added to this ExpandoMetaClass
	 *
	 * @return the expandoProperties
	 */
	public Collection getExpandoProperties() {
        synchronized (expandoProperties) {
            return Collections.unmodifiableCollection(expandoProperties.values());
        }
    }

    /**
     * Overrides default implementation just in case invokeMethod has been overriden by ExpandoMetaClass
     *
     * @see groovy.lang.MetaClassImpl#invokeMethod(Class, Object, String, Object[], boolean, boolean)
     */
    public Object invokeMethod(Class sender, Object object, String methodName, Object[] originalArguments, boolean isCallToSuper, boolean fromInsideClass) {
        if(invokeMethodMethod!=null) {
            return invokeMethodMethod.invoke(object, new Object[]{methodName, originalArguments});
        }
        return super.invokeMethod(sender, object, methodName, originalArguments, isCallToSuper, fromInsideClass);
    }

    /**
     * Overrides default implementation just in case getProperty method has been overriden by ExpandoMetaClass
     *
     * @see MetaClassImpl#getProperty(Class, Object, String, boolean, boolean)
     */
    public Object getProperty(Class sender, Object object, String name, boolean useSuper, boolean fromInsideClass) {
        if(hasOverrideGetProperty(name)) {
            return getPropertyMethod.invoke(object, new Object[]{name});
        }
        return super.getProperty(sender, object, name, useSuper, fromInsideClass);
    }

    private boolean hasOverrideGetProperty(String name) {
        return getPropertyMethod != null && !name.equals(META_CLASS_PROPERTY)&& !name.equals(CLASS_PROPERTY);
    }

    /**
     * Overrides default implementation just in case setProperty method has been overriden by ExpandoMetaClass
     *
     * @see MetaClassImpl#setProperty(Class, Object, String, Object, boolean, boolean)
     */

    public void setProperty(Class sender, Object object, String name, Object newValue, boolean useSuper, boolean fromInsideClass) {
        if(setPropertyMethod!=null  && !name.equals(META_CLASS_PROPERTY)) {
            setPropertyMethod.invoke(object, new Object[]{name, newValue});
            return;
        }
        super.setProperty(sender, object, name, newValue, useSuper, fromInsideClass);
    }

    /**
     * Looks up an existing MetaProperty by name
     *
     * @param name The name of the MetaProperty
     * @return The MetaProperty or null if it doesn't exist
     */
    public MetaProperty getMetaProperty(String name) {
        MetaProperty mp;
        synchronized (expandoProperties) {
            mp = (MetaProperty) this.expandoProperties.get(name);
        }
        if (mp != null) return mp;
        List properties = super.getProperties();
        for (Iterator i = properties.iterator(); i.hasNext();) {
            MetaProperty metaProperty = (MetaProperty) i.next();
            if (name.equals(metaProperty.getName())) return metaProperty;
        }
        return null;
    }

    /**
     * Returns true if the MetaClass has the given property
     *
     * @param name The name of the MetaProperty
     * @return True it exists as a MetaProperty
     */
    public boolean hasMetaProperty(String name) {
        return getMetaProperty(name) != null;
    }

    /**
     * Retrieves a MetaMethod for the given name and arguments
     *
     * @param name The name of the MetaMethod
     * @param args The arguments to the meta method
     * @return The MetaMethod or null if it doesn't exist
     */
    public MetaMethod getMetaMethod(String name, Class[] args) {
        return super.pickMethod(name, args);
    }

    /**
     * Retrieves a MetaMethod given a name and the arguments which may be an object,
     * or an array of objects
     *
     * @param name The name of the method
     * @param args The arguments to the method
     * @return A MetaMethod or null
     */
    public MetaMethod getMetaMethod(String name, Object args) {
        if(args instanceof Object[]) {
            Object[] allArgs = (Object[]) args;
            Class[] types = new Class[allArgs.length];
            for (int i = 0; i < types.length; i++) {
                if(allArgs[i] != null) {
                    types[i] = allArgs[i].getClass();
                }
                else {
                    types[i] = null;
                }

            }
            return this.getMetaMethod(name, types);
        }
        else {
            return this.getMetaMethod(name, new Class[]{args.getClass()});
        }
    }

    /**
     * Checks whether a MetaMethod for the given name and arguments exists
     *
     * @param name The name of the MetaMethod
     * @param args The arguments to the meta method
     * @return True if the method exists otherwise null
     */
    public boolean hasMetaMethod(String name, Class[] args) {
        return super.pickMethod(name, args) != null;
    }


    /**
     * Returns true if the name of the method specified and the number of arguments make it a javabean property
     *
     * @param name True if its a Javabean property
     * @param args The arguments
     * @return True if it is a javabean property method
     */
    private boolean isGetter(String name, Class[] args) {
        if(name == null || name.length() == 0 || args == null)return false;
        if(args.length != 0)return false;

        if(name.startsWith("get")) {
            name = name.substring(3);
            if(name.length() > 0 && Character.isUpperCase(name.charAt(0))) return true;
        }
        else if(name.startsWith("is")) {
            name = name.substring(2);
            if(name.length() > 0 && Character.isUpperCase(name.charAt(0))) return true;
        }
        return false;
    }

    /**
     * Returns a property name equivalent for the given getter name or null if it is not a getter
     *
     * @param getterName The getter name
     * @return The property name equivalent
     */
    private String getPropertyForGetter(String getterName) {
        if(getterName == null || getterName.length() == 0)return null;

        if(getterName.startsWith("get")) {
            String prop = getterName.substring(3);
            return convertPropertyName(prop);
        }
        else if(getterName.startsWith("is")) {
            String prop = getterName.substring(2);
            return convertPropertyName(prop);
        }
        return null;
    }

	private String convertPropertyName(String prop) {
		if(Character.isUpperCase(prop.charAt(0)) && Character.isUpperCase(prop.charAt(1))) {
			return prop;
		}
		else if(Character.isDigit(prop.charAt(0))) {
			return prop;
		}
		else {
			return Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
		}
	}

    /**
     * Returns a property name equivalent for the given setter name or null if it is not a getter
     *
     * @param setterName The setter name
     * @return The property name equivalent
     */
    public String getPropertyForSetter(String setterName) {
        if(setterName == null || setterName.length() == 0)return null;

        if(setterName.startsWith("set")) {
            String prop = setterName.substring(3);
            return convertPropertyName(prop);
        }
        return null;
    }

    public boolean isSetter(String name, Class[] args) {
        if(name == null || name.length() == 0 || args == null)return false;

        if(name.startsWith("set")) {
            if(args.length != 1) return false;
            name = name.substring(3);
            if(name.length() > 0 && Character.isUpperCase(name.charAt(0))) return true;
        }

        return false;
    }


}

