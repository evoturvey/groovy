/*
 $Id$

 Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.

 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.

 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.

 3. The name "groovy" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Codehaus.  For written permission,
    please contact info@codehaus.org.

 4. Products derived from this Software may not be called "groovy"
    nor may "groovy" appear in their names without prior written
    permission of The Codehaus. "groovy" is a registered
    trademark of The Codehaus.

 5. Due credit should be given to The Codehaus -
    http://groovy.codehaus.org/

 THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package org.codehaus.groovy.control;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.classgen.ClassGenerator;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.control.io.InputStreamReaderSource;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.tools.GroovyClass;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.DumpClassVisitor;



/**
 *  Collects all compilation data as it is generated by the compiler system.
 *  Allows additional source units to be added and compilation run again (to
 *  affect only the deltas).
 *
 *  @author <a href="mailto:cpoirier@dreaming.org">Chris Poirier</a>
 *
 *  @version $Id$
 */

public class CompilationUnit extends ProcessingUnit
{
    
  //---------------------------------------------------------------------------
  // CONSTRUCTION AND SUCH
    
    protected HashMap     sources;    // The SourceUnits from which this unit is built
    protected ArrayList   names;      // Names for each SourceUnit in sources.
    
    protected CompileUnit ast;        // The overall AST for this CompilationUnit.
    protected ArrayList   classes;    // The classes generated during classgen.
    
    protected Verifier    verifier;   // For use by verify().
    
    protected boolean     debug;      // Controls behaviour of classgen() and other routines.
    protected boolean     configured; // Set true after the first configure() operation
    
    protected ClassgenCallback classgenCallback;  // A callback for use during classgen()

    

   /**
    *  Initializes the CompilationUnit with defaults.
    */
    public CompilationUnit( )
    {
        this( null, null, null );
    }
    
    

   /**
    *  Initializes the CompilationUnit with defaults except for class loader.
    */
    public CompilationUnit( ClassLoader loader )
    {
        this( null, null, loader );
    }
    


   /**
    *  Initializes the CompilationUnit with no security considerations.
    */
    public CompilationUnit( CompilerConfiguration configuration )
    {
        this( configuration, null, null );
    }
    
    
       
   /**
    *  Initializes the CompilationUnit with a CodeSource for controlling
    *  security stuff and a class loader for loading classes.
    */
    
    public CompilationUnit( CompilerConfiguration configuration, CodeSource security, ClassLoader loader )
    {
        super( configuration, loader );

        this.names    = new ArrayList();
        this.sources  = new HashMap();
        
        this.ast      = new CompileUnit( this.classLoader, security, this.configuration );
        this.classes  = new ArrayList();
        
        this.verifier = new Verifier();
        
        this.classgenCallback = null;
    }
    
    
  
   /**
    *  Reconfigures the CompilationUnit.
    */
    
    public void configure( CompilerConfiguration configuration )
    {
        super.configure( configuration );
        this.debug = configuration.getDebug();
        
        
        //
        // Configure our class loader's classpath, if it is of
        // a configurable type.  We can't reconfigure it, 
        // unfortunately, due to limitations in URLClassLoader.
        
        if( !this.configured && this.classLoader instanceof CompilerClassLoader )
        {
            CompilerClassLoader loader = (CompilerClassLoader)this.classLoader;
            
            Iterator iterator = configuration.getClasspath().iterator();
            while( iterator.hasNext() )
            {
                try
                {
                    this.configured = true;
                    loader.addPath( (String)iterator.next() );
                }
                catch( MalformedURLException e )
                {   
                    throw new ConfigurationException( e );
                }
            }
        }
    }
    
    
   
   /**
    *  Returns the CompileUnit that roots our AST.
    */
    
    public CompileUnit getAST()
    {
        return this.ast;
    }
    
    

   /**
    *  Get the GroovyClasses generated by compile().
    */
    
    public List getClasses()
    {
        return classes;
    }
    
    
    
   /**
    *  Convenience routine to get the first ClassNode, for
    *  when you are sure there is only one. 
    */
    
    public ClassNode getFirstClassNode()
    {
        return (ClassNode)((ModuleNode)this.ast.getModules().get(0)).getClasses().get(0);
    }
    
    
    
   /**
    *  Convenience routine to get the named ClassNode.
    */
    
    public ClassNode getClassNode( final String name )
    {
        final ClassNode[] result = new ClassNode[] { null };
        LoopBodyForPrimaryClassNodeOperations handler = new LoopBodyForPrimaryClassNodeOperations() 
        {
            public void call( SourceUnit source, GeneratorContext context, ClassNode classNode )
            {
                if( classNode.getName().equals(name) )
                {
                    result[0] = classNode;
                }
            }
        };
        
        try { applyToPrimaryClassNodes( handler ); } catch( CompilationFailedException e ) { }
        return result[0];
    }
    
    
    
    

    
  //---------------------------------------------------------------------------
  // SOURCE CREATION

