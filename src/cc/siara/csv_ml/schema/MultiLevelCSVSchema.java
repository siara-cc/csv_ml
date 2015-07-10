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
package cc.siara.csv_ml.schema;

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

import cc.siara.csv.CSVParser;
import cc.siara.csv.ExceptionHandler;
import cc.siara.csv_ml.MultiLevelCSVParser;
import cc.siara.csv_ml.Util;

/**
 * Represents schema of a Multi-Level CSV structure
 * 
 * @author Arundale R.
 */
public class MultiLevelCSVSchema {

    // Members
    private Hashtable<String, Node> name_path_node_map = new Hashtable<String, Node>();
    private Hashtable<String, Node> seq_path_node_map = new Hashtable<String, Node>();

    /**
     * Getter for map between name path and node
     * 
     * @return map
     */
    public Hashtable<String, Node> getNamePathNodeMap() {
        return name_path_node_map;
    }

    /**
     * Adds a mapping between name path and node
     * 
     * @param path
     *            Path to node in the form 'node1.node2...'
     * @param node
     *            Node to add
     */
    public void addNamePathNodeMap(String path, Node node) {
        this.name_path_node_map.put(path, node);
    }

    /**
     * Getter for map between Sequence path and node
     * 
     * @return map
     */
    public Hashtable<String, Node> getSeqPathNodeMap() {
        return seq_path_node_map;
    }

    /**
     * Adds a mapping between Sequence path and node
     * 
     * @param seq_path
     *            Path to node in the form '1.2.4...'
     * @param node
     *            Node to add
     */
    public void addSeqPathNodeMap(String seq_path, Node node) {
        this.seq_path_node_map.put(seq_path, node);
    }

    /**
     * Gets corresponding Node for given sequence path
     * 
     * @param seq_path
     * @return corresponding node
     */
    public Node getNodeBySeqPath(String seq_path) {
        return seq_path_node_map.get(seq_path);
    }

    /**
     * Gets corresponding Node for given name path
     * 
     * @param seq_path
     * @return corresponding node
     */
    public Node getNodeByNamePath(String name_path) {
        return name_path_node_map.get(name_path);
    }

    /**
     * Parse the schema from the given java.io.Reader (r) and build the members
     * of this object.
     * 
     * @param csv_parser
     *            CSV Parser to use
     * @param csv_ml_node_name
     *            Whether to look for node name or not
     * @param csv_ml_schema
     *            Whether schema present or not. If not, schema is generated
     * @param ex
     *            The Exception Handler to use
     * @param r
     *            Reader to be used
     * @throws IOException
     */
    public void parseSchema(CSVParser csv_parser, String csv_ml_node_name,
            String csv_ml_schema, ExceptionHandler ex, Reader r)
            throws IOException {

        StringBuffer cur_path = new StringBuffer();
        StringBuffer cur_sequence_path = new StringBuffer();
        int cur_level = 0;
        int cur_sibling = 1;
        int token_ctr = 0;
        boolean to_continue = true;
        boolean is0def = false;
        boolean is0ref = false;
        Node prev_node_obj = null;
        int node_ctr = 1;
        String zero_def_name = "";

        int nxt_char = csv_parser.readChar(r);
        if (nxt_char == ' ' || nxt_char == '\t') {
            ex.set_err(MultiLevelCSVParser.E_SCH_START_WITH_SPACE);
            return;
        } else
            csv_parser.reInsertLastChar();

        do {

            String value = csv_parser.parseNextToken(r);
            boolean is_eol = csv_parser.isEOL();

            // Locate re-usable node defainitions
            if (value.length() > 0 && value.charAt(0) == '0' && token_ctr == 0) {
                if (cur_level == 0) {
                    is0def = true;
                    zero_def_name = value.trim();
                    token_ctr--;
                }
                // If level more than 0, this is just possible children to
                // re-usable nodes
                if (cur_level > 0)
                    is0ref = true;
            }

            if (token_ctr == 0 && !is0ref) { // First column - refers to node
                                             // name
                String node_name = value.trim();
                if (node_name.indexOf(' ') != -1)
                    node_name = node_name.replace(' ', '_');
                if (node_name.equals("") && is_eol) {
                    continue;
                }
                if (csv_ml_node_name.equals("no_node_name")) {
                    node_name = "n" + node_ctr; // Generate node name
                    node_ctr++;
                }
                if (cur_level == 0) {
                    // End of schema definition, so return
                    if (node_name.equals("end_schema"))
                        return;
                    // Data has started. return after putting it back
                    char c = node_name.charAt(0);
                    if (c >= '1' && c <= '9') {
                        csv_parser.reinsertLastToken();
                        return;
                    }
                }
                if (is0def) { // Re-usable node definition
                    cur_sequence_path.replace(0, cur_sequence_path.length(),
                            zero_def_name);
                    cur_path.replace(0, cur_path.length(), zero_def_name);
                } else { // Normal node definition. Form sequence and name paths
                    Util.addToSeqPath(cur_sequence_path, cur_sibling);
                    Util.addToPath(cur_path, node_name);
                }

                // Create node schema object and set members
                Node node_obj = new Node();
                prev_node_obj = node_obj;
                node_obj.setName(node_name);
                node_obj.setPath(cur_path.toString());
                node_obj.setSeqPath(cur_sequence_path.toString());
                if (getNodeByNamePath(cur_path.toString()) != null) {
                    ex.set_err(MultiLevelCSVParser.E_DUPLICATE_NODE);
                    return;
                }
                addNamePathNodeMap(cur_path.toString(), node_obj);
                addSeqPathNodeMap(cur_sequence_path.toString(), node_obj);
                if (csv_ml_node_name.equals("no_node_name")) {
                    Column column_obj = new Column();
                    node_obj.addColumn(column_obj);
                    column_obj.parseColumnSchema(value);
                }
            } else if (token_ctr > 0 && !is0ref) {
                // Remaining are column definitions
                Column column_obj = new Column();
                prev_node_obj.addColumn(column_obj);
                column_obj.parseColumnSchema(value);
            }

            if (is0ref) {
                // define child of re-usable node
                prev_node_obj.addZeroChild(value.trim());
            }

            token_ctr++;
            if (is_eol) { // End of line

                // Adjust level by counting space at the begining of next line
                int space_count = 0;
                nxt_char = csv_parser.readChar(r);
                while (nxt_char == ' ' || nxt_char == '\t') {
                    space_count++;
                    nxt_char = csv_parser.readChar(r);
                }
                if (nxt_char == -1) // end of file
                    return;
                else // Not a space or tab. Put it back.
                    csv_parser.reInsertLastChar();
                csv_parser.getCounter().setColNo(
                        csv_parser.getCounter().getColNo() + space_count);
                if (space_count > (cur_level + 1)) {
                    ex.set_err(MultiLevelCSVParser.E_DOWN_2_LEVELS);
                    return;
                } else {
                    // Adjust current level and paths
                    while (cur_level >= space_count) {
                        if (!is0def && !is0ref) {
                            Util.removeFromPath(cur_path);
                            cur_sibling = Util.removeFromSeqPath(
                                    cur_sequence_path, csv_ml_schema,
                                    csv_ml_node_name, cur_sibling);
                        }
                        cur_level--;
                    }
                    cur_level++;
                }

                if (csv_ml_node_name.equals("no_node_name") && space_count == 0)
                    return;
                if (is0def || is0ref) {
                    cur_path.setLength(0);
                    cur_sequence_path.setLength(0);
                }
                is0def = false;
                is0ref = false;
                token_ctr = 0;
            }
        } while (to_continue);

    }

}
