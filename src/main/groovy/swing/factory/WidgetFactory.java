/*
 * $Id:  $
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
 *
 */

package groovy.swing.factory;

import groovy.swing.SwingBuilder;
import java.awt.Component;
import java.util.Map;

/**
 *
 * @author Danno Ferrin
 */
public class WidgetFactory implements Factory {
    
    public Object newInstance(SwingBuilder builder, Object name, Object value, Map properties) throws InstantiationException, IllegalAccessException {
        Object answer = null;
        if (value instanceof Component) {
            answer = value;
        } else if (properties.get(name) instanceof Component) {
            answer = properties.remove(name);
        }
        return answer;
    }    
}
