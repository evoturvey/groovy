package org.codehaus.groovy.runtime;

import groovy.lang.*;

public class CallSite {

    CachedCall cachedCall;

    private static class CachedCall {
        final String name;
        final Class  receiverClass;

        CachedCall (String name, Object receiver, Object [] args) {
            this.name = name;
            receiverClass = receiver.getClass();
        }

        Object call(Object receiver, Object [] args) {
            return null;
        }

        boolean accept(String name, Object receiver, Object[] args) {
            return receiverClass == receiver.getClass() && this.name.equals(name);
        }
    }

    private static class ClassCall extends CachedCall {
        ClassCall(String name, Object receiver, Object[] args) {
            super(name, receiver, args);
        }

        Object call(Object receiver, Object [] args) {
            Class theClass = (Class) receiver;
            MetaClass metaClass = InvokerHelper.metaRegistry.getMetaClass(theClass);
            return metaClass.invokeStaticMethod(receiver, name, InvokerHelper.asArray(args));
        }
    }

    private static class PojoCall extends CachedCall {
        final MetaClass metaClass;
        final MetaMethod metaMethod;
        final Class [] params;

        public PojoCall(String name, Object receiver, Object[] args) {
            super(name, receiver, args);
            metaClass = InvokerHelper.metaRegistry.getMetaClass(receiverClass);
            params = MetaClassHelper.convertToTypeArray(args);
            metaMethod = metaClass.pickMethod(name, params);
        }

        Object call(Object receiver, Object[] args) {
            MetaClassHelper.unwrap(args);
            return metaMethod.doMethodInvoke(receiver,  args);
        }

        boolean accept(String name, Object receiver, Object[] args) {
            return super.accept(name, receiver, args) && MetaClassHelper.sameClasses(params, args, false);
        }
    }

    private static class PogoCall extends CachedCall {
        public PogoCall(String name, Object receiver, Object[] args) {
            super(name, receiver, args);
        }

        Object call(Object receiver, Object[] args) {
            GroovyObject groovy = (GroovyObject) receiver;
            try {
                return groovy.getMetaClass().invokeMethod(receiver, name, InvokerHelper.asArray(args));
            } catch (MissingMethodException e) {
                if (e.getMethod().equals(name) && receiver.getClass() == e.getType()) {
                    // in case there's nothing else, invoke the object's own invokeMethod()
                    return groovy.invokeMethod(name, InvokerHelper.asUnwrappedArray(args));
                }
                throw e;
            }
        }
    }

    private static class InterceptingCall extends CachedCall {
        public InterceptingCall(String name, Object receiver, Object[] args) {
            super(name, receiver, args);
        }

        Object call(Object receiver, Object[] args) {
          return ((GroovyObject)receiver).invokeMethod(name, InvokerHelper.asUnwrappedArray(args));
        }
    }

    private CachedCall getCall (String name, Object receiver, Object [] args) {
        CachedCall call = cachedCall;
        if (call == null || !call.accept(name, receiver, args)) {
          cachedCall = call = createCall(name, receiver, args);
        }
        return call;
    }

    private static CachedCall createCall(String name, Object receiver, Object[] args) {
        if (receiver instanceof Class)
          return new ClassCall(name, receiver, args);

        if (!(receiver instanceof GroovyObject))
          return new PojoCall(name, receiver, args);

        if (receiver instanceof GroovyInterceptable)
          return new InterceptingCall(name, receiver, args);

        return new PogoCall(name, receiver, args);
    }


    public final Object call (String name, Object receiver, Object [] args) throws Throwable {
        try {
            if (receiver == null) {
                receiver = NullObject.getNullObject();
            }

            return getCall (name, receiver, args).call (receiver, args);
        } catch (GroovyRuntimeException gre) {
            throw ScriptBytecodeAdapter.unwrap(gre);
        }
    }
}
