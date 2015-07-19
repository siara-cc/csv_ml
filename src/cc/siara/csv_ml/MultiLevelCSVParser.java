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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cc.siara.csv.CSVParser;
import cc.siara.csv.Counter;
import cc.siara.csv.ExceptionHandler;
import cc.siara.csv_ml.schema.Column;
import cc.siara.csv_ml.schema.MultiLevelCSVSchema;
import cc.siara.csv_ml.schema.Node;

/**
 * MultiLevelCSVParser parses csv_ml format files into corresponding JSON or W3C
 * DOM objects.
 * 
 * @author Arundale R.
 * @since 1.0
 */
public class MultiLevelCSVParser {

    // Parts of csv_ml directive
    String csv_ml_ver = "1.0";
    String csv_ml_encoding = "UTF-8"; // as per java standard
    String csv_ml_root = "root"; // root node name
    String csv_ml_node_name = "no_node_name"; // other possible value -
                                              // with_node_name
    String csv_ml_schema = "inline"; // other possible values - inline,
                                     // external. If external, next field
                                     // should contain valid path to file
    String csv_ml_schema_file = ""; // path to schema file

    // Associated classes
    MultiLevelCSVSchema schema = null;
    Counter counter = new Counter();
    ExceptionHandler ex = new ExceptionHandler(counter);
    CSVParser csv_parser = new CSVParser(counter, ex);

    // Additional exceptions
    public static final short E_SCH_START_WITH_SPACE = 3;
    public static final short E_DUPLICATE_NODE = 4;
    public static final short E_DOWN_2_LEVELS = 5;
    public static final short E_TOO_MANY_CHARS = 6;
    public static final short E_NODE_NOT_FOUND = 7;
    public static final short E_ONLY_ONE_ROOT = 8;

    // Transient variables used during parsing
    // These are not members
    private int cur_sibling = 1;
    private ParsedObject obj_out = null;
    private Reader r = null;
    private int cur_level = 0;
    private int node_ctr = 1;
    private int token_ctr = 0;
    private StringBuffer cur_path = null;
    private StringBuffer cur_sequence_path = null;
    private boolean is0def = false;
    private Node cur_node_schema;

    /**
     * Constructor (no parameters needed to create instance)
     */
    public MultiLevelCSVParser() {
        // Override with more exceptions
        ex.setMsgs("en-US", new String[] { "", "IOException",
                "Unexpected character",
                "Schema definition cannot begin with a space",
                "Duplicate node definition", "Cannot go down two levels",
                "Too many characters in a column", "Node not found",
                "There can be only one root node" });
    }

    /**
     * Looks for the directive starting with csv_ml with the given Reader.
     * 
     * If the directive is not found, it puts back the first token and returns.
     * 
     * If directive is found, extracts and sets the directive attributes.
     * 
     * @param r
     *            Input reader
     * @throws IOException
     */
    private void parseDirective(Reader r) throws IOException {
        int token_ctr = 0;
        boolean to_continue = true;
        do {
            String value = csv_parser.parseNextToken(r);
            boolean is_eol = csv_parser.isEOL();
            if (token_ctr == 0 && value.equals("") && is_eol) {
                continue;
            }
            switch (token_ctr) {
            case 0:
                if (value.trim().toLowerCase().equals("csv_ml")) {
                    csv_ml_node_name = "with_node_name";
                    csv_ml_schema = "inline";
                } else {
                    to_continue = false;
                    csv_parser.reinsertLastToken();
                }
                break;
            case 1:
                csv_ml_ver = value;
                break;
            case 2:
                if (!value.equals(""))
                    csv_ml_encoding = value;
                break;
            case 3:
                if (!value.equals(""))
                    csv_ml_root = value;
                break;
            case 4:
                if (value.equals("no_node_name"))
                    csv_ml_node_name = value;
                break;
            case 5:
                if (!value.equals(""))
                    csv_ml_schema = value;
                break;
            case 6:
                if (!value.equals(""))
                    csv_ml_schema_file = value;
                break;
            }
            if (is_eol)
                to_continue = false;
            if (to_continue)
                token_ctr++;
        } while (to_continue);
    }

