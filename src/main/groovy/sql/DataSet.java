/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.sql;

import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.Statement;

/**
 * Represents an extent of objects
 * 
 * @author Chris Stevenson
 * @author Paul King
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class DataSet extends Sql {

    private Closure where;
    private DataSet parent;
    private String table;
    private SqlWhereVisitor visitor;
    private String sql;
    private List params;

    public DataSet(Sql sql, Class type) {
        super(sql);
        String table = type.getName();
        int idx = table.lastIndexOf('.');
        if (idx > 0) {
            table = table.substring(idx + 1);
        }
        this.table = table.toLowerCase();
    }

    public DataSet(Sql sql, String table) {
        super(sql);
        this.table = table;
    }

    public DataSet(DataSet parent, Closure where) {
        super(parent);
        this.table = parent.table;
        this.parent = parent;
        this.where = where;
    }

    public void add(Map values) throws SQLException {
        StringBuffer buffer = new StringBuffer("insert into ");
        buffer.append(table);
        buffer.append(" (");
        StringBuffer paramBuffer = new StringBuffer();
        boolean first = true;
        for (Iterator iter = values.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String column = entry.getKey().toString();
            if (first) {
                first = false;
                paramBuffer.append("?");
            }
            else {
                buffer.append(", ");
                paramBuffer.append(", ?");
            }
            buffer.append(column);
        }
        buffer.append(") values (");
        buffer.append(paramBuffer.toString());
        buffer.append(")");

        Connection connection = createConnection();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(buffer.toString());
            int i = 1;
            for (Iterator iter = values.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                setObject(statement, i++, entry.getValue());
            }
            int answer = statement.executeUpdate();
            if (answer != 1) {
                log.log(Level.WARNING, "Should have updated 1 row not " + answer + " when trying to add: " + values);
            }
        }
        catch (SQLException e) {
            log.log(Level.WARNING, "Failed to add row for: " + values, e);
            throw e;
        }
        finally {
            closeResources(connection, statement);
        }
    }

    public DataSet findAll(Closure where) {
        return new DataSet(this, where);
    }

    public void each(Closure closure) throws SQLException {
        eachRow(getSql(), getParameters(), closure);
    }

    public String getSql() {
        if (sql == null) {
            sql = "select * from " + table;
            if (where != null) {
                String clause = "";
                if (parent != null && parent.where != null) {
                    clause += parent.getSqlVisitor().getWhere() + " and ";
                }
                clause += getSqlVisitor().getWhere();
                if (clause.length() > 0) {
                    sql += " where " + clause;
                }
            }
        }
        return sql;
    }

    public List getParameters() {
        if (params == null) {
            params = new ArrayList();
            if (parent != null && parent.where != null) {
                params.addAll(parent.getParameters());
            }
            params.addAll(getSqlVisitor().getParameters());
        }
        return params;
    }

    protected SqlWhereVisitor getSqlVisitor() {
        if (visitor == null) {
            visitor = new SqlWhereVisitor();
            if (where != null) {
                ClassNode classNode = where.getMetaClass().getClassNode();
                if (classNode == null) {
                    throw new GroovyRuntimeException(
                        "Could not find the ClassNode for MetaClass: " + where.getMetaClass());
                }
                List methods = classNode.getDeclaredMethods("doCall");
                if (!methods.isEmpty()) {
                    MethodNode method = (MethodNode) methods.get(0);
                    if (method != null) {
                        Statement statement = method.getCode();
                        if (statement != null) {
                            statement.visit(visitor);
                        }
                    }
                }
            }
        }
        return visitor;
    }
    /*
     * create a subset of the original dataset
     */
    public DataSet createView(Closure criteria) {
    	return new DataSet(this, criteria);
    }
    
    /**
     * Returns a List of all of the rows from the table a DataSet
     * represents
     * @return  Returns a list of GroovyRowResult objects from the dataset
     * @throws SQLException if a database error occurs
     */
    public List rows() throws SQLException {
        return rows(getSql(), getParameters());
    }

    /**
     * Returns the first row from a DataSet's underlying table
     * 
     * @return  Returns the first GroovyRowResult object from the dataset
     * @throws SQLException if a database error occurs
     */
    public Object firstRow() throws SQLException{
        List rows = rows();
        if (rows.isEmpty()) return null;
        return(rows.get(0));
    }
}
