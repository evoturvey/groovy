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
 * The 'purgeimports' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class PurgeImportsCommand
    extends CommandSupport
{
    PurgeImportsCommand(final Shell shell) {
        super(shell, 'purgeimports', '\\pi')
    }
    
    Object execute(final List args) {
        assert args != null

        if (args.size() > 0) {
            io.error.println(messages.format('error.unexpected_args', args.join(' ')))
            return
        }
        
        if (imports.isEmpty()) {
            io.output.println("No custom imports have been defined") // TODO: i18n
            return
        }

        imports.clear()

        if (io.verbose) {
            io.output.println("Custom imports purged") // TODO: i18n
        }
    }
}