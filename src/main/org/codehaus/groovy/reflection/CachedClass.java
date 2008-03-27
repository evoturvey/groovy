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
package org.codehaus.groovy.reflection;

import groovy.lang.MetaMethod;
import org.codehaus.groovy.classgen.BytecodeHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * @author Alex.Tkachman
 */
public class CachedClass {
    private final Class cachedClass;
    public final ClassInfo classInfo;

    private final LazyReference<CachedField[]> fields = new LazyReference<CachedField[]>() {
        public CachedField[] initValue() {
            final Field[] declaredFields = (Field[])
               AccessController.doPrivileged(new PrivilegedAction/*<Field[]>*/() {
                   public /*Field[]*/ Object run() {
                       return getTheClass().getDeclaredFields();
                   }
               });
            CachedField [] fields = new CachedField[declaredFields.length];
            for (int i = 0; i != fields.length; ++i)
                fields[i] = new CachedField(CachedClass.this, declaredFields[i]);
            return fields;
        }
    };

    private LazyReference<CachedConstructor[]> constructors = new LazyReference<CachedConstructor[]> () {
        public CachedConstructor[] initValue() {
            final Constructor[] declaredConstructors = (Constructor[])
               AccessController.doPrivileged(new PrivilegedAction/*<Constructor[]>*/() {
                   public /*Constructor[]*/ Object run() {
                       return getTheClass().getDeclaredConstructors();
                   }
               });
            CachedConstructor [] constructors = new CachedConstructor[declaredConstructors.length];
            for (int i = 0; i != constructors.length; ++i)
                constructors[i] = new CachedConstructor(CachedClass.this, declaredConstructors[i]);
            return constructors;
        }
    };

    private LazyReference<CachedMethod[]> methods = new LazyReference<CachedMethod[]> () {
        public CachedMethod[] initValue() {
            final Method[] declaredMethods = (Method[])
               AccessController.doPrivileged(new PrivilegedAction/*<Method[]>*/() {
                   public /*Method[]*/ Object run() {
                       return getTheClass().getDeclaredMethods();
                   }
               });
            ArrayList methods = new ArrayList(declaredMethods.length);
            ArrayList mopMethods = new ArrayList(declaredMethods.length);
            for (int i = 0; i != declaredMethods.length; ++i) {
                final CachedMethod cachedMethod = new CachedMethod(CachedClass.this, declaredMethods[i]);
                final String name = cachedMethod.getName();

                if (name.indexOf('+') >= 0) {
                    // Skip Synthetic methods inserted by JDK 1.5 compilers and later
                    continue;
                } /*else if (Modifier.isAbstract(reflectionMethod.getModifiers())) {
                   continue;
                }*/

                if (name.startsWith("this$") || name.startsWith("super$"))
                  mopMethods.add(cachedMethod);
                else
                  methods.add(cachedMethod);
            }
            CachedMethod [] resMethods = (CachedMethod[]) methods.toArray(new CachedMethod[methods.size()]);
            Arrays.sort(resMethods);

            final CachedClass superClass = getCachedSuperClass();
            if (superClass != null) {
                superClass.getMethods();
                final CachedMethod[] superMopMethods = superClass.mopMethods;
                for (int i = 0; i != superMopMethods.length; ++i)
                  mopMethods.add(superMopMethods[i]);
            }
            CachedClass.this.mopMethods = (CachedMethod[]) mopMethods.toArray(new CachedMethod[mopMethods.size()]);
            Arrays.sort(CachedClass.this.mopMethods, CachedMethodComparatorByName.INSTANCE);

            return resMethods;
        }
    };

    private MetaMethod[] newMetaMethods = EMPTY;

    private LazyReference<CachedClass> cachedSuperClass = new LazyReference<CachedClass> () {
        public CachedClass initValue() {
            if (!isArray)
              return ReflectionCache.getCachedClass(getTheClass().getSuperclass());
            else
              if (cachedClass.getComponentType().isPrimitive() || cachedClass.getComponentType() == Object.class)
                return ReflectionCache.OBJECT_CLASS;
              else
                return ReflectionCache.OBJECT_ARRAY_CLASS;
        }
    };

    private static final MetaMethod[] EMPTY = new MetaMethod[0];

    int hashCode;

    public  CachedMethod [] mopMethods;
    public static final CachedClass[] EMPTY_ARRAY = new CachedClass[0];

    private Set<CachedClass> ownInterfaces;

    private Set<CachedClass> interfaces;

    public final boolean isArray;
    public final boolean isPrimitive;
    public final int modifiers;
    int distance = -1;
    public final boolean isInterface;
    public final boolean isNumber;

