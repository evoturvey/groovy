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
package groovy

class EqualsTest extends GroovyTestCase {

    void testParentChildrenEquals() {
        def x = new Date()
        def y = new java.sql.Time(x.time)
        def z = new java.sql.Timestamp(x.time)
        assert y == x
        assert x == y
        assert z == x
        assert x != z // Gotcha see: http://mattfleming.com/node/141
        // above assert is just documenting that Groovy currently
        // follows Java behaviour for this case
    }

    void testUnrelatedComparablesShouldNeverBeEqual() {
        def x = new Date()
        def n = 3
        assert n != x
        assert x != n
    }

}
