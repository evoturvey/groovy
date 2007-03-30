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

import java.util.*;

import org.codehaus.groovy.groovydoc.*;

public class SimpleGroovyExecutableMemberDoc extends SimpleGroovyMemberDoc implements GroovyExecutableMemberDoc {
	List parameters;
	
	public SimpleGroovyExecutableMemberDoc(String name) {
		super(name);
		parameters = new ArrayList();
	}
	public GroovyParameter[] parameters() {
		return (GroovyParameter[]) parameters.toArray(new GroovyParameter[parameters.size()]);
	}
	public void add(GroovyParameter parameter) {
		parameters.add(parameter);
	}

	
	public String flatSignature() {/*todo*/return null;}
	public boolean isNative() {/*todo*/return false;}
	public boolean isSynchronized() {/*todo*/return false;}
	public boolean isVarArgs() {/*todo*/return false;}
//	public GroovyParamTag[] paramTags() {/*todo*/return null;}
	public String signature() {/*todo*/return null;}
	public GroovyClassDoc[] thrownExceptions() {/*todo*/return null;}
	public GroovyType[] thrownExceptionTypes() {/*todo*/return null;}
//	public GroovyThrowsTag[] throwsTags() {/*todo*/return null;}
//	public GroovyTypeVariable[] typeParameters() {/*todo*/return null;}
//	public GroovyParamTag[] typeParamTags() {/*todo*/return null;}
}
