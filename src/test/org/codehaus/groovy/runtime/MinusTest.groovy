
class MinusTest extends GroovyTestCase {

	void doTestMinus(String type, def a, b, c, d) {
		assertEquals(type, [ a, b ], [ a, b, c ] - [ c ])
		assertEquals(type, [ a, b ], [ a, b, c ] - [ c, d ])
		assertEquals(type, [], [ a, b, c ] - [ a, b, c ])
		assertEquals(type, [], [ a, b, c ] - [ c, b, a ])
		assertEquals(type, [ a, b, c ], [ a, b, c ] - [])
		assertEquals(type, [], [] - [ a, b, c ])
	}

	void doTestMinusDupplicates(String type, def a, b, c, d) {
		assertEquals(type, [a, a], [a, a] - [])
		assertEquals(type, [a, b, b, c], [a, b, b, c] - [])
		assertEquals(type, [b, b, c], [a, b, b, c] - [a])
		assertEquals(type, [a], [a, b, b, c] - [b, c])
		assertEquals(type, [], [a] - [ a, a ])
	}

	void doTestMinusWithNull(String type, def a, b, c, d) {
		assertEquals(type, [ a, b, c ], [ a, b, c ] - [ null ])
		assertEquals(type, [ a, b, c ], [ a, b, c , null] - [ null ])
		assertEquals(type, [ a, b ], [ a, b, c, null ] - [ null, c ])
		assertEquals(type, [], [] - [ a, b, c, null ])
		assertEquals(type, [ a, b, c, null ], [ a, b, c, null ] - [ ])
		assertEquals(type, [ a, b, null ], [ a, b, c, null ] - [ c ])
	}

	void testMinusComparable() {
	    def a = 'a'
	    def b = 'b'
	    def c = 'c'
	    def d = 'd'
	    
	    doTestMinus('Comparable', a, b, c, d)
	    doTestMinusDupplicates('Comparable', a, b, c, d)
	    doTestMinusWithNull('Comparable', a, b, c, d)
	}
	
	void testMinusNumber() {
	    def a = 1
	    def b = 2
	    def c = 3
	    def d = 4
	    
	    doTestMinus('Number', a, b, c, d)
	    doTestMinusDupplicates('Number', a, b, c, d)
	    doTestMinusWithNull('Number', a, b, c, d)
	}

	void testMinusNumbersMixed() {
	    def a = 1
	    def b = new BigInteger('2')
	    def c = 3.0d
	    def d = new BigDecimal('4.0')
	    
	    doTestMinus('NumbersMixed', a, b, c, d)
	    doTestMinusDupplicates('NumbersMixed', a, b, c, d)
	    doTestMinusWithNull('NumbersMixed', a, b, c, d)
	}

	void testMinusNonComparable() {
	    def a = new Object()
	    def b = new Object()
	    def c = new Object()
	    def d = new Object()
	    
	    doTestMinus('NonComparable', a, b, c, d)
	    doTestMinusDupplicates('NonComparable', a, b, c, d)
	    doTestMinusWithNull('NonComparable', a, b, c, d)
	}
	    
	void testMinusMixed() {
	    def a = new Object()
	    def b = 2
	    def c = '3'
	    def d = new BigDecimal('4.0')
	    
	    doTestMinus('Mixed', a, b, c, d)
	    doTestMinusDupplicates('Mixed', a, b, c, d)
	    doTestMinusWithNull('Mixed', a, b, c, d)
	}
}