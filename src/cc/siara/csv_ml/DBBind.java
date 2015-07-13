/*
 * Copyright (C) 2015 Siara Logics (cc)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Arundale R.
 *
 */
package cc.siara.csv_ml;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import cc.siara.csv_ml.schema.Column;
import cc.siara.csv_ml.schema.MultiLevelCSVSchema;
import cc.siara.csv_ml.schema.Node;

/**
 * DBBind is a utility class that provides methods to generate DDL/DML for a
 * given csv_ml object and methods to retrieve information from DB in csv_ml
 * format.
 * 
 * This is a sample implementation and not intended as the only way to bind with
 * the database.
 * 
 * Also the syntax generated is specific to SQLite.
 * 
 * @author Arundale R.
 * @since 1.0
 */
public class DBBind {

    /**
     * This method generates DDL (CREATE TABLE etc.) for the given schema
     * object.
     * 
     * @param schema
     *            A schema object obtained by parsing csv_ml schema source
     * @param out_str
     *            A StringBuffer object to hold the generated DDL strings.
     * @since 1.0
     */
    public static void generateDDL(MultiLevelCSVSchema schema,
            StringBuffer out_str) {

        boolean is_first = true;
        boolean is_id_present = false;
        boolean is_parent_id_present = false;
        Hashtable<String, Node> node_map = schema.getNamePathNodeMap();
        // Generate a CREATE TABLE statement for each node
        // in the path-node map. Need not recurse as this map is flat
        for (String key : node_map.keySet()) {
            Node node = node_map.get(key);
            out_str.append("CREATE TABLE ");
            out_str.append(node.getName());
            out_str.append(" (");
            List<Column> columns_obj = node.getColumns();
            for (Column col_obj : columns_obj) {
                if (is_first)
                    is_first = false;
                else
                    out_str.append(", ");
                String col_name = col_obj.getName();
                String col_type = col_obj.getType();
                String col_default = col_obj.getColDefault();
                if (col_type.equals(""))
                    col_type = "text";
                String col_len = col_obj.getLen();
                out_str.append(col_name);
                out_str.append(" ");
                out_str.append(col_type);
                if (!col_len.equals("") && !col_len.equals("0"))
                    out_str.append("(").append(col_len).append(")");
                if (col_default == null) {
                    out_str.append(" NOT NULL");
                } else {
                    if (!col_default.equals("null"))
                        out_str.append(" DEFAULT '")
                                .append(col_default.replace("\'", "''"))
                                .append("'");
                }
                if (col_name.equals("id")) {
                    is_id_present = true;
                    out_str.append(" primary key autoincrement");
                }
                if (col_name.equals("parent_id"))
                    is_parent_id_present = true;
            }
            if (!is_id_present)
                out_str.append(", id integer primary key autoincrement");
            if (!is_parent_id_present && key.indexOf(".") != -1)
                out_str.append(", parent_id integer");
            out_str.append(");\r\n\r\n");
            is_first = true;
        }

    }

