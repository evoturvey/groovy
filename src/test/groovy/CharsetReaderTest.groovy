import java.io.File

class CharsetReaderTest extends GroovyTestCase {

	void testSamples() {
		referenceLines = ["a grave    �", "e acute    �", "e grave    �", "i umlaut   �", "o circ     �", "u grave    �", "c cedil    �", "n tilde    �"]

		dir = new File("src/test/groovy")
		dir.eachFile{ file |
			name = file.getName()
			if (name ==~ "charset-.*\\.txt") {
			    println("file: ${name}")
			    
				readLines = file.readLines()
				
				println("readlines     : ${readLines}")
				println("referenceLines: ${referenceLines}")
				
				assert readLines == referenceLines
			}
		}
	}
}