    /**
     * Parses the given InputStream into a W3C DOM Object
     * 
     * @param is
     *            The InputStream to parse
     * @param toValidate
     *            Whether to validate against the schema or not
     * @return Document object
     * @throws IOException
     */
    public Document parseToDOM(InputStream is, boolean toValidate)
            throws IOException {
        ParsedObject parsedObject = parse(ParsedObject.TARGET_W3C_DOC,
                new InputSource(is), toValidate);
        return (parsedObject == null ? null : parsedObject.getDocument());
    }

    /**
     * Parses the given InputStream into a Javascript Notation Object
     * 
     * @param is
     *            The InputStream to parse
     * @param toValidate
     *            Whether to validate against the schema or not
     * @return JSONObject of root
     * @throws IOException
     */
    public JSONObject parseToJSO(InputStream is, boolean toValidate)
            throws IOException {
        ParsedObject parsedObject = parse(ParsedObject.TARGET_JSON_OBJ,
                new InputSource(is), toValidate);
        return (parsedObject == null ? null : parsedObject.getJSONObject());
    }

    /**
     * Parses the given Reader into a W3C DOM Object
     * 
     * @param is
     *            The Reader to parse
     * @param toValidate
     *            Whether to validate against the schema or not
     * @return Document object
     * @throws IOException
     */
    public Document parseToDOM(Reader r, boolean toValidate) throws IOException {
        ParsedObject parsedObject = parse(ParsedObject.TARGET_W3C_DOC,
                new InputSource(r), toValidate);
        return (parsedObject == null ? null : parsedObject.getDocument());
    }

    /**
     * Parses the given Reader into a Javascript Notation Object
     * 
     * @param is
     *            The Reader to parse
     * @param toValidate
     *            Whether to validate against the schema or not
     * @return JSONObject of root
     * @throws IOException
     */
    public JSONObject parseToJSO(Reader r, boolean toValidate)
            throws IOException {
        ParsedObject parsedObject = parse(ParsedObject.TARGET_JSON_OBJ,
                new InputSource(r), toValidate);
        return (parsedObject == null ? null : parsedObject.getJSONObject());
    }

    /**
     * Main parsing logic
     * 
     * @param targetObject
     * @param is
     * @param toValidate
     * @return
     * @throws IOException
     */
    private ParsedObject parse(short targetObject, InputSource is,
            boolean toValidate) throws IOException {

        initParse(targetObject, is, toValidate);
        for (ParsedObject parsedObject = parseNext(); parsedObject != null; parsedObject = parseNext()) {
            // Do nothing. initParse() and parseNext() are public.
            // This code snippet could be used by caller and have some logic
            // here to have control during parsing and managing memory.
            //
            // parseNext() returns for each Node successfully constructed.
            // parsedObject.getCurrentElement() or parsedObject.getCurrentJSO()
            // can be called to have access to the successfully constructed
            // node, such as:
            //
            // if (parser.getEx().getErrorCode() == 0) {
            // Element parsedElement = parsedObject.getCurrentElement();
            // // Do something with parsedElement here
            // // if necessary free memory by deleting attributes
            // // // or even remove parseElement itself from the document object
            // after use
            // }
        }
        return obj_out;

    }

