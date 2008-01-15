package org.codehaus.groovy.runtime.callsite;

import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import org.codehaus.groovy.runtime.MetaClassHelper;

/**
 * Call site for invoking static methods
*   meta class  - cached
*   method - not cached
*
* @author Alex Tkachman
*/
public class ConstructorMetaMethodSite extends MetaMethodSite {

    public ConstructorMetaMethodSite(MetaClass metaClass, MetaMethod method, Class [] params) {
        super("", metaClass, method, params);
    }

    public final Object call(Object receiver, Object [] args) {
        MetaClassHelper.unwrap(args);
        return metaMethod.doMethodInvoke(metaClass.getTheClass(), args);
    }

    public final boolean accept(Object receiver, Object[] args) {
        return receiver == metaClass.getTheClass() // meta class match receiver
           && MetaClassHelper.sameClasses(params, args, false); // right arguments
    }
}