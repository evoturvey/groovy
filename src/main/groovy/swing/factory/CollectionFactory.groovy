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

/**
 * This returns a mutable java.util.Collection of some sort, to which items are added.  
 */
public class CollectionFactory implements Factory {
    
    public Object newInstance(SwingBuilder builder, Object name, Object value, Map properties) throws InstantiationException, IllegalAccessException {
        SwingBuilder.checkValueIsNull(value, name);
        if (properties.isEmpty()) {
            return new ArrayList();
        } else {
            Map.Entry item = (Map.Entry) properties.entrySet().iterator().next();
            throw new MissingPropertyException(
                "The builder element '$name' is a collections element and has no properties",
                item.key as String, item.value as Class);
        }
    }
}