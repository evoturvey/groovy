/*
 * Copyright 2003-2008 the original author or authors.
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
package org.codehaus.groovy.runtime;

import groovy.lang.IntRange;
import groovy.lang.EmptyRange;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.util.*;

/**
 * Support methods for DefaultGroovyMethods and PluginDefaultMethods.
 */
public class DefaultGroovyMethodsSupport {

    // helper method for getAt and putAt
    protected static RangeInfo subListBorders(int size, IntRange range) {
        int from = DefaultGroovyMethods.normaliseIndex(DefaultTypeTransformation.intUnbox(range.getFrom()), size);
        int to = DefaultGroovyMethods.normaliseIndex(DefaultTypeTransformation.intUnbox(range.getTo()), size);
        boolean reverse = range.isReverse();
        if (from > to) {
            // support list[1..-1]
            int tmp = to;
            to = from;
            from = tmp;
            reverse = !reverse;
        }
        return new RangeInfo(from, to + 1, reverse);
    }

    // helper method for getAt and putAt
    protected static RangeInfo subListBorders(int size, EmptyRange range) {
        int from = DefaultGroovyMethods.normaliseIndex(DefaultTypeTransformation.intUnbox(range.getFrom()), size);
        return new RangeInfo(from, from, false);
    }

    protected static class RangeInfo {
        public int from, to;
        public boolean reverse;

        public RangeInfo(int from, int to, boolean reverse) {
            this.from = from;
            this.to = to;
            this.reverse = reverse;
        }
    }

    protected static Collection cloneSimilarCollection(Collection left, int newCapacity) {
        Collection answer;
        if (left instanceof SortedSet) {
            answer = new TreeSet();
        } else if (left instanceof Set) {
            answer = new HashSet();
        } else {
            answer = new ArrayList(newCapacity);
        }
        answer.addAll(left);
        return answer;
    }

    protected static Map cloneSimilarMap(Map left) {
        Map map;
        if (left instanceof TreeMap)
            map = new TreeMap(left);
        else if (left instanceof LinkedHashMap)
            map = new LinkedHashMap(left);
        else if (left instanceof Properties) {
            map = new Properties();
            map.putAll(left);
        } else if (left instanceof Hashtable)
            map = new Hashtable(left);
        else
            map = new HashMap(left);
        return map;
    }

    protected static Set createLikeSet(Set self) {
        final Set ansSet;
        if (self instanceof SortedSet) {
            ansSet = new TreeSet();
        } else {
            ansSet = new HashSet();
        }
        return ansSet;
    }

    /**
     * Determines if all items of this array are of the same type.
     *
     * @param cols an array of collections
     * @return true if the collections are all of the same type
     */
    protected static boolean sameType(Collection[] cols) {
        List all = new LinkedList();
        for (int i = 0; i < cols.length; i++) {
            all.addAll(cols[i]);
        }
        if (all.size() == 0)
            return true;

        Object first = all.get(0);

        //trying to determine the base class of the collections
        //special case for Numbers
        Class baseClass;
        if (first instanceof Number) {
            baseClass = Number.class;
        } else {
            baseClass = first.getClass();
        }

        for (int i = 0; i < cols.length; i++) {
            for (Iterator iter = cols[i].iterator(); iter.hasNext();) {
                if (!baseClass.isInstance(iter.next())) {
                    return false;
                }
            }
        }
        return true;
    }
}
