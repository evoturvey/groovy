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

package org.codehaus.groovy.tools.shell.commands

import jline.ArgumentCompletor
import jline.NullCompletor

import org.codehaus.groovy.control.CompilationFailedException

import org.codehaus.groovy.tools.shell.CommandSupport
import org.codehaus.groovy.tools.shell.Shell
import org.codehaus.groovy.tools.shell.completor.SimpleCompletor
import org.codehaus.groovy.tools.shell.completor.ClassNameCompletor

/**
 * The 'import' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class ImportCommand
    extends CommandSupport
{
    ImportCommand(final Shell shell) {
        super(shell, 'import', '\\i')
    }

    Object execute(final List args) {
        assert args != null

        if (args.isEmpty()) {
            io.error.println("Command 'import' requires one or more arguments") // TODO: i18n
            return
        }

        def buff = [ 'import ' + args.join(' ') ]
        buff << 'def dummp = false'
        
        def cl = shell.shell.classLoader
        
        def type
        try {
            type = cl.parseClass(buff.join(shell.NEWLINE))

            log.debug("Adding import: ${buff[0]}")

            imports << buff[0]
        }
        catch (CompilationFailedException e) {
            def msg = "Invalid import definition: '${buff[0]}'; reason: $e.message" // TODO: i18n
            log.debug(msg, e)
            io.error.println(msg)
        }
        finally {
            // Remove the class generated while testing the import syntax
            cl.classCache.remove(type?.name)
        }
    }
    
    protected List createCompletors() {
        return [ new ImportCommandCompletor(shell.shell.classLoader), null ]
    }
}

/**
 * Completor for the 'import' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class ImportCommandCompletor
    extends ArgumentCompletor
{
    ImportCommandCompletor(final GroovyClassLoader classLoader) {
        super([
            new ClassNameCompletor(classLoader),
            new SimpleCompletor('as'),
            new NullCompletor()
        ])
    }
}