    /**
     * Generates DML (INSERT/UPDATE/DELETE) statements starting at the root
     * node. Automatically determines type of statement using the logic below:
     * 
     * <p>
     * If 'id' is empty or missing - generates INSERT statement
     * <p>
     * If 'id' is present - generates UPDATE statement
     * <p>
     * If 'id' starts with a * character - generates DELETE statement.
     * 
     * @param schema
     *            A schema object
     * @param ele
     *            Initially the method to be called with the root element of the
     *            Document object corresponding to the schema
     *            (doc.getDocumentElement()).
     * @param path
     *            Initially "" to be passed.
     * @param out_str
     *            The StringBuffer to which DML statements to be appended.
     */
    public static void generate_dml_recursively(MultiLevelCSVSchema schema,
            Element ele, String path, StringBuffer out_str) {

        // No DML generation for root element
        // Otherwise, this section generates INSERT/UPDATE/DELETE
        // for the current node
        if (!path.equals("")) {
            Node node = schema.getNodeByNamePath(path);
            List<Column> columns_obj = node.getColumns();
            String node_name = ele.getNodeName();
            String id_value = ele.getAttribute("id");
            if (id_value == null || id_value.equals("")) {
                out_str.append("INSERT INTO ");
                out_str.append(node_name);
                out_str.append(" (");
                boolean is_first = true;
                for (Column col_obj : columns_obj) {
                    String col_name = col_obj.getName();
                    if (col_name.equals("id"))
                        continue;
                    if (is_first)
                        is_first = false;
                    else
                        out_str.append(", ");
                    out_str.append(col_name);
                }
                int last_dot_idx = path.lastIndexOf('.');
                if (last_dot_idx != -1 && ele.getAttribute("parent_id") == null) {
                    out_str.append(", parent_id");
                }
                out_str.append(") VALUES (");
                is_first = true;
                for (Column col_obj : columns_obj) {
                    String col_name = col_obj.getName();
                    if (col_name.equals("id"))
                        continue;
                    if (is_first)
                        is_first = false;
                    else
                        out_str.append(", ");
                    out_str.append("'")
                            .append(ele.getAttribute(col_name).replace("\'",
                                    "''")).append("'");
                }
                if (last_dot_idx != -1 && ele.getAttribute("parent_id") == null) {
                    int earlier_dot_idx = path.lastIndexOf('.',
                            last_dot_idx - 1);
                    String parent_table_name = "";
                    if (earlier_dot_idx == -1)
                        parent_table_name = path.substring(0, last_dot_idx);
                    else
                        parent_table_name = path.substring(earlier_dot_idx + 1,
                                last_dot_idx);
                    out_str.append(", (select seq from sqlite_sequence where name='");
                    out_str.append(parent_table_name);
                    out_str.append("')");
                }
                out_str.append(");\r\n\r\n");
            } else {
                if (id_value.charAt(0) == '*') {
                    out_str.append("DELETE FROM ");
                    out_str.append(node_name);
                    id_value = id_value.substring(1);
                } else {
                    out_str.append("UPDATE ");
                    out_str.append(node_name);
                    out_str.append(" SET ");
                    boolean is_first = true;
                    for (Column col_obj : columns_obj) {
                        String col_name = col_obj.getName();
                        if (col_name.equals("id"))
                            continue;
                        if (is_first)
                            is_first = false;
                        else
                            out_str.append(", ");
                        out_str.append(col_name);
                        out_str.append(" = ");
                        out_str.append("'")
                                .append(ele.getAttribute(col_name).replace(
                                        "\'", "''")).append("'");
                    }
                }
                out_str.append(" WHERE id=");
                out_str.append(id_value);
                out_str.append(";\r\n\r\n");
            }
        }

        // Go through all children recursively
        NodeList childNodes = ele.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                continue;
            StringBuffer cur_path = new StringBuffer(path);
            if (cur_path.length() > 0)
                cur_path.append(".");
            cur_path.append(childNodes.item(i).getNodeName());
            generate_dml_recursively(schema, (Element) childNodes.item(i),
                    cur_path.toString(), out_str);
        }

    }

    /**
     * Generates SQL (SELECT statements) recursively, executes them against the
     * given Connection object and returns a csv_ml string in out_str.
     * 
     * @param schema
     *            A schema object
     * @param seq_path
     *            Should be "" intially. Used internally
     * @param prefix
     *            Should be "" intially. Used internally
     * @param out_str
     *            The StringBuffer to which csv_ml to be appended.
     * @param jdbcCon
     *            JDBC Connection instance to be used
     * @param id
     *            id value for the first level. This is an array because there
     *            could be more than one node in the first level.
     */
    public static void generateSQLRecursively(MultiLevelCSVSchema schema,
            String seq_path, String prefix, StringBuffer out_str,
            Connection jdbcCon, String[] id) {

        if (seq_path.length() == 0)
            seq_path = "1"; // Start with "1"
        else
            seq_path = seq_path.concat(".1"); // Check next level
        Node node = schema.getNodeBySeqPath(seq_path);
        if (node == null)
            return;

        while (node != null) {

            List<Column> columns_obj = node.getColumns();
            String node_name = node.getName();

            // Generate SQL Statement
            StringBuffer sql = new StringBuffer("SELECT ");
            boolean is_first = true;
            for (Column col_obj : columns_obj) {
                String col_name = col_obj.getName();
                if (is_first)
                    is_first = false;
                else
                    out_str.append(", ");
                out_str.append(col_name);
            }
            if (seq_path.indexOf('.') == -1)
                sql.append(", parent_id");
            sql.append(", id FROM ");
            sql.append(node_name);
            if (seq_path.indexOf('.') == -1) {
                sql.append(" WHERE id='");
                sql.append(id[Integer.parseInt(seq_path)-1]);
            } else {
                sql.append(" WHERE parent_id='");
                sql.append(id[0]);
            }
            sql.append("'");

            // Run the SQL and generate record
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = jdbcCon.createStatement();
                rs = stmt.executeQuery(sql.toString());
                out_str.append(prefix);
                out_str.append(node_name);
                while (rs.next()) {
                    for (Column col : node.getColumns()) {
                        String col_alias = col.getAlias();
                        if (col_alias == null)
                            col_alias = col.getName();
                        out_str.append(',');
                        out_str.append(Util.encodeToCSVText(rs
                                .getString(col_alias)));
                    }
                    out_str.append("\n");
                    // Recursively generate data for children
                    generateSQLRecursively(schema, seq_path,
                            prefix.concat(" "), out_str, jdbcCon,
                            new String[] { rs.getString("id") });
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            } finally {
                try {
                    if (rs != null)
                        rs.close();
                    if (stmt != null)
                        stmt.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Repeat for a sibling
        int seqIdx = seq_path.lastIndexOf('.');
        if (seqIdx == -1)
            seq_path = String.valueOf(Integer.parseInt(seq_path) + 1);
        else {
            seq_path = seq_path.substring(0, seqIdx).concat(
                    String.valueOf(Integer.parseInt(seq_path
                            .substring(seqIdx + 1) + 1)));
        }
        node = schema.getNodeBySeqPath(seq_path);

    }

    /**
     * Generates SQL (SELECT statements), executes them against the given
     * Connection object and returns a csv_ml string in out_str.
     * 
     * @param schema
     *            A schema object
     * @param seq_path
     *            Should be "" intially. Used internally
     * @param prefix
     *            Should be "" intially. Used internally
     * @param out_str
     *            The StringBuffer to which csv_ml to be appended.
     * @param jdbcCon
     *            JDBC Connection instance to be used
     * @param id
     *            id value for the first level. This is an array because there
     *            could be more than one node in the first level.
     */
    public static void generateSQL(MultiLevelCSVSchema schema,
            StringBuffer out_str, Connection jdbcCon, String[] id) {
        out_str.append("csv_ml,1.0\n");
        schema.outputSchemaRecursively(out_str, "", "", true);
        generateSQLRecursively(schema, "", "", out_str, jdbcCon, id);
    }

    /**
     * Runs DDL/DML statements separated by ; character
     * 
     * @param statements
     *            String containing statements separated by ;
     * @param jdbcCon
     *            Connection instance to use
     * @return String containing the statements executed and corresponding
     *         results.
     */
    public static String runStatements(String statements, Connection jdbcCon) {
        StringBuffer out_str = new StringBuffer();
        StringBuffer stmt_str = new StringBuffer();
        Statement stmt = null;
        try {
            stmt = jdbcCon.createStatement();
            int stmt_pos = 0;
            do {

                // extract single statement
                boolean is_stmt_started = false;
                boolean is_within_quote = false;
                for (; stmt_pos < statements.length(); stmt_pos++) {
                    char c = statements.charAt(stmt_pos);
                    if (Character.isWhitespace(c) && !is_stmt_started)
                        continue;
                    is_stmt_started = true;
                    if (c == ';' && !is_within_quote) {
                        stmt_pos++;
                        break;
                    }
                    stmt_str.append(c);
                    if (c == '\'') {
                        if (is_within_quote) {
                            if (stmt_pos + 1 < statements.length()
                                    && statements.charAt(stmt_pos + 1) == '\'') {
                                stmt_pos++;
                            } else
                                is_within_quote = false;
                        } else
                            is_within_quote = true;
                    }
                }
                if (stmt_str.length() == 0)
                    continue;
                // Execute and append the result
                out_str.append(stmt_str).append("\n");
                try {
                    int result = stmt.executeUpdate(stmt_str.toString());
                    out_str.append("Success: ").append(result)
                            .append(" records affected.\n\n");
                } catch (SQLException e) {
                    out_str.append("Error:").append(e.getMessage())
                            .append("\n\n");
                }
                stmt_str.setLength(0);
            } while (stmt_pos < statements.length());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return out_str.toString();

    }

}