    /**
     * Initializes transient variables used during parsing, parses directive and
     * schema.
     * 
     * @param targetObject
     *            Whether DOM or JSO
     * @param is
     *            InputSource to read from
     * @param toValidate
     *            Whether to Validate or not
     * @throws IOException
     */
    public void initParse(short targetObject, InputSource is, boolean toValidate)
            throws IOException {

        // Initialize
        ex.reset_exceptions();
        csv_parser.reset();
        obj_out = null;
        r = null;
        csv_ml_ver = "1.0";
        csv_ml_encoding = "UTF-8";
        csv_ml_root = "root";
        csv_ml_node_name = "no_node_name";
        csv_ml_schema = "inline";
        csv_ml_schema_file = "";
        schema = new MultiLevelCSVSchema();

        // Parse directive
        String orig_encoding = csv_ml_encoding;
        if (is.getType() == InputSource.IS_BYTE_STREAM)
            r = new InputStreamReader(is.getInputStream(), csv_ml_encoding);
        else
            r = is.getReader();
        parseDirective(r);
        if (ex.getErrorCode() != 0)
            return;
        // If the encoding changed for a
        // byte input stream, reinitialize with
        // different encoding
        // This is commented because it is not working
        // apparently because InputStreamReader reads more bytes
        // than it requires from the InputStream and assigning
        // the stream to a new InputStreamReader does not read it correctly
        // if (!orig_encoding.equals(csv_ml_encoding)) {
        // if (is.getType() == InputSource.IS_BYTE_STREAM) {
        // r = new InputStreamReader(is.getInputStream(), csv_ml_encoding);
        // }
        // }
        // TODO: To read the directive as InputStream and to create
        // new InputStreamReader depending on given encoding

        // Parse Schema if present
        if (csv_ml_schema.equals("inline"))
            schema.parseSchema(csv_parser, csv_ml_node_name, csv_ml_schema, ex,
                    r);
        if (ex.getErrorCode() != 0)
            return;

        // Initialize variables
        cur_level = 0;
        node_ctr = 1;
        token_ctr = 0;
        cur_path = new StringBuffer();
        cur_sequence_path = new StringBuffer();
        cur_sibling = 1;
        is0def = false;
        cur_node_schema = null;

        obj_out = new ParsedObject(csv_ml_root, csv_ml_encoding, ex,
                targetObject);
        int slashIdx = csv_ml_root.indexOf('/'); // Remove namespaces after
                                                 // slash if present
        if (slashIdx != -1)
            csv_ml_root = csv_ml_root.substring(0, slashIdx);

    }

