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

import org.codehaus.groovy.tools.shell.CommandSupport
import org.codehaus.groovy.tools.shell.Shell

/**
 * The 'alias' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class AliasCommand
    extends CommandSupport
{
    AliasCommand(final Shell shell) {
        super(shell, 'alias', '\\a')
    }

    Object execute(final List args) {
        assert args != null
        
        if (args.size() < 2) {
            fail("Command 'alias' requires at least 2 arguments") // TODO: i18n
        }
        
        String name = args[0]
        List target = args[1..-1]
        
        //
        // TODO: Balk if already exists (or replace?) but if not an alias, then complain
        //
        
        log.debug("Creating alias '$name' to: $target")
        
        // Register the command
        def cmd = registry << new AliasTargetProxyCommand(shell, name, target)
        
        // Add a completor if we can
        def reader = shell.runner?.reader
        
        if (reader) {
            reader.addCompletor(cmd.completor)
        }
    }
}

class AliasTargetProxyCommand
    extends CommandSupport
{
    private static int counter = 0
    
    final List args
    
    AliasTargetProxyCommand(final Shell shell, final String name, final List args) {
        super(shell, name, "\\a${counter++}".toString()) // HACK: Must toString() here
        
        assert args
        
        this.args = args
    }
    
    String getDescription() {
        return "Alias to: ${args.join(' ')}"
    }

    String getUsage() {
        return ''
    }
    
    String getHelp() {
        return "Alias to: ${args.join(' ')}"
    }
    
    Object execute(List args) {
        args = this.args + args
        
        log.debug("Executing with args: $args")
        
        return shell.execute(args.join(' '))
    }
}
