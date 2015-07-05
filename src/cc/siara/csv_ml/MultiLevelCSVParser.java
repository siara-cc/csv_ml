/*
 * Copyright (C) 2015 arun@siara.cc
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
    String csv_ml_root = "root";
    String csv_ml_node_name = "no_node_name";
    String csv_ml_schema = "no_schema";
    String csv_ml_encoding = "UTF-8";

    // Members
    int cur_sibling = 1;

    // Associated classes
    MultiLevelCSVSchema schema = new MultiLevelCSVSchema();
    Counter counter = new Counter();
    ExceptionHandler ex = new ExceptionHandler(counter);
    CSVParser csv_parser = new CSVParser(counter, ex);

    public MultiLevelCSVParser() {
        ex.setMsgs("en-US", new String[] {""
                , "IOException"
                , "Unexpected character"
                , "Schema definition cannot begin with a space"
                , "Duplicate node definition"
                , "Cannot go down two levels"
                , "Too many characters in a column"
                , "Node not found"
                , "There can be only one root node"
                });
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
                if (value.equals("no_schema"))
                    csv_ml_schema = value;
                break;
            }
            if (is_eol)
                to_continue = false;
            if (to_continue)
                token_ctr++;
        } while (to_continue);
    }

    public Document parseToDOM(InputStream is, boolean toValidate) throws IOException {
        ParsedObject parsedObject = parse(ParsedObject.TARGET_W3C_DOC, new InputSource(is), toValidate);
        return parsedObject.getDocument();
    }

    public JSONObject parseToJSO(InputStream is, boolean toValidate) throws IOException {
        ParsedObject parsedObject = parse(ParsedObject.TARGET_JSON_OBJ, new InputSource(is), toValidate);
        return parsedObject.getJSONObject();
    }

    public Document parseToDOM(Reader r, boolean toValidate) throws IOException {
        ParsedObject parsedObject = parse(ParsedObject.TARGET_W3C_DOC, new InputSource(r), toValidate);
        return parsedObject.getDocument();
    }

    public JSONObject parseToJSO(Reader r, boolean toValidate) throws IOException {
        ParsedObject parsedObject = parse(ParsedObject.TARGET_JSON_OBJ, new InputSource(r), toValidate);
        return parsedObject.getJSONObject();
    }

    private ParsedObject parse(short targetObject, InputSource is, boolean toValidate)
            throws IOException {
        ex.reset_exceptions();
        csv_parser.reset();
        ParsedObject obj_out = new ParsedObject(ex);
        Reader r = null;
        String orig_encoding = csv_ml_encoding;
        if (is.getType() == InputSource.IS_BYTE_STREAM)
            r = new InputStreamReader(is.getInputStream(), csv_ml_encoding);
        else
            r = is.getReader();
        parseDirective(r);
        if (ex.getErrorCode() != 0)
            return obj_out;
        if (!orig_encoding.equals(csv_ml_encoding)) {
            if (is.getType() == InputSource.IS_BYTE_STREAM)
                r = new InputStreamReader(is.getInputStream(), csv_ml_encoding);
        }
        if (csv_ml_schema.equals("inline"))
            schema.parseSchema(csv_parser, csv_ml_node_name, csv_ml_schema, ex,
                    r);
        if (ex.getErrorCode() != 0)
            return obj_out;
        int cur_level = 0;
        int node_ctr = 1;
        int token_ctr = 0;
        StringBuffer cur_path = new StringBuffer();
        StringBuffer cur_sequence_path = new StringBuffer();
        cur_sibling = 1;
        boolean is0def = false;
        String namespace = "";
        String[] ns_uri = new String[0];
        int e_idx = csv_ml_root.indexOf("/");
        if (e_idx > 0) {
            ns_uri = csv_ml_root.substring(e_idx + 1).split(" ");
            csv_ml_root = csv_ml_root.substring(0, e_idx);
        }
        int c_idx = csv_ml_root.indexOf(":");
        if (c_idx > 0) {
            namespace = csv_ml_root.substring(0, c_idx);
            if (ns_uri.length == 0)
                ns_uri = new String[] { namespace + "='http://siara.cc/ns'" };
        }
        if (targetObject == ParsedObject.TARGET_W3C_DOC)
            obj_out = new ParsedObject(csv_ml_root, ns_uri, ex);
        Node cur_node_schema = null;
        do {
            String value = csv_parser.parseNextToken(r);
            boolean is_eol = csv_parser.isEOL();
            if (token_ctr == 0) {
                if (value.trim().equals("") && is_eol) {
                    continue;
                }
                if (csv_ml_schema.equals("no_schema")) {
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
                    if (csv_ml_node_name.equals("no_node_name")) {
                        token_ctr++;
                        csv_parser.reinsertLastToken();
                        continue;
                    }
                } else {
                    char c = value.charAt(0);
                    if (csv_ml_node_name.equals("no_node_name")) {
                        c = '1';
                        value = "1";
                    }
                    if (c == '0') {
                        cur_sequence_path.replace(0,
                                cur_sequence_path.length(), value);
                        cur_node_schema = schema
                                .getNodeBySeqPath(cur_sequence_path.toString());
                        if (cur_node_schema == null) {
                            ex.set_err(ExceptionHandler.E_NODE_NOT_FOUND);
                            return obj_out;
                        }
                        // TODO: Validate whether valid child or not
                        cur_path.replace(0, cur_path.length(),
                                cur_node_schema.getPath());
                    } else if (c >= '1' && c <= '9') {
                        Util.addToSeqPath(cur_sequence_path,
                                Integer.parseInt(value.trim()));
                        cur_node_schema = schema
                                .getNodeBySeqPath(cur_sequence_path.toString());
                        if (cur_node_schema == null) {
                            ex.set_err(ExceptionHandler.E_NODE_NOT_FOUND);
                            return obj_out;
                        }
                        cur_path.replace(0, cur_path.length(),
                                cur_node_schema.getPath());
                    } else {
                        Util.addToPath(cur_path, value);
                        cur_node_schema = schema.getNodeByNamePath(cur_path
                                .toString());
                        if (cur_node_schema == null) {
                            ex.set_err(ExceptionHandler.E_NODE_NOT_FOUND);
                            return obj_out;
                        }
                        cur_sequence_path.replace(0,
                                cur_sequence_path.length(),
                                (String) cur_node_schema.getSeqPath());
                    }
                    String cur_node_name = (String) cur_node_schema.getName();
                    obj_out.addNewNode(cur_node_name);
                    if (csv_ml_node_name.equals("no_node_name")) {
                        token_ctr++;
                        csv_parser.reinsertLastToken();
                        continue;
                    }
                }
            } else if (token_ctr > 0) {
                List<Column> column_arr = cur_node_schema
                        .getColumns();
                int arr_len = column_arr.size();
                if (!csv_ml_schema.equals("no_schema") && token_ctr > arr_len) {
                    value = CSVParser.encodeToCSVText(value);
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
            if (is_eol) {
                List<Column> column_arr = cur_node_schema
                        .getColumns();
                int arr_len = column_arr.size();
                while (token_ctr <= arr_len) {
                    String col_name = column_arr.get(token_ctr - 1).getName();
                    obj_out.setAttribute(col_name, "");
                    token_ctr++;
                }
                int space_count = 0;
                int nxt_char = csv_parser.readChar(r);
                while (nxt_char == ' ' || nxt_char == '\t') {
                    space_count++;
                    nxt_char = csv_parser.readChar(r);
                }
                if (nxt_char == -1)
                    break;
                else
                    csv_parser.reInsertLastChar();
                counter.setColNo(counter.getColNo() + space_count);
                if (space_count > (cur_level + 1)) {
                    ex.set_err(ExceptionHandler.E_DOWN_2_LEVELS);
                    return obj_out;
                } else {
                    while (cur_level >= space_count) {
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
                }
                is0def = false;
                token_ctr = 0;
            }
        } while (!csv_parser.isEOS());
        obj_out.deleteParentRefs();
        return obj_out;
    }

    public MultiLevelCSVSchema getSchema() {
        return schema;
    }

    public ExceptionHandler getEx() {
        return ex;
    }

}
