package org.codehaus.groovy.tools

import groovy.xml.StreamingMarkupBuilder

import java.io.File

import com.thoughtworks.qdox.JavaDocBuilder
import com.thoughtworks.qdox.model.JavaSource
import com.thoughtworks.qdox.model.JavaClass
import com.thoughtworks.qdox.model.JavaMethod
import com.thoughtworks.qdox.model.JavaParameter
import com.thoughtworks.qdox.model.Type
import java.util.*;


/**
 * Generate documentation about the methods provided by the Groovy Development Kit
 * enhancing the standard JDK classes.
 *
 * @author Guillaume Laforge, John Wilson
 */
class DocGenerator
{
	def sourceFiles = []
	File outputFolder
	JavaDocBuilder builder
	// categorize all groovy methods per core JDK class to which it applies
	def jdkEnhancedClasses = [:]
    def packages = [:]
	def sortedPackages

	DocGenerator(sourceFiles, File outputFolder)
	{
		this.sourceFiles = sourceFiles
		this.outputFolder = outputFolder
		parse()
	}

	/**
	 * Parse the DefaultGroovyMethods class to build a graph representing the structure of the class,
	 * with its methods, javadoc comments and tags.
	 */
	private void parse()
	{
		builder = new JavaDocBuilder()
		sourceFiles.each {
			println "adding reader for $it"
			builder.addSource(it.newReader())
		}

		def sources = builder.getSources()

		def methods = []
		sources.each { source ->
			def classes = source.getClasses()
			classes.each { aClass ->
				methods.addAll (aClass.methods as List)
			}
		}
	
        def start = System.currentTimeMillis();
		for (method in methods) {
			if (method.isPublic() && method.isStatic()) {
				def parameters = method.getParameters()
				def jdkClass = parameters[0].getType().toString()

				if (jdkClass.startsWith('groovy')) {
					// nothing, skip it
				}
				else if (jdkEnhancedClasses.containsKey(jdkClass)) {
				    List l = jdkEnhancedClasses[jdkClass];
					l.add(method)
			    }
				else
					jdkEnhancedClasses[jdkClass] = [method]
			}
		}

		jdkEnhancedClasses.keySet().each { className ->
        	def thePackage = className.contains(".") ? className.replaceFirst(/\.[^\.]*$/, "") : ""
        	if (!packages.containsKey(thePackage)) {
        		packages[thePackage] = []
        	}
    		packages[thePackage] << className
        }
        sortedPackages = new TreeSet(packages.keySet())
	}

	/**
	 * Builds an HTML page from the structure of DefaultGroovyMethods.
	 */
		
	def generateNew() {
		def engine = new groovy.text.SimpleTemplateEngine()

		// the index
		def templateIndex = createTemplate(engine, 'index.html')
		def out = new File(outputFolder, 'index.html')
		def binding = [packages: sortedPackages]
		out.withWriter {
			it << templateIndex.make(binding)
		}
		// the overview
		def templateOverview = createTemplate(engine, 'overview-summary.html')
		out = new File(outputFolder, 'overview-summary.html')
		binding = [packages: sortedPackages]
		out.withWriter {
			it << templateOverview.make(binding)
		}
		
		def templateOverviewFrame = createTemplate(engine, 'template.overview-frame.html')
		out = new File(outputFolder, 'overview-frame.html')
		binding = [packages: sortedPackages]
		out.withWriter {
			it << templateOverviewFrame.make(binding)
		}
		
		// the allclasses-frame.html
		def templateAllClasses = createTemplate(engine, 'template.allclasses-frame.html')
		out = new File(outputFolder, 'allclasses-frame.html')
		binding = [classes: jdkEnhancedClasses.keySet().sort { it.replaceAll('.*\\.', '')}]
		out.withWriter {
			it << templateAllClasses.make(binding)
		}
		
		// the package-frame.html for each package
		def templatePackageFrame = createTemplate(engine, 'template.package-frame.html')
		packages.each { curPackage, packageClasses ->
			def packageName = curPackage ? curPackage : "primitive-types"
			generatePackageFrame(templatePackageFrame, packageName, packageClasses)
		}		
		
		// the class.html for each class
		def templateClass = createTemplate(engine, 'template.class.html')
		packages.each { curPackage, packageClasses ->
			def packageName = curPackage ? curPackage : "primitive-types"
			packageClasses.each {
				generateClassDetails(templateClass, packageName, it)
			}
		}		
		
	}
	private generateClassDetails(template, curPackage, aClass)
	{
		def dir = new File(outputFolder, curPackage.replaceAll('\\.', File.separator))
		dir.mkdirs()
		def out = new File(dir, aClass.replaceAll('.*\\.', '') + '.html')
		def listOfMethods = jdkEnhancedClasses[aClass].sort{ it.name }
		def methods = []
		listOfMethods.each { method ->
			def parameters = method.getTagsByName("param").collect { [name: it.value.replaceAll(' .*', ''), comment: it.value.replaceAll('^\\w*', '')]}
			if (parameters)
				parameters.remove(0) // method is static, first arg is the "real this"

			def returnType = getReturnType(method)
			def methodInfo = [name: method.name, 
			                  comment: getComment(method),
			                  shortComment: getComment(method).replaceAll('\\..*', ''),
			                  returnComment: method.getTagByName("return")?.getValue() ?: '',
			                  returnTypeDocUrl: getDocUrl(returnType),
			                  parametersSignature: getParametersDecl(method),
			                  parametersDocUrl: getParametersDocUrl(method),
			                  parameters: parameters,
			                  isStatic: method.parentClass.name == 'DefaultGroovyStaticMethods']
			methods << methodInfo
		}

		def binding = [className: aClass.replaceAll(/.*\./, ''),
		           packageName: curPackage,
		           methods: methods]
		out.withWriter {
			it << template.make(binding)
		}
	}

