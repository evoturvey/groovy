/*
 $Id$

 Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.

 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.

 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.

 3. The name "groovy" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Codehaus.  For written permission,
    please contact info@codehaus.org.

 4. Products derived from this Software may not be called "groovy"
    nor may "groovy" appear in their names without prior written
    permission of The Codehaus. "groovy" is a registered
    trademark of The Codehaus.

 5. Due credit should be given to The Codehaus -
    http://groovy.codehaus.org/

 THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package org.codehaus.groovy.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;

/**
 * Represents a module, which consists typically of a class declaration
 * but could include some imports, some statements and multiple classes
 * intermixed with statements like scripts in Python or Ruby
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class ModuleNode extends ASTNode {
    
    private BlockStatement statements = new BlockStatement();
    private List classes = new ArrayList();
    private Map imports = new HashMap();
    private CompileUnit unit;
    private String packageName;
    private String description;
    
    
    public ModuleNode() {
    }
    
    public BlockStatement getStatements() {
        return statements;
    }
    
    public List getClasses() {
        return classes;
    }

    /**
     * @return the class name for the given alias or null if none is available
     */
    public String getImport(String alias) {
        return (String) imports.get(alias);
    }
    
    public void addImport(String alias, String className) {
        imports.put(alias, className);
    }
    
    public void addStatement(Statement node) {
        statements.addStatement(node);
    }
    
    public void addClass(ClassNode node) {
        classes.add(node);
        node.setModule(this);
    }
    
    public void visit(GroovyCodeVisitor visitor) {
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @return the underlying character stream description
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Appends all of the fully qualified class names in this
     * module into the given map
     */
    public void addClasses(Map classMap) {
        for (Iterator iter = classes.iterator(); iter.hasNext(); ) {
            ClassNode node = (ClassNode) iter.next();
            String name = node.getName();
            if (classMap.containsKey(name)) {
                throw new RuntimeException("Error: duplicate class declaration for name: " + name + " and class: " + node);
            }
            classMap.put(name, node);
        }
    }

    public CompileUnit getUnit() {
        return unit;
    }

    void setUnit(CompileUnit unit) {
        this.unit = unit;
    }

}
