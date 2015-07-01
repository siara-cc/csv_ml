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

import java.util.Hashtable;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import cc.siara.csv_ml.MultiLevelCSVSchema.Column;
import cc.siara.csv_ml.MultiLevelCSVSchema.Node;

public class DBBind {

    public static void generateDDL(MultiLevelCSVSchema schema, StringBuffer out_str) {
       boolean is_first = true;
       boolean is_id_present = false;
       boolean is_parent_id_present = false;
       Hashtable<String, Node> node_map = schema.getNamePathNodeMap();
       for (String key : node_map.keySet()) {
          MultiLevelCSVSchema.Node node = node_map.get(key);
          out_str.append("CREATE TABLE ");
          out_str.append(node.getName());
          out_str.append(" (");
          List<Column> columns_obj = node.getColumns();
          for (MultiLevelCSVSchema.Column col_obj : columns_obj) {
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
                   out_str.append(" DEFAULT '").append(col_default.replace("\'", "''")).append("'");
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

    public static void generate_dml_recursively(MultiLevelCSVSchema schema, Element ele, String path, StringBuffer out_str) {
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
             for (MultiLevelCSVSchema.Column col_obj : columns_obj) {
                String col_name = col_obj.getName();
                if (col_name.equals("id")) continue;
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
             for (MultiLevelCSVSchema.Column col_obj : columns_obj) {
                String col_name = col_obj.getName();
                if (col_name.equals("id")) continue;
                if (is_first)
                   is_first = false;
                else
                   out_str.append(", ");
                out_str.append("'").append(ele.getAttribute(col_name).replace("\'", "''")).append("'");
             }
             if (last_dot_idx != -1 && ele.getAttribute("parent_id") == null) {
                int earlier_dot_idx = path.lastIndexOf('.', last_dot_idx-1);
                String parent_table_name = "";
                if (earlier_dot_idx == -1)
                   parent_table_name = path.substring(0, last_dot_idx);
                else
                   parent_table_name = path.substring(earlier_dot_idx+1, last_dot_idx);
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
                for (MultiLevelCSVSchema.Column col_obj : columns_obj) {
                   String col_name = col_obj.getName();
                   if (col_name.equals("id")) continue;
                   if (is_first)
                      is_first = false;
                   else
                      out_str.append(", ");
                   out_str.append(col_name);
                   out_str.append(" = ");
                   out_str.append("'").append(ele.getAttribute(col_name).replace("\'", "''")).append("'");
                }
             }
             out_str.append(" WHERE id=");
             out_str.append(id_value);
             out_str.append(";\r\n\r\n");
          }
       }
       NodeList childNodes = ele.getChildNodes();
       for (int i=0; i<childNodes.getLength(); i++) {
           if (childNodes.item(i).getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
              continue;
           StringBuffer cur_path = new StringBuffer(path);
           if (cur_path.length() > 0) cur_path.append(".");
           cur_path.append(childNodes.item(i).getNodeName());
           generate_dml_recursively(schema, (Element) childNodes.item(i), cur_path.toString(), out_str);
       }
    }

}