	private String getParametersDocUrl(method)
	{
		getParameters(method).collect{"${getDocUrl(it.type.toString())} ${it.getName()}" }.join(", ")
	}

	private String getDocUrl(type)
	{
		if (!type.contains('.'))
			return type
		
		def shortClassName = type.replaceAll(".*\\.", "")
		def packageName = type[0..(-shortClassName.size()-2)] 
		def apiBaseUrl, title
		if (type.startsWith("groovy")) {
			apiBaseUrl = "http://groovy.codehaus.org/api/"
			title = "Groovy class in $packageName"
		}
		else {
			apiBaseUrl = "http://java.sun.com/j2se/1.4.2/docs/api/"
			title = "JDK class in $packageName"
		}

		def url = apiBaseUrl + type.replaceAll("\\.", "/") + '.html'
		return "<a href='$url' title='$title'>$shortClassName</a>"
	}

	private generatePackageFrame(templatePackageFrame, curPackage, packageClasses)
	{
		def dir = new File(outputFolder, curPackage.replaceAll('\\.', File.separator))
		dir.mkdirs()
		def out = new File(dir, 'package-frame.html')
		def binding = [classes: packageClasses.sort().collect { it.replaceAll(/.*\./, '')},
		           packageName: curPackage]
		out.withWriter {
			it << templatePackageFrame.make(binding)
		}
	}
	
	def createTemplate(templateEngine, resourceFile)
	{
		def resourceUrl = getClass().getResource(resourceFile)
		return templateEngine.createTemplate(resourceUrl.text)
	}


	/**
 	* Retrieves a String representing the return type
 	*/
	private getReturnType(method)
	{
	    def returnType = method.getReturns()
	    
	    if (returnType != null) {
	    	    return returnType.toString()
	    } else {
	    	    return ""
	    }
	}

	/**
	 * Retrieve a String representing the declaration of the parameters of the method passed as parameter.
	 *
	 * @param method a method
	 * @return the declaration of the method (long version)
	 */
	private getParametersDecl(method)
	{
		getParameters(method).collect{ "${it.getType()} ${it.getName()}" }.join(", ")
	}

	/**
	 * Retrieve the parameters of the method.
	 *
	 * @param method a method
	 * @return a list of parameters without the first one
	 */
	private getParameters(method)
	{
	    if (method.getParameters().size() > 1)
		    return method.getParameters().toList()[1..-1]
		else
		    return []
	}

	/**
	 * Retrieve the JavaDoc comment associated with the method passed as parameter.
	 *
	 * @param method a method
	 * @return the JavaDoc comment associated with this method
	 */
	private getComment(method)
	{
		def ans = method.getComment()
		if (ans == null) return ""
		return ans
	}

    /**
     * Main entry point.
     */
    static void main(args)
    {
        def defaultGroovyMethodSource = new File("src/main/org/codehaus/groovy/runtime/DefaultGroovyMethods.java")
        def defaultGroovyStaticMethodSource = new File("src/main/org/codehaus/groovy/runtime/DefaultGroovyStaticMethods.java")
        def outFolder = new File("target/html/groovy-jdk")
        outFolder.mkdirs()

        def start = System.currentTimeMillis();

        def docGen = new DocGenerator([defaultGroovyMethodSource, defaultGroovyStaticMethodSource], outFolder)
        docGen.generateNew()

        def end = System.currentTimeMillis();

        println "Done. in ${end - start} millis"

    }
}
