

/**
 * Some GPath tests using maps and lists
 */
class GPathTest extends GroovyTestCase {
    
    void testSimpleGPathExpressions() {
        tree = createTree()
        
        assert tree.people.find { it.name == 'James' }.location == 'London'
        
        assert tree.people.name == ['James', 'Bob']

        expected = ['James works on 2 project(s)', 'Bob works on 2 project(s)']
        assert tree.people.findAll { it.projects.size() > 1 }.collect { it.name + ' works on ' + it.projects.size() + " project(s)" } == expected
}
    
    protected createTree() {       
        return [	
            'people': [
                ['name' : 'James', 'location':'London', 'projects':['geronimo', 'groovy'] ],
                ['name' : 'Bob', 'location':'Atlanta', 'projects':['drools', 'groovy'] ]
			] 
		]
    }
}
