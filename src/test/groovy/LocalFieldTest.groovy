package groovy;

import org.codehaus.groovy.GroovyTestCase;

class LocalFieldTest extends GroovyTestCase {

	// lets define some fields - no necessary for instance variables but supported
    private x;
    private String y;
    private static z;
    private static Integer iz;
	
	void testAssert() {
        this.x = "abc";
	    
	    assert this.x := "abc";
	    assert this.x != "def";
	}
}