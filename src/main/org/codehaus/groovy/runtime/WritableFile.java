/*
 * $Id$
 *
 * Copyright 2003 (C) John Wilson. All Rights Reserved.
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided that the
 * following conditions are met:
 *  1. Redistributions of source code must retain copyright statements and
 * notices. Redistributions must also contain a copy of this document.
 *  2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  3. The name "groovy" must not be used to endorse or promote products
 * derived from this Software without prior written permission of The Codehaus.
 * For written permission, please contact info@codehaus.org.
 *  4. Products derived from this Software may not be called "groovy" nor may
 * "groovy" appear in their names without prior written permission of The
 * Codehaus. "groovy" is a registered trademark of The Codehaus.
 *  5. Due credit should be given to The Codehaus - http://groovy.codehaus.org/
 *
 * THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 */

package org.codehaus.groovy.runtime;

import groovy.lang.Writable;

import java.io.File;
import java.io.Writer;
import java.io.IOException;
import java.io.Reader;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * A Writable File.
 *
 * @author John Wilson
 *
 */
public class WritableFile extends File implements Writable {
    private final File delegate;
    private final String encoding;

    public WritableFile(File delegate) {
        this(delegate, null);
    }

    public WritableFile(File delegate, String encoding) {
        super("");
        this.delegate = delegate;
        this.encoding = encoding;
    }

    public Writer writeTo(Writer out) throws IOException {
        final Reader reader =
            (this.encoding == null)
                ? DefaultGroovyMethods.newReader(this.delegate)
                : DefaultGroovyMethods.newReader(this.delegate, this.encoding);

        try {
            int c = reader.read();

            while (c != -1) {
                out.write(c);
                c = reader.read();
            }
        }
        finally {
            reader.close();
        }
        return out;
    }

    public boolean canRead() {
        return delegate.canRead();
    }

    public boolean canWrite() {
        return delegate.canWrite();
    }

    public int compareTo(File arg0) {
        return delegate.compareTo(arg0);
    }

    public boolean createNewFile() throws IOException {
        return delegate.createNewFile();
    }

    public boolean delete() {
        return delegate.delete();
    }

    public void deleteOnExit() {
        delegate.deleteOnExit();
    }

    public boolean equals(Object arg0) {
        return delegate.equals(arg0);
    }

    public boolean exists() {
        return delegate.exists();
    }

    public File getAbsoluteFile() {
        return delegate.getAbsoluteFile();
    }

    public String getAbsolutePath() {
        return delegate.getAbsolutePath();
    }

    public File getCanonicalFile() throws IOException {
        return delegate.getCanonicalFile();
    }

    public String getCanonicalPath() throws IOException {
        return delegate.getCanonicalPath();
    }

    public String getName() {
        return delegate.getName();
    }

    public String getParent() {
        return delegate.getParent();
    }

    public File getParentFile() {
        return delegate.getParentFile();
    }

    public String getPath() {
        return delegate.getPath();
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean isAbsolute() {
        return delegate.isAbsolute();
    }

    public boolean isDirectory() {
        return delegate.isDirectory();
    }

    public boolean isFile() {
        return delegate.isFile();
    }

    /* (non-Javadoc)
     * @see java.io.File#isHidden()
     */
    public boolean isHidden() {
        return delegate.isHidden();
    }

    /* (non-Javadoc)
     * @see java.io.File#lastModified()
     */
    public long lastModified() {
        return delegate.lastModified();
    }

    /* (non-Javadoc)
     * @see java.io.File#length()
     */
    public long length() {
        return delegate.length();
    }

    /* (non-Javadoc)
     * @see java.io.File#list()
     */
    public String[] list() {
        return delegate.list();
    }

    /* (non-Javadoc)
     * @see java.io.File#list(java.io.FilenameFilter)
     */
    public String[] list(FilenameFilter arg0) {
        return delegate.list(arg0);
    }

    /* (non-Javadoc)
     * @see java.io.File#listFiles()
     */
    public File[] listFiles() {
        return delegate.listFiles();
    }

    /* (non-Javadoc)
     * @see java.io.File#listFiles(java.io.FileFilter)
     */
    public File[] listFiles(FileFilter arg0) {
        return delegate.listFiles(arg0);
    }

    /* (non-Javadoc)
     * @see java.io.File#listFiles(java.io.FilenameFilter)
     */
    public File[] listFiles(FilenameFilter arg0) {
        return delegate.listFiles(arg0);
    }

    /* (non-Javadoc)
     * @see java.io.File#mkdir()
     */
    public boolean mkdir() {
        return delegate.mkdir();
    }

    /* (non-Javadoc)
     * @see java.io.File#mkdirs()
     */
    public boolean mkdirs() {
        return delegate.mkdirs();
    }

    /* (non-Javadoc)
     * @see java.io.File#renameTo(java.io.File)
     */
    public boolean renameTo(File arg0) {
        return delegate.renameTo(arg0);
    }

    /* (non-Javadoc)
     * @see java.io.File#setLastModified(long)
     */
    public boolean setLastModified(long arg0) {
        return delegate.setLastModified(arg0);
    }

    /* (non-Javadoc)
     * @see java.io.File#setReadOnly()
     */
    public boolean setReadOnly() {
        return delegate.setReadOnly();
    }

    /* (non-Javadoc)
     * @see java.io.File#toString()
     */
    public String toString() {
        return delegate.toString();
    }

    /* (non-Javadoc)
     * @see java.io.File#toURI()
     */
    public URI toURI() {
        return delegate.toURI();
    }

    /* (non-Javadoc)
     * @see java.io.File#toURL()
     */
    public URL toURL() throws MalformedURLException {
        return delegate.toURL();
    }

}
