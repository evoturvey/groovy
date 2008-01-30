package org.codehaus.groovy.classgen;

import groovy.lang.MetaClass;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import org.codehaus.groovy.reflection.GeneratedMetaMethod;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.callsite.CallSiteArray;
import org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite;
import org.codehaus.groovy.runtime.callsite.StaticMetaMethodSite;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifierClassVisitor;

public class JO extends GeneratedMetaMethod implements Opcodes {
    public static MetaClass staticMetaClass;
    public static Class myMetaClass = initMyClass();

    private static CallSiteArray callSiteArray;

    private static Class initMyClass() {
        try {
            return Class.forName("org.codehaus.groovy.classgen.JO");
        } catch (ClassNotFoundException e) {
            final NoClassDefFoundError foundError = new NoClassDefFoundError();
            foundError.initCause(e);
            throw foundError;
        }
    }

    protected MetaClass getStaticMetaClass () {
        final Class aClass = getClass();
        if (aClass == myMetaClass) {
          if (staticMetaClass == null)
            staticMetaClass = InvokerHelper.getMetaClass(this);
          return staticMetaClass;
        }
        else
          return InvokerHelper.getMetaClass(aClass);
    }

    public JO() {
        super();
    }

    public boolean isValidMethod(Class[] arguments) {
        return arguments == null || getParameterTypes()[0].isAssignableFrom(arguments[0]);
    }

    public Object invoke(Object object, Object[] arguments) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public final Object doMethodInvoke(Object object, Object[] argumentArray) {
        return invoke(object, coerceArgumentsToClasses(argumentArray));
    }

    public static void main(String[] args) throws Exception {
//        ASMifierClassVisitor.main(new String[]{"target/test-classes/org/codehaus/groovy/classgen/CompiledStaticMetaMethodSite.class"});
//        ASMifierClassVisitor.main(new String[]{"target/test-classes/org/codehaus/groovy/classgen/TestCallSite.class"});
        ASMifierClassVisitor.main(new String[]{"target/test-classes/spectralnorm.class"});
//        ASMifierClassVisitor.main(new String[]{"target/test-classes/groovy/bugs/CustomMetaClassTest.class"});
    }
}

class TestCallSite extends PojoMetaMethodSite {

    public TestCallSite(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class[] params) {
        super(site, metaClass, metaMethod, params);
    }

    public Object invoke(Object receiver, Object[] args) {
        return super.invoke(receiver, args);
    }
}

class CompiledStaticMetaMethodSite extends StaticMetaMethodSite {

    public CompiledStaticMetaMethodSite(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class[] params) {
        super(site, metaClass, metaMethod, params);
    }

    public Object invoke(Object receiver, Object[] args) {
        return InvokerHelper.runScript((Class)args[0],(String[])args[1]);
    }
}