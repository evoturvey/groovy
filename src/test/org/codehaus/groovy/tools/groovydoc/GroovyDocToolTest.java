/*
 *
 * Copyright 2007 Jeremy Rayner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.groovy.tools.groovydoc;

import groovy.util.GroovyTestCase;

import java.io.File;

public class GroovyDocToolTest extends GroovyTestCase {
    GroovyDocTool xmlTool;
    private static final String MOCK_DOC = "mock/doc";
    private static final String FS = File.separator;

    public void setUp() {
        xmlTool = new GroovyDocTool(
                new FileSystemResourceManager("src"), // template storage
                "src/main", // source file dirs
                new String[]{"main/org/codehaus/groovy/tools/groovydoc/gstring-templates/top-level/rootDocStructuredData.xml"},
                new String[]{"main/org/codehaus/groovy/tools/groovydoc/gstring-templates/package-level/packageDocStructuredData.xml"},
                new String[]{"main/org/codehaus/groovy/tools/groovydoc/gstring-templates/class-level/classDocStructuredData.xml"}
        );
    }

    public void testGroovyDocTheCategoryMethodClass() throws Exception {
        xmlTool.add("groovy" + FS + "util/CliBuilder.groovy");
        xmlTool.add("groovy" + FS + "lang/GroovyLogTestCase.groovy");
        xmlTool.add("groovy" + FS + "mock" + FS + "interceptor/StrictExpectation.groovy");
        xmlTool.add("groovy" + FS + "ui/Console.groovy");
        xmlTool.add("org" + FS + "codehaus" + FS + "groovy" + FS + "runtime/GroovyCategorySupport.java");
        xmlTool.add("org" + FS + "codehaus" + FS + "groovy" + FS + "runtime/ConvertedMap.java");
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DOC);
        System.out.println("output = " + output);

        String categoryMethodDocument = output.getText(MOCK_DOC + FS + "org" + FS + "codehaus" + FS + "groovy" + FS + "runtime/CategoryMethod.html"); // todo - figure out how to get xml extension for templates

        assertTrue(categoryMethodDocument.indexOf("<method returns=\"boolean\" name=\"hasCategoryInAnyThread\">") > 0);

        String packageDocument = output.getText(MOCK_DOC + FS + "org" + FS + "codehaus" + FS + "groovy" + FS + "runtime/packageDocStructuredData.xml");
        // TODO: fix code then reinstate assertion
        //assertTrue(packageDocument.indexOf("<class name=\"CategoryMethod\" />") > 0);

        String rootDocument = output.getText(MOCK_DOC + FS + "rootDocStructuredData.xml");
        // TODO: fix code then reinstate assertion(s)
        // assertTrue(rootDocument.indexOf("<package name=\"org/codehaus/groovy/runtime\" />") > 0);
        // assertTrue(rootDocument.indexOf("<class path=\"org/codehaus/groovy/runtime/CategoryMethod\" name=\"CategoryMethod\" />") > 0);
    }

    public void testConstructors() throws Exception {
        xmlTool.add("groovy" + FS + "ui/Console.groovy");
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, "mock/doc");

        String consoleDoc = output.getText("mock/doc" + FS + "groovy" + FS + "ui/Console.html");
        System.out.println(consoleDoc);
        assertTrue(consoleDoc.indexOf("<constructor name=\"Console\">") > 0);
        assertTrue(consoleDoc.indexOf("<parameter type=\"ClassLoader\" name=\"parent\" />") > 0);
    }
}