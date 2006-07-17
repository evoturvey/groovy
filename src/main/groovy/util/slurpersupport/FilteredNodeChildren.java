/*
 * Copyright 2005 John G. Wilson
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

package groovy.util.slurpersupport;

import java.util.Iterator;
import java.util.Map;

import org.codehaus.groovy.runtime.InvokerHelper;

import groovy.lang.Closure;

/**
 * @author John Wilson
 *
 */

public class FilteredNodeChildren extends NodeChildren {
  private final Closure closure;
  
  public FilteredNodeChildren(final GPathResult parent, final Closure closure, final Map namespaceTagHints) {
    super(parent, parent.name, namespaceTagHints);
    this.closure = closure;
  }

  /* (non-Javadoc)
   * @see org.codehaus.groovy.sandbox.util.slurpersupport.GPathResult#iterator()
   */
  public Iterator iterator() {
    return new Iterator() {
      final Iterator iter = FilteredNodeChildren.this.parent.iterator();
      
      public boolean hasNext() {
        return this.iter.hasNext();
      }
      
      public Object next() {
        while (iter.hasNext()) {
        final Object childNode = iter.next();
      
      
          if (InvokerHelper.asBool(FilteredNodeChildren.this.closure.call(new Object[]{childNode}))) {
            return childNode;
          }
        }
        
        return null;
      }
      
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /* (non-Javadoc)
   * @see org.codehaus.groovy.sandbox.util.slurpersupport.NodeChildren#iterator()
   */
  public Iterator nodeIterator() {
    return new NodeIterator(this.parent.nodeIterator()) {
              /* (non-Javadoc)
               * @see org.codehaus.groovy.sandbox.util.slurpersupport.NodeIterator#getNextNode(java.util.Iterator)
               */
              protected Object getNextNode(final Iterator iter) {
                while (iter.hasNext()) {
                final Object node = iter.next();
                
                  if (InvokerHelper.asBool(FilteredNodeChildren.this.closure.call(new Object[]{new NodeChild((Node)node, FilteredNodeChildren.this.parent, FilteredNodeChildren.this.namespaceTagHints)}))) {
                    return node;
                  }
                }
                
                return null;
              }   
            };
  }
  
}
