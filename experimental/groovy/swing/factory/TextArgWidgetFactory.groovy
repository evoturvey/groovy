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

package groovy.swing.factory

import groovy.swing.SwingBuilder
import org.codehaus.groovy.runtime.InvokerHelper

/**
 *
 * @author Danno Ferrin
 */
public class TextArgWidgetFactory implements Factory {
    
    Class klass;
    
    public TextArgWidgetFactory(Class klass) {
        this.klass = klass;
    }
    
    public Object newInstance(SwingBuilder builder, Object name, Object value, Map properties) throws InstantiationException, IllegalAccessException {
        if (SwingBuilder.checkValueIsTypeNotString(value, name, klass)) {
            return value;
        }
        
        Object widget = klass.newInstance();
        
        if (value instanceof String) {
            // this does not create property setting order issues, since the value arg preceeds all properties in the builder element
            InvokerHelper.setProperty(widget, "text", value);
        }
        
        return widget;
    }    

}