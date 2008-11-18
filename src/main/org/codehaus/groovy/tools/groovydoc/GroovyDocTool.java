/*
 * Copyright 2007 Jeremy Rayner
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
package org.codehaus.groovy.tools.groovydoc;

import org.apache.tools.ant.types.Path;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import org.codehaus.groovy.groovydoc.GroovyRootDoc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class GroovyDocTool {

    /**
     * Constructor for use by people who only want to interact with the Groovy Doclet Tree (rootDoc)
     * @param sourcepath where the sources to be added can be found
     */
    public GroovyDocTool(Path sourcepath) {
        this(null,sourcepath,null);
    }

    public GroovyDocTool(ResourceManager resourceManager, Path sourcepath, String classTemplate) {
		this(resourceManager, sourcepath, new String[]{}, new String[]{}, new String[] {classTemplate}, new ArrayList());
	}

	public GroovyDocTool(ResourceManager resourceManager, Path sourcepath, String[] docTemplates, String[] packageTemplates, String[] classTemplates, List links) {
		rootDocBuilder = new GroovyRootDocBuilder(this, sourcepath, links);
		if (resourceManager == null) {
            templateEngine = null;
        } else {
            templateEngine = new GroovyDocTemplateEngine(this, resourceManager, docTemplates, packageTemplates, classTemplates);
        }
    }

    public void add(List filenames) throws RecognitionException, TokenStreamException, IOException {
		if (templateEngine != null) {
            // only print out if we are being used for template generation
            System.out.println("Loading source files for " + filenames);
        }
		rootDocBuilder.buildTree(filenames);
	}
	
	public GroovyRootDoc getRootDoc() {
		return rootDocBuilder.getRootDoc();
	}

	public void renderToOutput(OutputTool output, String destdir) throws Exception {
		if (templateEngine != null) {
            GroovyDocWriter writer = new GroovyDocWriter(this, output, templateEngine);
            GroovyRootDoc rootDoc = rootDocBuilder.getRootDoc();
            writer.writeRoot(rootDoc, destdir);
            writer.writePackages(rootDoc, destdir);
            writer.writeClasses(rootDoc, destdir);
        } else {
            throw new UnsupportedOperationException("No template engine was found");
        }
	}
	
	private final GroovyRootDocBuilder rootDocBuilder;
	private final GroovyDocTemplateEngine templateEngine;

	String getPath(String filename) {
		String path = new File(filename).getParent();
		// path length of 1 indicates that probably is 'default package' i.e. "/"
		if (path == null || path.length() == 1) {
			path = "DefaultPackage"; // "DefaultPackage" for 'default package' path, rather than null...
		}
		return path;
	}
	String getFile(String filename) {
        return new File(filename).getName();        
	}
		

}
