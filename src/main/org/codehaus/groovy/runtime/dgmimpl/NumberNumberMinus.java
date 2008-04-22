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
package org.codehaus.groovy.runtime.dgmimpl;

import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.typehandling.NumberMath;

public final class NumberNumberMinus extends NumberNumberMetaMethod {
    public String getName() {
        return "minus";
    }

    public Object invoke(Object object, Object[] arguments) {
        return NumberMath.subtract((Number) object, (Number) arguments[0]);
    }

    /**
     * Substraction of two Numbers.
     *
     * @param left  a Number
     * @param right another Number to substract to the first one
     * @return the substraction
     */
    public static Number minus(Number left, Number right) {
        return NumberMath.subtract(left, right);
    }

    public CallSite createPojoCallSite(CallSite site, MetaClassImpl metaClass, MetaMethod metaMethod, Class[] params, Object receiver, Object[] args) {
        NumberMath m = NumberMath.getMath((Number)receiver, (Number)args[0]);


        if (receiver instanceof Integer) {
            if (args[0] instanceof Integer)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Integer && arg instanceof Integer)
                                && checkPojoMetaClass()
                              ? Integer.valueOf(((Integer) receiver).intValue() - ((Integer) arg).intValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return Integer.valueOf(((Integer) receiver).intValue() - ((Integer) args[0]).intValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return Integer.valueOf(((Integer) receiver).intValue() - ((Integer) arg).intValue());
                    }
                };

            if (args[0] instanceof Long)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Integer && arg instanceof Long)
                                && checkPojoMetaClass()
                              ? new Long(((Integer) receiver).longValue() - ((Long) arg).longValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Long(((Integer) receiver).longValue() - ((Long) args[0]).longValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Long(((Integer) receiver).longValue() - ((Long) arg).longValue());
                    }
                };

            if (args[0] instanceof Float)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Integer && arg instanceof Float)
                                && checkPojoMetaClass()
                              ? new Double(((Integer) receiver).doubleValue() - ((Float) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Integer) receiver).doubleValue() - ((Float) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Integer) receiver).doubleValue() - ((Float) arg).doubleValue());
                    }
                };

            if (args[0] instanceof Double)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Integer && arg instanceof Double)
                                && checkPojoMetaClass()
                              ? new Double(((Integer) receiver).doubleValue() - ((Double) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Integer) receiver).doubleValue() - ((Double) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Integer) receiver).doubleValue() - ((Double) arg).doubleValue());
                    }
                };
            }

        if (receiver instanceof Long) {
            if (args[0] instanceof Integer)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Long && arg instanceof Integer)
                                && checkPojoMetaClass()
                              ? new Long(((Long) receiver).longValue() - ((Integer) arg).longValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Long(((Long) receiver).longValue() - ((Integer) args[0]).longValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Long(((Long) receiver).longValue() - ((Integer) arg).longValue());
                    }
                };

            if (args[0] instanceof Long)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Long && arg instanceof Long)
                                && checkPojoMetaClass()
                              ? new Long(((Long) receiver).longValue() - ((Long) arg).longValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Long(((Long) receiver).longValue() - ((Long) args[0]).longValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Long(((Long) receiver).longValue() - ((Long) arg).longValue());
                    }
                };

            if (args[0] instanceof Float)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Long && arg instanceof Float)
                                && checkPojoMetaClass()
                              ? new Double(((Long) receiver).doubleValue() - ((Float) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Long) receiver).doubleValue() - ((Float) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Long) receiver).doubleValue() - ((Float) arg).doubleValue());
                    }
                };

            if (args[0] instanceof Double)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Long && arg instanceof Double)
                                && checkPojoMetaClass()
                              ? new Double(((Long) receiver).doubleValue() - ((Double) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Long) receiver).doubleValue() - ((Double) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Long) receiver).doubleValue() - ((Double) arg).doubleValue());
                    }
                };
            }

        if (receiver instanceof Float) {
            if (args[0] instanceof Integer)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Float && arg instanceof Integer)
                                && checkPojoMetaClass()
                              ? new Double(((Float) receiver).doubleValue() - ((Integer) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Float) receiver).doubleValue() - ((Integer) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Float) receiver).doubleValue() - ((Integer) arg).doubleValue());
                    }
                };

            if (args[0] instanceof Long)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Float && arg instanceof Long)
                                && checkPojoMetaClass()
                              ? new Double(((Float) receiver).doubleValue() - ((Long) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Float) receiver).doubleValue() - ((Long) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Float) receiver).doubleValue() - ((Long) arg).doubleValue());
                    }
                };

            if (args[0] instanceof Float)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Float && arg instanceof Float)
                                && checkPojoMetaClass()
                              ? new Double(((Float) receiver).doubleValue() - ((Float) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Float) receiver).doubleValue() - ((Float) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Float) receiver).doubleValue() - ((Float) arg).doubleValue());
                    }
                };

            if (args[0] instanceof Double)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Float && arg instanceof Double)
                                && checkPojoMetaClass()
                              ? new Double(((Float) receiver).doubleValue() - ((Double) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Float) receiver).doubleValue() - ((Double) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Float) receiver).doubleValue() - ((Double) arg).doubleValue());
                    }
                };
            }

        if (receiver instanceof Double) {
            if (args[0] instanceof Integer)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Double && arg instanceof Integer)
                                && checkPojoMetaClass()
                              ? new Double(((Double) receiver).doubleValue() - ((Integer) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Double) receiver).doubleValue() - ((Integer) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Double) receiver).doubleValue() - ((Integer) arg).doubleValue());
                    }
                };

            if (args[0] instanceof Long)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Double && arg instanceof Long)
                                && checkPojoMetaClass()
                              ? new Double(((Double) receiver).doubleValue() - ((Long) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Double) receiver).doubleValue() - ((Long) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Double) receiver).doubleValue() - ((Long) arg).doubleValue());
                    }
                };

            if (args[0] instanceof Float)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Double && arg instanceof Float)
                                && checkPojoMetaClass()
                              ? new Double(((Double) receiver).doubleValue() - ((Float) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Double) receiver).doubleValue() - ((Float) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Double) receiver).doubleValue() - ((Float) arg).doubleValue());
                    }
                };

            if (args[0] instanceof Double)
                return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
                    public final Object callBinop(Object receiver, Object arg) {
                        return (receiver instanceof Double && arg instanceof Double)
                                && checkPojoMetaClass()
                              ? new Double(((Double) receiver).doubleValue() - ((Double) arg).doubleValue())
                              : super.callBinop(receiver,arg);
                    }

                    public final Object invoke(Object receiver, Object[] args) {
                        return new Double(((Double) receiver).doubleValue() - ((Double) args[0]).doubleValue());
                    }

                    public final Object invokeBinop(Object receiver, Object arg) {
                        return new Double(((Double) receiver).doubleValue() - ((Double) arg).doubleValue());
                    }
                };
            }

        return new NumberNumberCallSite (site, metaClass, metaMethod, params, (Number)receiver, (Number)args[0]){
            public final Object invoke(Object receiver, Object[] args) {
                return math.subtractImpl((Number)receiver,(Number)args[0]);
            }

            public final Object invokeBinop(Object receiver, Object arg) {
                return math.subtractImpl((Number)receiver,(Number)arg);
            }
        };
    }
}