    /**
     * Main parsing logic. Returns each time an element formation is complete.
     * 
     * @return Parsed object for each element that gets built at
     *         parsedObject.getCurrentElement()
     * @throws IOException
     */
    public ParsedObject parseNext() throws IOException {

        // Main loop that processes each token from the stream
        // till there are no more tokens
        do {

            String value = csv_parser.parseNextToken(r);
            boolean is_eol = csv_parser.isEOL();

            // First column - usually the node indicator
            if (token_ctr == 0) {

                // skip empty lines
                if (value.trim().equals("") && is_eol) {
                    continue;
                }

                // trim if having leading spaces
                if (value.charAt(0) == ' ') {
                    value = value.trim();
                }

                if (csv_ml_schema.equals("no_schema")) {

                    // Generates node names if no schema specified
                    Util.addToSeqPath(cur_sequence_path, cur_sibling);
                    cur_node_schema = schema.getNodeBySeqPath(cur_sequence_path
                            .toString());
                    String cur_node_name = null;
                    if (cur_node_schema == null) {
                        if (csv_ml_node_name.equals("with_node_name"))
                            cur_node_name = value;
                        else
                            cur_node_name = "n" + node_ctr;
                        cur_node_schema = new Node();
                        cur_node_schema.setName(cur_node_name);
                        schema.addSeqPathNodeMap(cur_sequence_path.toString(),
                                cur_node_schema);
                        node_ctr++;
                    } else {
                        cur_node_name = (String) cur_node_schema.getName();
                    }
                    obj_out.addNewNode(cur_node_name);

                } else { // Schema present - value has node indicator in this
                         // section
                    char c = value.charAt(0);
                    // If not name will not be specified, then only one sibling
                    // can be present and node indicator would be '1'
                    if (csv_ml_node_name.equals("no_node_name")) {
                        c = '1';
                        value = "1";
                    }
                    if (c == '0') {
                        // Value indicates a re-usable node in schema
                        cur_sequence_path.replace(0,
                                cur_sequence_path.length(), value);
                        cur_node_schema = schema
                                .getNodeBySeqPath(cur_sequence_path.toString());
                        if (cur_node_schema == null) {
                            ex.set_err(E_NODE_NOT_FOUND);
                            break;
                        }
                        // TODO: Validate whether valid child or not
                        cur_path.replace(0, cur_path.length(),
                                cur_node_schema.getPath());
                    } else if (c >= '1' && c <= '9') {
                        // Value indicates sibling number
                        // so find node by sequence path
                        Util.addToSeqPath(cur_sequence_path,
                                Integer.parseInt(value.trim()));
                        cur_node_schema = schema
                                .getNodeBySeqPath(cur_sequence_path.toString());
                        if (cur_node_schema == null) {
                            ex.set_err(E_NODE_NOT_FOUND);
                            break;
                        }
                        cur_path.replace(0, cur_path.length(),
                                cur_node_schema.getPath());
                    } else {
                        // Value indicates node name
                        // so find node by node name path
                        Util.addToPath(cur_path, value);
                        cur_node_schema = schema.getNodeByNamePath(cur_path
                                .toString());
                        if (cur_node_schema == null) {
                            ex.set_err(E_NODE_NOT_FOUND);
                            break;
                        }
                        cur_sequence_path.replace(0,
                                cur_sequence_path.length(),
                                (String) cur_node_schema.getSeqPath());
                    }
                    String cur_node_name = (String) cur_node_schema.getName();
                    obj_out.addNewNode(cur_node_name);
                }
                // If first column will not specify node,
                // assume it is data and put it back
                // (node name would be a generated one)
                if (csv_ml_node_name.equals("no_node_name")) {
                    token_ctr++;
                    csv_parser.reinsertLastToken();
                    continue;
                }
            } else if (token_ctr > 0) {
                // Add attributes to node
                List<Column> column_arr = cur_node_schema.getColumns();
                int arr_len = column_arr.size();
                if (!csv_ml_schema.equals("no_schema") && token_ctr > arr_len) {
                    // If no more attributes in schema, add as Node Content
                    value = Util.encodeToCSVText(value);
                    obj_out.addContent(value);
                } else {
                    String col_name = "";
                    if (csv_ml_schema.equals("no_schema")) {
                        col_name = "c" + token_ctr;
                    } else
                        col_name = column_arr.get(token_ctr - 1).getName();
                    if (col_name.indexOf(' ') != -1)
                        col_name = col_name.replace(' ', '_');
                    obj_out.setAttribute(col_name, value);
                }
            }
            token_ctr++;

            // End of line, need to check for spaces at the beginning
            // of next line for changing levels
            if (is_eol) {

                // If all columns did not get filled as per schema
                // add the remaining with empty data
                List<Column> column_arr = cur_node_schema.getColumns();
                int arr_len = column_arr.size();
                while (token_ctr <= arr_len) {
                    String col_name = column_arr.get(token_ctr - 1).getName();
                    obj_out.setAttribute(col_name, "");
                    token_ctr++;
                }
                obj_out.finalizeElement();

                // Count number of spaces at the beginning of next line
                int space_count = 0;
                int nxt_char = csv_parser.readChar(r);
                while (nxt_char == ' ' || nxt_char == '\t') {
                    space_count++;
                    nxt_char = csv_parser.readChar(r);
                }
                if (nxt_char == -1) // end of stream
                    break;
                else {
                    // If space not found put back the character
                    csv_parser.reInsertLastChar();
                }
                counter.setColNo(counter.getColNo() + space_count);
                if (space_count > (cur_level + 1)) {
                    // More spaces found than expected, so stop parsing
                    ex.set_err(E_DOWN_2_LEVELS);
                    break;
                } else {
                    // adjust cur_level according to space_count
                    while (cur_level >= space_count) {
                        // update sequence and name paths
                        // according to change in level
                        if (!is0def) {
                            Util.removeFromPath(cur_path);
                            cur_sibling = Util.removeFromSeqPath(
                                    cur_sequence_path, csv_ml_schema,
                                    csv_ml_node_name, cur_sibling);
                        }
                        obj_out.traverseToParent();
                        cur_level--;
                    }
                    cur_level++;
                }
                if (is0def) {
                    cur_path.setLength(0);
                    cur_sequence_path.setLength(0);
                    // TODO: restore paths?
                }
                is0def = false;
                token_ctr = 0;
                return obj_out;
            }
        } while (!csv_parser.isEOS());
        obj_out.deleteParentRefs();
        return null;
    }

    /**
     * Getter for schema
     * 
     * @return schema associated with the parser
     */
    public MultiLevelCSVSchema getSchema() {
        return schema;
    }

    /**
     * Getter for Exception Handler
     * 
     * @return Exception handler associated with the parser
     */
    public ExceptionHandler getEx() {
        return ex;
    }

    /**
     * Sets delimiter character used for parsing
     * 
     * @param d
     *            Delimiter
     */
    public void setDelimiter(char d) {
        csv_parser.setDelimiter(d);
    }

}