   /**
    *  Adds a set of file paths to the unit.
    */
    
    public void addSources( String[] paths )
    {
        for( int i = 0; i < paths.length; i++ )
        {
            File file = new File( paths[i] );
            addSource( file );
        }
    }
    
    
   /**
    *  Adds a set of source files to the unit.
    */
    
    public void addSources( File[] files )
    {
        for( int i = 0; i < files.length; i++ ) 
        {
            addSource( files[i] );
        }
    }
    
    
    
   /**
    *  Adds a source file to the unit.
    */
    
    public SourceUnit addSource( File file )
    {
        return addSource( new SourceUnit(file, configuration, classLoader) );
    }
    
    
    
    /**
     *  Adds a source file to the unit.
     */
     
     public SourceUnit addSource( URL url )
     {
         return addSource( new SourceUnit(url, configuration, classLoader) );
     }
     
     
     
   /**
    *  Adds a InputStream source to the unit.
    */
    
    public SourceUnit addSource( String name, InputStream stream )
    {
        ReaderSource source = new InputStreamReaderSource( stream, configuration );
        return addSource( new SourceUnit(name, source, configuration, classLoader) );
    } 
    
    

   /**
    *  Adds a SourceUnit to the unit.
    */
    
    public SourceUnit addSource( SourceUnit source )
    {
        String name = source.getName();
        
        source.setClassLoader( this.classLoader );

        names.add( name );
        sources.put( name, source );
        
        return source;
    }
    
    
    
   /**
    *  Returns an iterator on the unit's SourceUnits.
    */
    
