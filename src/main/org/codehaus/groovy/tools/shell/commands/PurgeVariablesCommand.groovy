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

import org.codehaus.groovy.tools.shell.InteractiveShell

/**
 * The 'purgevariables' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class PurgeVariablesCommand
    extends CommandSupport
{
    PurgeVariablesCommand(final InteractiveShell shell) {
        super(shell, 'purgevariables', '\\pv')
    }

    void execute(final List args) {
        assert args != null

        if (args.size() > 0) {
            io.error.println("Unexpected arguments: $args") // TODO: i18n
            return
        }
        
        def vars = shell.shell.context.variables

        if (vars.isEmpty()) {
            io.output.println('No variables defined') // TODO: i18n
            return
        }

        vars.clear()

        if (shell.verbose) {
            io.output.println("Custom variables purged")
        }
    }
}