    public CachedClass(Class klazz, ClassInfo classInfo) {
        cachedClass = klazz;
        this.classInfo = classInfo;
        isArray = klazz.isArray();
        isPrimitive = klazz.isPrimitive();
        modifiers = klazz.getModifiers();
        isInterface = klazz.isInterface();
        isNumber = Number.class.isAssignableFrom(klazz);

        for (Iterator it = getInterfaces().iterator(); it.hasNext(); ) {
            CachedClass inf = (CachedClass) it.next();
            ReflectionCache.isAssignableFrom(klazz, inf.cachedClass);
        }

        for (CachedClass cur = this; cur != null; cur = cur.getCachedSuperClass()) {
            ReflectionCache.setAssignableFrom(cur.cachedClass, klazz);
        }
    }

    public CachedClass getCachedSuperClass() {
        return cachedSuperClass.get();
    }

    public Set<CachedClass> getInterfaces() {
        if (interfaces == null)  {
            interfaces = new HashSet<CachedClass> (0);

            if (getTheClass().isInterface())
              interfaces.add(this);

            Class[] classes = getTheClass().getInterfaces();
            for (int i = 0; i < classes.length; i++) {
                final CachedClass aClass = ReflectionCache.getCachedClass(classes[i]);
                if (!interfaces.contains(aClass))
                  interfaces.addAll(aClass.getInterfaces());
            }

            final CachedClass superClass = getCachedSuperClass();
            if (superClass != null)
              interfaces.addAll(superClass.getInterfaces());
        }
        return interfaces;
    }

    public Set<CachedClass> getDeclaredInterfaces() {
        if (ownInterfaces == null)  {
            ownInterfaces = new HashSet<CachedClass> (0);

            Class[] classes = getTheClass().getInterfaces();
            for (int i = 0; i < classes.length; i++) {
                ownInterfaces.add(ReflectionCache.getCachedClass(classes[i]));
            }
        }
        return ownInterfaces;
    }

    public CachedMethod[] getMethods() {
        return methods.get();
    }

    public CachedField[] getFields() {
        return fields.get();
    }

    public CachedConstructor[] getConstructors() {
        return constructors.get();
    }

    public CachedMethod searchMethods(String name, CachedClass[] parameterTypes) {
        CachedMethod[] methods = getMethods();

        CachedMethod res = null;
        for (int i = 0; i < methods.length; i++) {
            CachedMethod m = methods[i];
            if (m.getName().equals(name)
                    && ReflectionCache.arrayContentsEq(parameterTypes, m.getParameterTypes())
                    && (res == null || res.getReturnType().isAssignableFrom(m.getReturnType())))
                res = m;
        }

        return res;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Object coerceArgument(Object argument) {
        return argument;
    }
    
    public int getSuperClassDistance() {
        synchronized (getTheClass()) {
            if (distance == -1) {
                int distance = 0;
                for (Class klazz= getTheClass(); klazz != null; klazz = klazz.getSuperclass()) {
                    distance++;
                }
                this.distance = distance;
            }
            return distance;
        }
    }

    public int hashCode() {
        if (hashCode == 0) {
          hashCode = super.hashCode();
          if (hashCode == 0)
            hashCode = 0xcafebebe;
        }
        return hashCode;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public boolean isVoid() {
        return getTheClass() == void.class;
    }

    public void box(BytecodeHelper helper) {
        helper.box(getTheClass());
    }

    public void unbox(BytecodeHelper helper) {
        helper.unbox(getTheClass());
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void doCast(BytecodeHelper helper) {
        helper.doCast(getTheClass());
    }

    public String getName() {
        return getTheClass().getName();
    }

    public String getTypeDescription() {
        return BytecodeHelper.getTypeDescription(getTheClass());
    }

    public final Class getTheClass() {
        return cachedClass;
    }

    public MetaMethod[] getNewMetaMethods() {
        return newMetaMethods;
    }

    public void setNewMopMethods(ArrayList arr) {
        newMetaMethods = (MetaMethod[]) arr.toArray(new MetaMethod[arr.size()]);
    }

    public boolean isAssignableFrom(Class argument) {
        return argument == null || ReflectionCache.isAssignableFrom(getTheClass(), argument);
    }

    public boolean isDirectlyAssignable(Object argument) {
        return ReflectionCache.isAssignableFrom(getTheClass(), argument.getClass());
    }

    public static class CachedMethodComparatorByName implements Comparator {
        public static final Comparator INSTANCE = new CachedMethodComparatorByName();

        public int compare(Object o1, Object o2) {
              return ((CachedMethod)o1).getName().compareTo(((CachedMethod)o2).getName());
        }
    }

    public static class CachedMethodComparatorWithString implements Comparator {
        public static final Comparator INSTANCE = new CachedMethodComparatorWithString();

        public int compare(Object o1, Object o2) {
            if (o1 instanceof CachedMethod)
              return ((CachedMethod)o1).getName().compareTo((String)o2);
            else
              return ((String)o1).compareTo(((CachedMethod)o2).getName());
        }
    }

    public String toString() {
        return cachedClass.toString();
    }
}