    public Iterator iterator()
    {
        return new Iterator() {
            Iterator nameIterator = names.iterator();
            
            public boolean hasNext() { return nameIterator.hasNext(); }
            
            public Object next() { String name = (String)nameIterator.next(); return sources.get(name); }
            
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }
    
    
    
   /**
    *  Adds a ClassNode directly to the unit (ie. without source).
    *  Used primarily for testing support.
    */
    
    public void addClassNode( ClassNode node )
    {
        ModuleNode module = new ModuleNode( this.ast );
        this.ast.addModule( module );
        module.addClass( node );
    }

    

    
  //---------------------------------------------------------------------------
  // EXTERNAL CALLBACKS
      
   /**
    *  A callback interface you can use to "accompany" the classgen()
    *  code as it traverses the ClassNode tree.  You will be called-back
    *  for each primary and inner class.  Use setClassgenCallback() before
    *  running compile() to set your callback. 
    */
     
    public static abstract class ClassgenCallback
    {
        public abstract void call( ClassVisitor writer, ClassNode node ) throws CompilationFailedException;
    }
    
    
    
   /**
    *  Sets a ClassgenCallback.  You can have only one, and setting
    *  it to null removes any existing setting.
    */
    
    public void setClassgenCallback( ClassgenCallback visitor )
    {
        this.classgenCallback = visitor;
    }
    
    
    

  //---------------------------------------------------------------------------
  // ACTIONS
    

   /**
    *  Synonym for compile(Phases.ALL).
    */
    
    public void compile( ) throws CompilationFailedException
    {
        compile( Phases.ALL );
    }
   
    

   /**
    *  Compiles the compilation unit from sources.
    */
    
    public void compile( int throughPhase ) throws CompilationFailedException
    {
        //
        // To support delta compilations, we always restart 
        // the compiler.  The individual passes are responsible
        // for not reprocessing old code.
        
        gotoPhase( Phases.INITIALIZATION );
        
        do
        {        
            if( throughPhase < Phases.PARSING ) { break; }
            
            gotoPhase( Phases.PARSING );
            parse();

            if( throughPhase < Phases.CONVERSION ) { break; }
            
            gotoPhase( Phases.CONVERSION );
            convert();

            if( throughPhase < Phases.CLASS_GENERATION ) { break; }
            
            gotoPhase( Phases.CLASS_GENERATION );
            classgen();
        
            if( throughPhase < Phases.OUTPUT ) { break; }

            gotoPhase( Phases.OUTPUT );
            output();

            if( throughPhase < Phases.FINALIZATION ) { break; }
            
            gotoPhase( Phases.FINALIZATION );
            
        } while( false );
        
    }
    
    
   
   /**
    *  Parses all sources.
    */
    
    public void parse() throws CompilationFailedException
    {
        if( this.phase != Phases.PARSING )
        {
            throw new GroovyBugError( "CompilationUnit not read for parse()" );
        }
        
        applyToSourceUnits( parse );
        
        completePhase();
        applyToSourceUnits( mark );
    }


   /**
    *  Runs parse() on a single SourceUnit.
    */
     
    private LoopBodyForSourceUnitOperations parse = new LoopBodyForSourceUnitOperations() 
    { 
        public void call( SourceUnit source ) throws CompilationFailedException 
        { 
            source.parse();    
        } 
    };

    
     
   
   /**
    *  Builds ASTs for all parsed sources. 
    */
     
    public void convert() throws CompilationFailedException
    {
        if( this.phase != Phases.CONVERSION )
        {
            throw new GroovyBugError( "CompilationUnit not ready for convert()" );
        }
        
        applyToSourceUnits( convert );

        completePhase();
        applyToSourceUnits( mark );
    }


   /**
    *  Runs convert() on a single SourceUnit.
    */
     
    private LoopBodyForSourceUnitOperations convert = new LoopBodyForSourceUnitOperations() 
    { 
        public void call( SourceUnit source ) throws CompilationFailedException 
        { 
            source.convert();
            /* this. */ast.addModule( source.getAST() );
        } 
    };

     
     
    
   /**
    *  Expands and canonicalizes the ASTs generated during
    *  parsing and conversion, then generates classes.
    */
    
    public void classgen() throws CompilationFailedException
    {
        if( this.phase != Phases.CLASS_GENERATION )
        {
            throw new GroovyBugError( "CompilationUnit not ready for classgen()" );
        }
        
        applyToPrimaryClassNodes( classgen );

        completePhase();
        applyToSourceUnits( mark );
    }
    
    
    
   /**
    *  Runs classgen() on a single ClassNode.
    */
      
    private LoopBodyForPrimaryClassNodeOperations classgen = new LoopBodyForPrimaryClassNodeOperations() 
    { 
        public void call( SourceUnit source, GeneratorContext context, ClassNode classNode ) throws CompilationFailedException 
        {
            //
            // Run the Verifier on the outer class
      
            verifier.visitClass( classNode );

             
            //
            // Prep the generator machinery
             
            ClassVisitor visitor = null;
            if( debug ) 
            { 
                visitor = new DumpClassVisitor(output); 
            } 
            else  
            { 
                visitor = new ClassWriter(true); 
            }

            String sourceName = (source == null ? classNode.getModule().getDescription() : source.getName() );
            ClassGenerator generator = new ClassGenerator( context, visitor, classLoader, sourceName );
             
             
            // 
            // Run the generation and create the class (if required)

            generator.visitClass( classNode );
             
            if( !debug )
            {
                byte[] bytes = ((ClassWriter)visitor).toByteArray();
                /* this. */classes.add( new GroovyClass(classNode.getName(), bytes) );
            }
            
            
            //
            // Handle any callback that's been set
            
            if( /* this. */ classgenCallback != null )
            {
                if( debug )
                {
                    try
                    {
                        classgenCallback.call( visitor, classNode );
                    }
                    catch( Throwable t )
                    {
                        output.println( "Classgen callback threw: " + t );
                        t.printStackTrace( output );
                    }
                }
                else
                {
                    classgenCallback.call( visitor, classNode );
                }
            }
             

            //
            // Recurse for inner classes

            LinkedList innerClasses = generator.getInnerClasses();
            while( !innerClasses.isEmpty() ) 
            {
                classgen.call( source, context, (ClassNode)innerClasses.removeFirst() );
            }
        } 
    };
    
    
    



   /**
    *  Outputs the generated class files to permanent storage.
    */
    
    public void output() throws CompilationFailedException
    {
        if( this.phase != Phases.OUTPUT && !(this.phase == Phases.CLASS_GENERATION && this.phaseComplete) )
        {
            throw new GroovyBugError( "CompilationUnit not ready for output()" );
        }
        
        boolean failures = false;
        
        Iterator iterator = this.classes.iterator();
        while( iterator.hasNext() )
        {
            //
            // Get the class and calculate its filesystem name
            
            GroovyClass gclass = (GroovyClass)iterator.next();
            String name = gclass.getName().replace('.', File.separatorChar) + ".class";
            File   path = new File( configuration.getTargetDirectory(), name );
            
            //
            // Ensure the path is ready for the file
            
            File directory = path.getParentFile();
            if( directory != null && !directory.exists() ) 
            {
                directory.mkdirs();
            }

            //
            // Create the file and write out the data
            
            byte[] bytes = gclass.getBytes();
            
            FileOutputStream stream = null;
            try 
            {
                stream = new FileOutputStream( path );
                stream.write( bytes, 0, bytes.length );
            }
            catch( IOException e )
            {
                addError( Message.create(e.getMessage()) );
                failures = true;
            }
            finally 
            {
                if( stream != null )
                {
                    try { stream.close(); } catch( Exception e ) { }
                }
            }
        }
        
        if( failures )
        {
            fail();
        }
        
        completePhase();
        applyToSourceUnits( mark );
    }
    
    
    
   /**
    *  Returns true if there are any errors pending.
    */
     
    public boolean hasErrors()
    {
        boolean hasErrors = false;
        
        Iterator keys = names.iterator();
        while( keys.hasNext() )
        {
            String     name   = (String)keys.next();
            SourceUnit source = (SourceUnit)sources.get( name );
            
            if( source.hasErrors() )
            {
                hasErrors = true;
                break;
            }
        }
        
        return hasErrors || super.hasErrors();
    }


    
    
  //---------------------------------------------------------------------------
  // PHASE HANDLING
  
    
   /**
    *  Updates the phase marker on all sources.
    */
    
    protected void mark() throws CompilationFailedException
    {
        applyToSourceUnits( mark );
    }
    
    
   /**
    *  Marks a single SourceUnit with the current phase,
    *  if it isn't already there yet.
    */
    
    private LoopBodyForSourceUnitOperations mark = new LoopBodyForSourceUnitOperations()
    {
        public void call( SourceUnit source ) throws CompilationFailedException
        {
            if( source.phase < phase )
            {
                source.gotoPhase( phase );
            }
            
            if( source.phase == phase && phaseComplete && !source.phaseComplete )
            {
                source.completePhase();
            }
        }
    };
    
    
    
     
  //---------------------------------------------------------------------------
  // LOOP SIMPLIFICATION FOR SourceUnit OPERATIONS
    
   /**
    *  An callback interface for use in the applyToSourceUnits loop driver.
    */
     
    public abstract class LoopBodyForSourceUnitOperations
    {
        public abstract void call( SourceUnit source ) throws CompilationFailedException;
    }

     
     
   /**
    *  A loop driver for applying operations to all SourceUnits.
    *  Automatically skips units that have already been processed
    *  through the current phase.
    */
    
    public void applyToSourceUnits( LoopBodyForSourceUnitOperations body ) throws CompilationFailedException
    {
        boolean failures = false;
        
        Iterator keys = names.iterator();
        while( keys.hasNext() )
        {
            String     name   = (String)keys.next();
            SourceUnit source = (SourceUnit)sources.get( name );
            if( source.phase <= phase )
            {
                try
                {
                    body.call( source );
                }
                catch( CompilationFailedException e )
                {
                    failures = true;
                }
                catch( Exception e )
                {
                    throw new GroovyBugError( e );
                }
            }
        }
        
        if( failures )
        {
            fail();
        }
    }
    
    

    
  //---------------------------------------------------------------------------
  // LOOP SIMPLIFICATION FOR PRIMARY ClassNode OPERATIONS

    
   /**
    *  An callback interface for use in the applyToSourceUnits loop driver.
    */
      
    public abstract class LoopBodyForPrimaryClassNodeOperations
    {
        public abstract void call( SourceUnit source, GeneratorContext context, ClassNode classNode ) throws CompilationFailedException;
    }
     
     
   /**
    *  A loop driver for applying operations to all primary ClassNodes in
    *  our AST.  Automatically skips units that have already been processed
    *  through the current phase.
    */

    public void applyToPrimaryClassNodes( LoopBodyForPrimaryClassNodeOperations body ) throws CompilationFailedException
    {
        boolean failures = false;
    
        Iterator modules = this.ast.getModules().iterator();
        while( modules.hasNext() )
        {
            ModuleNode module = (ModuleNode)modules.next();
            
            try 
            {
                Iterator classNodes = module.getClasses().iterator();
                while( classNodes.hasNext() ) 
                {
                    ClassNode classNode = (ClassNode)classNodes.next();
                    SourceUnit  context = module.getContext();
                    if( context == null || context.phase <= phase )
                    {
                        body.call( module.getContext(), new GeneratorContext(this.ast), classNode );
                    }
                }
            }
            catch( CompilationFailedException e )
            {
                failures = true;
            }
            catch( Exception e )
            {
                failures = true;
                addError( new ExceptionMessage(e) );
            }
        }
        
        if( failures )
        {
            fail();
        }
    }

    
    

  //---------------------------------------------------------------------------
  // OUTPUT
      
    
   /**
    *  Writes error messages to the specified PrintWriter.
    */
    
    public void write( PrintWriter writer, Janitor janitor )
    {
        super.write( writer, janitor );
        
        Iterator keys = names.iterator();
        while( keys.hasNext() )
        {
            String     name   = (String)keys.next();
            SourceUnit source = (SourceUnit)sources.get( name );
   
            if( source.hasErrors() )
            {
                source.write( writer, janitor );
            }
        }
        
    }
    

}




