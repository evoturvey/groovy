class RangeTest extends GroovyTestCase {

	property x
	
	void testRange() {
	    x = 0

	    for ( i in 0..10 ) {
	        x = x + i
	    }

	    assert x == 45
	}

	void testRangeEach() {
	    x = 0

	    (0..10).each {
	        x = x + it
	    }

	    assert x == 45
	}

	void testRangeStepEach() {
	    x = 0

	    (0..10).step(3) {
	        x = x + it
	    }

	    assert x == 18
	}

	void testRangeStepFor() {
	    x = 0

	    for (it in (0..10).step(3)) {
	        x = x + it
	    }

	    assert x == 18
	}
	
	void testRangeToString() {
	    range = 0..10
	    text = range.toString()
	    assert text == "0..10"
	    text = range.toConsoleOutput()
	    assert text == "0..10"
	    
	    list = [1, 4..10, 9]
	    text = list.toString()
	    assert text == "[1, 4..10, 9]"
	    text = list.toConsoleOutput()
	    assert text == "[1, 4..10, 9]"
	}
}
