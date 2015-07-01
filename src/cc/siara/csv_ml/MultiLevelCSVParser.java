package cc.siara.csv_ml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cc.siara.csv.CSVParser;
import cc.siara.csv.ExceptionHandler;

public class MultiLevelCSVParser {

    String csv_ml_ver = "1.0";
    String csv_ml_root = "root";
    String csv_ml_node_name = "no_node_name";
    String csv_ml_schema = "no_schema";
    String csv_ml_encoding = "UTF-8";

    int cur_sibling = 1;
    MultiLevelCSVSchema schema = new MultiLevelCSVSchema();
    public ExceptionHandler ex = new ExceptionHandler();
    CSVParser csv_parser = new CSVParser(ex);

    static final short ST_SCH_COL_NAME = 1;
    static final short ST_SCH_COL_ALIAS = 2;
    static final short ST_SCH_COL_ARR = 3;
    static final short ST_SCH_COL_LEN = 4;
    static final short ST_SCH_COL_TYPE = 5;
    static final short ST_SCH_COL_DEF = 6;
    static final short ST_SCH_COL_VAL = 7;

    public MultiLevelCSVParser() {
    }

    private void parse_directive(Reader r) throws IOException {
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
          if (to_continue) token_ctr++;
       } while (to_continue);
       return;
    }

	private String remove_from_seq_path(String cur_sequence_path) {
		int idx = cur_sequence_path.lastIndexOf(".");
		if (idx == -1) {
			cur_sequence_path = "";
			if (!csv_ml_schema.equals("no_schema")
					|| csv_ml_node_name.equals("with_node_name"))
				cur_sibling++;
		} else {
			if (!csv_ml_schema.equals("no_schema")
					|| csv_ml_node_name.equals("with_node_name"))
				cur_sibling = 1 + Integer.parseInt(cur_sequence_path
						.substring(idx + 1).trim());
			cur_sequence_path = cur_sequence_path.substring(0, idx);
		}
		return cur_sequence_path;
	}

    private String remove_from_path(String cur_path) {
       int idx = cur_path.lastIndexOf(".");
       if (idx == -1)
          cur_path = "";
       else
          cur_path = cur_path.substring(0, idx);
       return cur_path;
    }
    private String add_to_seq_path(String cur_sequence_path, int cur_sibling) {
       if (!cur_sequence_path.equals("")) {
          cur_sequence_path += ".";
       }
       cur_sequence_path += cur_sibling;
       return cur_sequence_path;
    }
    private String add_to_path(String cur_path, String node_name) {
       if (!cur_path.equals("")) {
          cur_path += ".";
       }
       cur_path += node_name;
       return cur_path;
    }
    private int deduce_column_state(char c, int cur_state) {
		if (c == '/')
			cur_state = ST_SCH_COL_ALIAS;
		else if (c == '(')
			cur_state = ST_SCH_COL_LEN;
		else if (c == ')')
			cur_state = ST_SCH_COL_TYPE;
		else if (c == '=')
			cur_state = ST_SCH_COL_DEF;
		else if (c == '{')
			cur_state = ST_SCH_COL_VAL;
		else if (c == '}')
			cur_state = 999;
		return cur_state;
    }
    private void parse_column_schema(String value, MultiLevelCSVSchema.Column column_obj) throws IOException {
       int cur_state = ST_SCH_COL_NAME;
       StringBuffer col_name = new StringBuffer();
       StringBuffer col_alias = new StringBuffer();
       StringBuffer col_len = new StringBuffer();
       StringBuffer col_type = new StringBuffer();
       StringBuffer col_default = null;
       StringBuffer col_values = new StringBuffer();
       for (int j=0; j<value.length(); j++) {
           char c = value.charAt(j);
           switch (cur_state) {
             case ST_SCH_COL_NAME:
                  cur_state = deduce_column_state(c, cur_state);
                  if (cur_state == ST_SCH_COL_NAME)
                     col_name.append(c);
                  break;
             case ST_SCH_COL_ALIAS:
                  cur_state = deduce_column_state(c, cur_state);
                  if (cur_state == ST_SCH_COL_ALIAS)
                      col_alias.append(c);
                  break;
             case ST_SCH_COL_LEN:
                  cur_state = deduce_column_state(c, cur_state);
                  if (cur_state == ST_SCH_COL_LEN)
                      col_len.append(c);
                  break;
             case ST_SCH_COL_TYPE:
                  cur_state = deduce_column_state(c, cur_state);
                  if (cur_state == ST_SCH_COL_TYPE)
                      col_type.append(c);
                  break;
             case ST_SCH_COL_DEF:
           	      if (col_default == null)
        		     col_default = new StringBuffer();
                  cur_state = deduce_column_state(c, cur_state);
                  if (cur_state == ST_SCH_COL_DEF)
                      col_default.append(c);
                  break;
             case ST_SCH_COL_VAL:
                  cur_state = deduce_column_state(c, cur_state);
                  if (cur_state == ST_SCH_COL_VAL)
                      col_values.append(c);
                  break;
           }
       }
       if (cur_state == ST_SCH_COL_DEF && col_default == null)
    	  col_default = new StringBuffer();
       List<String> col_values_list = new LinkedList<String>();
       if (col_values.length() == 0) {
          col_values_list.add("");
       } else {
          String val_csv = col_values.toString();
          CSVParser parser = new CSVParser(new ExceptionHandler());
          while (!csv_parser.isEOS()) {
        	String token = parser.parseNextToken(new StringReader(val_csv));
            col_values_list.add(token);
         }
       }
       int c_idx = col_name.indexOf(":");
       if (c_idx == -1) {
          column_obj.setNs("");
          column_obj.setName(col_name.toString());
       } else {
          column_obj.setNs(col_name.substring(0, c_idx));
          column_obj.setName(col_name.substring(c_idx+1));
       }
       column_obj.setAlias(col_alias.toString());
       column_obj.setLen(col_len.toString());
       column_obj.setType(col_type.toString());
       if (col_default == null)
          column_obj.setColDefault(null);
       else
          column_obj.setColDefault(col_default.toString());
       column_obj.setValues(col_values_list);
    }

    private void parse_schema(Reader r) throws IOException {
       String cur_path = "";
       String cur_sequence_path = "";
       int cur_level = 0;
       cur_sibling = 1;
       int token_ctr = 0;
       boolean to_continue = true;
       boolean is0def = false;
       boolean is0ref = false;
       MultiLevelCSVSchema.Node prev_node_obj = null;
       int node_ctr = 1;
       String zero_def_name = "";
       int nxt_char = csv_parser.readChar(r);
       if (nxt_char==' ' || nxt_char=='\t') {
          ex.set_err(ExceptionHandler.E_SCH_START_WITH_SPACE, csv_parser);
          return;
       } else
    	  csv_parser.reInsertLastChar();
       do {
          String value = csv_parser.parseNextToken(r);
          boolean is_eol = csv_parser.isEOL();
          if (value.length() > 0 && value.charAt(0) == '0' && token_ctr == 0) {
             if (cur_level == 0) {
                is0def = true;
                zero_def_name = value.trim();
                token_ctr--;
             }
             if (cur_level > 0)
                is0ref = true;
          }
          if (token_ctr == 0 && !is0ref) {
             String node_name = value.trim();
             if (node_name.equals("") && is_eol) {
                continue;
             }
             if (csv_ml_node_name.equals("no_node_name")) {
                node_name = "n"+node_ctr;
                node_ctr++;
             }
             if (cur_level == 0) {
                if (node_name.equals("end_schema"))
                   return;
                char c = node_name.charAt(0);
                if (c >= '1' && c <= '9') {
                   csv_parser.reinsertLastToken();
                   return;
                }
             }
             if (is0def) {
                cur_sequence_path = zero_def_name;
                cur_path = zero_def_name;
             } else {
                cur_sequence_path = add_to_seq_path(cur_sequence_path, cur_sibling);
                cur_path = add_to_path(cur_path, node_name);
             }
             MultiLevelCSVSchema.Node node_obj = new MultiLevelCSVSchema.Node();
             prev_node_obj = node_obj;
             node_obj.setName(node_name);
             node_obj.setPath(cur_path);
             node_obj.setSeqPath(cur_sequence_path);
             if (schema.getNodeByNamePath(cur_path) != null) {
                ex.set_err(ExceptionHandler.E_DUPLICATE_NODE, csv_parser);
                return;
             }
             schema.addNamePathNode(cur_path, node_obj);
             schema.setSeqPathNodeMap(cur_sequence_path, node_obj);
             if (csv_ml_node_name.equals("no_node_name")) {
            	MultiLevelCSVSchema.Column column_obj = new MultiLevelCSVSchema.Column();
                node_obj.addColumn(column_obj);
                parse_column_schema(value, column_obj);
             }
          } else
          if (token_ctr > 0 && !is0ref) {
             MultiLevelCSVSchema.Column column_obj = new MultiLevelCSVSchema.Column();
             prev_node_obj.addColumn(column_obj);
             parse_column_schema(value, column_obj);
          }
          if (is0ref) {
             prev_node_obj.addZeroChild(value.trim());
          }
          token_ctr++;
          if (is_eol) {
             int space_count = 0;
             nxt_char = csv_parser.readChar(r);
			 while (nxt_char == ' '	|| nxt_char == '\t') {
				space_count++;
				nxt_char = csv_parser.readChar(r);
			 }
			 if (nxt_char == -1)
				 return;
			 else
				 csv_parser.reInsertLastChar();
             csv_parser.setColNo(csv_parser.getColNo()+space_count);
             if (space_count > (cur_level+1)) {
                ex.set_err(ExceptionHandler.E_DOWN_2_LEVELS, csv_parser);
                return;
             } else {
                while (cur_level >= space_count) {
                   if (!is0def && !is0ref) {
                      cur_path = remove_from_path(cur_path);
                      cur_sequence_path = remove_from_seq_path(cur_sequence_path);
                   }
                   cur_level--;
                }
                cur_level++;
             }
             if (csv_ml_node_name.equals("no_node_name") && space_count == 0)
                return;
             if (is0def || is0ref) {
                cur_path = "";
                cur_sequence_path = "";
             }
             is0def = false;
             is0ref = false;
             token_ctr = 0;
          }
       } while (to_continue);
       return;
    }

    private void delete_parent_refs(JSONObject obj) {
       if (obj.containsKey("parent_ref"))
            obj.remove("parent_ref");
       for (Object set : obj.keySet()) {
    	  String key = (String) set;
    	  Object child_obj = obj.get(key);
          if (child_obj instanceof JSONArray) {
        	 JSONArray ja = (JSONArray) child_obj;
             for (Object ele : ja) {
                 if (ele instanceof JSONObject)
            	    delete_parent_refs((JSONObject)ele);
             }
          } else
          if (child_obj instanceof JSONObject) {
             delete_parent_refs((JSONObject)child_obj);
          }
       }
    }

    private Object add_new_node(String dom_or_jso, Object obj_out, Object cur_node, String node_name) {
       Object new_node = null;
       if (dom_or_jso.equals("dom")) {
          Document doc = (Document) obj_out;
          boolean to_create_new_element = true;
          Node cur_node_parent = ((Node) cur_node).getParentNode();
          if (cur_node_parent != null
               && cur_node_parent.getNodeType() == 9
               && node_name.equals(csv_ml_root)) {
             to_create_new_element = false;
          }
          if (to_create_new_element) {
             new_node = doc.createElement(node_name);
             if (cur_node_parent == null)
                ex.set_err(ExceptionHandler.E_ONLY_ONE_ROOT, csv_parser);
             else
                ((Element)cur_node).appendChild((Element)new_node);
          } else {
             new_node = cur_node;
          }
       } else {
          JSONObject jo = (JSONObject) cur_node;
          if (jo.get(node_name) == null) {
        	  jo.put(node_name, new JSONArray());
          }
          JSONArray ja = (JSONArray) jo.get(node_name);
          new_node = new JSONObject();
          ((JSONObject) new_node).put("parent_ref", cur_node);
          ja.add((JSONObject)new_node);
       }
       return new_node;
    }

    public Object parse(String dom_or_jso, Reader r, boolean to_validate) throws IOException {
       ex.reset_exceptions();
       csv_parser.reset();
       parse_directive(r);
       if (ex.error_code != 0)
          return null;
       if (csv_ml_schema.equals("inline"))
          parse_schema(r);
       if (ex.error_code != 0)
          return null;
       int cur_level = 0;
       int node_ctr = 1;
       int token_ctr = 0;
       String cur_path = "";
       String cur_sequence_path = "";
       Object obj_out = null;
       cur_sibling = 1;
       boolean is0def = false;
       String zero_def_name = "";
       if (dom_or_jso.equals("dom")) {
          String namespace = "";
          String[] ns_uri = new String[0];
          int e_idx = csv_ml_root.indexOf("/");
          if (e_idx > 0) {
             ns_uri = csv_ml_root.substring(e_idx+1).split(" ");
             csv_ml_root = csv_ml_root.substring(0, e_idx);
          }
          String xml_str = "<"+csv_ml_root;
          int c_idx = csv_ml_root.indexOf(":");
          if (c_idx > 0) {
             namespace = csv_ml_root.substring(0, c_idx);
             if (ns_uri.length == 0)
                ns_uri = new String[] {namespace+"='http://siara.cc/ns'"};
          }
          for (int j=0; j<ns_uri.length; j++)
             xml_str += (" xmlns:"+ns_uri[j]);
          xml_str += ("></"+csv_ml_root+">");
          obj_out = Util.parseXMLToDOM(xml_str);
       } else {
    	  JSONObject jo = new JSONObject();
          obj_out = jo;
       }
       Object cur_node = (dom_or_jso.equals("dom")?((Document)obj_out).getDocumentElement():obj_out);
       MultiLevelCSVSchema.Node cur_node_schema = null;
       do {
          String value = csv_parser.parseNextToken(r);
          boolean is_eol = csv_parser.isEOL();
          if (value.length() > 0 && value.charAt(0) == '0' && token_ctr == 0) {
             if (cur_level == 0) {
                is0def = true;
                zero_def_name = value.trim();
                token_ctr--;
             }
          }
          if (token_ctr == 0) {
             if (value.trim().equals("") && is_eol) {
                continue;
             }
             if (csv_ml_schema.equals("no_schema")) {
                cur_sequence_path = add_to_seq_path(cur_sequence_path, cur_sibling);
                cur_node_schema = schema.getNodeBySeqPath(cur_sequence_path);
                String cur_node_name = null;
                if (cur_node_schema == null) {
                   if (csv_ml_node_name.equals("with_node_name"))
                      cur_node_name = value;
                   else
                      cur_node_name = "n"+node_ctr;
                   cur_node_schema = new MultiLevelCSVSchema.Node();
                   cur_node_schema.setName(cur_node_name);
                   schema.setSeqPathNodeMap(cur_sequence_path, cur_node_schema);
                   node_ctr++;
                } else {
                   cur_node_name = (String) cur_node_schema.getName();
                }
                cur_node = add_new_node(dom_or_jso, obj_out, cur_node, cur_node_name);
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
                   cur_sequence_path = zero_def_name;
                   cur_node_schema = schema.getNodeBySeqPath(cur_sequence_path);
                   if (cur_node_schema == null) {
                      ex.set_err(ExceptionHandler.E_NODE_NOT_FOUND, csv_parser);
                      return null;
                   }
                   // TODO: Validate whether valid child or not
                   cur_path = (String) cur_node_schema.getPath();
                } else
                if (c >= '1' && c <= '9') {
                   cur_sequence_path = add_to_seq_path(cur_sequence_path, Integer.parseInt(value.trim()));
                   cur_node_schema = schema.getNodeBySeqPath(cur_sequence_path);
                   if (cur_node_schema == null) {
                      ex.set_err(ExceptionHandler.E_NODE_NOT_FOUND, csv_parser);
                      return null;
                   }
                   cur_path = (String) cur_node_schema.getPath();
                } else {
                   cur_path = add_to_path(cur_path, value);
                   cur_node_schema = schema.getNodeByNamePath(cur_path);
                   if (cur_node_schema == null) {
                      ex.set_err(ExceptionHandler.E_NODE_NOT_FOUND, csv_parser);
                      return null;
                   }
                   cur_sequence_path = (String) cur_node_schema.getSeqPath();
                }
                String cur_node_name = (String) cur_node_schema.getName();
                cur_node = add_new_node(dom_or_jso, obj_out, cur_node, cur_node_name);
                if (csv_ml_node_name.equals("no_node_name")) {
                   token_ctr++;
                   csv_parser.reinsertLastToken();
                   continue;
                }
             }
          } else
          if (token_ctr > 0) {
             List<MultiLevelCSVSchema.Column> column_arr = cur_node_schema.getColumns();
             int arr_len = column_arr.size();
             if (!csv_ml_schema.equals("no_schema") && token_ctr > arr_len) {
                value = CSVParser.encodeToCSVText(value);
                if (dom_or_jso.equals("dom")) {
                   Element cur_ele = (Element) cur_node;
                   if (!cur_ele.getTextContent().equals(""))
                      cur_ele.setTextContent(cur_ele.getTextContent() + ",");
                   cur_ele.setTextContent(cur_ele.getTextContent() + value);
                } else {
                   JSONObject cur_obj = (JSONObject) cur_node;
                   String cur_val = (String) cur_obj.get("_content");
                   if (cur_val == null)
                      cur_obj.put("_content", value);
                   else
                      cur_obj.put("_content", cur_val+","+value);
                }
             } else {
                String col_name = "";
                if (csv_ml_schema.equals("no_schema")) {
                   col_name = "c"+token_ctr;
                } else
                   col_name = column_arr.get(token_ctr-1).getName();
                if (dom_or_jso.equals("dom"))
                   ((Element)cur_node).setAttribute(col_name, value);
                else
                   ((JSONObject)cur_node).put(col_name, value);
             }
          }
          token_ctr++;
          if (is_eol) {
             List<MultiLevelCSVSchema.Column> column_arr = cur_node_schema.getColumns();
             int arr_len = column_arr.size();
             while (token_ctr <= arr_len) {
                if (dom_or_jso.equals("dom")) {
                   String col_name = column_arr.get(token_ctr-1).getName();
                   if (col_name.indexOf("xmlns:") != 0)
                      ((Element)cur_node).setAttribute(col_name, "");
                } else
                   ((JSONObject)cur_node).put(column_arr.get(token_ctr-1).getName(), "");
                token_ctr++;
             }
             int space_count = 0;
             int nxt_char = csv_parser.readChar(r);
			 while (nxt_char == ' '	|| nxt_char == '\t') {
				space_count++;
				nxt_char = csv_parser.readChar(r);
			 }
			 if (nxt_char == -1)
				 break;
			 else
				 csv_parser.reInsertLastChar();
             csv_parser.setColNo(csv_parser.getColNo()+space_count);
             if (space_count > (cur_level+1)) {
                ex.set_err(ExceptionHandler.E_DOWN_2_LEVELS, csv_parser);
                return null;
             } else {
                while (cur_level >= space_count) {
                   if (!is0def) {
                      cur_path = remove_from_path(cur_path);
                      cur_sequence_path = remove_from_seq_path(cur_sequence_path);
                   }
                   if (dom_or_jso.equals("dom")) {
                      cur_node = ((Element)cur_node).getParentNode();
                      if (cur_node == null)
                         cur_node = ((Document)obj_out).getDocumentElement();
                   } else {
                	  JSONObject parent_ref = (JSONObject) ((JSONObject)cur_node).get("parent_ref");
                      if (parent_ref != null)
                    	 cur_node = parent_ref;
                   }
                   cur_level--;
                }
                cur_level++;
             }
             if (is0def) {
                cur_path = "";
                cur_sequence_path = "";
             }
             is0def = false;
             token_ctr = 0;
          }
       } while (!csv_parser.isEOS());
       if (dom_or_jso.equals("jso"))
          delete_parent_refs((JSONObject)obj_out);
       return obj_out;
    }

    public MultiLevelCSVSchema get_schema() {
       return schema;
    }

}
