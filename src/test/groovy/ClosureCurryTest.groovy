/** 
 * @author Hallvard Tr�tteberg
 * @version $Revision$
 */
class ClosureCurryTest extends GroovyTestCase {

    void testCurry() {
		clos1 = {s1, s2 | s1 + s2}
		clos2 = clos1.curry("hi")
		value = clos2("there") 
		assert value == "hithere"
		
		clos3 = {s1, s2, s3 | s1 + s2 + s3}
		clos4 = clos3.curry('a')
		clos5 = clos4.curry('b')
		clos6 = clos4.curry('x')
		clos7 = clos4.curry('f', 'g')
		value = clos5('c')
		assert value == "abc"
		value = clos6('c')
		assert value == "axc"
		value = clos4('y', 'z')
		assert value == "ayz"
		value = clos7()
		assert value == "afg"
		
		clos3 = {s1, s2, s3 | s1 + s2 + s3}.asWritable()
		clos4 = clos3.curry('a')
		clos5 = clos4.curry('b')
		clos6 = clos4.curry('x')
		clos7 = clos4.curry('f', 'g')
		value = clos5('c')
		assert value == "abc"
		value = clos6('c')
		assert value == "axc"
		value = clos4('y', 'z')
		assert value == "ayz"
		value = clos7()
		assert value == "afg"
		
		clos3 = {s1, s2, s3 | s1 + s2 + s3}
		clos4 = clos3.curry('a').asWritable()
		clos5 = clos4.curry('b').asWritable()
		clos6 = clos4.curry('x').asWritable()
		clos7 = clos4.curry('f', 'g').asWritable()
		value = clos5('c')
		assert value == "abc"
		value = clos6('c')
		assert value == "axc"
		value = clos4('y', 'z')
		assert value == "ayz"
		value = clos7()
		assert value == "afg"
    }  
}
