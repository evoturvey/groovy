class ArrayTypeTest extends GroovyTestCase {

    void testClosureWithTypedParam() {
        c = {String[] foo->println("called with $foo") }
        c(null)
    }

    void testVariableType() {
        Object[] foo = methodThatReturnsArray()
        println "foo is $foo"

    }



    Object[] methodThatReturnsArray() {
        println "Invoked the method"
        return null
    }
}
