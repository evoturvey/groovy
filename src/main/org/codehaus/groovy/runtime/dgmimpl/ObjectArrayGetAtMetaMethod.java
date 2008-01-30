package org.codehaus.groovy.runtime.dgmimpl;

import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.callsite.CallSiteAwareMetaMethod;
import org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite;

import java.lang.reflect.Modifier;

/**
 * Support the subscript operator for an Array.
 *
 */
public class ObjectArrayGetAtMetaMethod extends CallSiteAwareMetaMethod {
    private static final CachedClass INTEGER_CLASS = ReflectionCache.getCachedClass(Integer.class);
    private static final CachedClass OBJECT_ARR_CLASS = ReflectionCache.OBJECT_ARRAY_CLASS;
    private static final CachedClass [] INTEGER_CLASS_ARR = new CachedClass[] {INTEGER_CLASS};

    public ObjectArrayGetAtMetaMethod() {
        parameterTypes = INTEGER_CLASS_ARR;
    }

    public int getModifiers() {
        return Modifier.PUBLIC;
    }

    public String getName() {
        return "getAt";
    }

    public Class getReturnType() {
        return Object.class;
    }

    public final CachedClass getDeclaringClass() {
        return OBJECT_ARR_CLASS;
    }

    public Object invoke(Object object, Object[] arguments) {
        final Object[] objects = (Object[]) object;
        return objects[normaliseIndex(((Integer) arguments[0]).intValue(), objects.length)];
    }

    public CallSite createPojoCallSite(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class[] params, Object receiver, Object[] args) {
        if (!(args [0] instanceof Integer))
          return PojoMetaMethodSite.createNonAwareCallSite(site, metaClass, metaMethod, params, args);
        else
            return new PojoMetaMethodSite(site, metaClass, metaMethod, params) {
                public Object invoke(Object receiver, Object[] args) {
                    final Object[] objects = (Object[]) receiver;
                    return objects[normaliseIndex(((Integer) args[0]).intValue(), objects.length)];
                }

                public Object invokeBinop(Object receiver, Object arg) {
                    final Object[] objects = (Object[]) receiver;
                    return objects[normaliseIndex(((Integer) arg).intValue(), objects.length)];
                }
            };
    }

    protected static int normaliseIndex(int i, int size) {
        int temp = i;
        if (i < 0) {
            i += size;
        }
        if (i < 0) {
            throw new ArrayIndexOutOfBoundsException("Negative array index [" + temp + "] too large for array size " + size);
        }
        return i;
    }
}