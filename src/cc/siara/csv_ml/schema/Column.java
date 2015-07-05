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
package cc.siara.csv_ml.schema;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import cc.siara.csv.CSVParser;
import cc.siara.csv.Counter;
import cc.siara.csv.ExceptionHandler;

public class Column {
    String ns;
    String name;
    String alias;
    String len;
    String type;
    String col_default;
    List<String> values;
    public String getNs() {
        return ns;
    }
    public void setNs(String ns) {
        this.ns = ns;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }
    public String getLen() {
        return len;
    }
    public void setLen(String len) {
        this.len = len;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getColDefault() {
        return col_default;
    }
    public void setColDefault(String col_default) {
        this.col_default = col_default;
    }
    public List<String> getValues() {
        return values;
    }
    public void setValues(List<String> values) {
        this.values = values;
    }

    // States for parsing column schema
    static final short ST_SCH_COL_NAME = 1;
    static final short ST_SCH_COL_ALIAS = 2;
    static final short ST_SCH_COL_ARR = 3;
    static final short ST_SCH_COL_LEN = 4;
    static final short ST_SCH_COL_TYPE = 5;
    static final short ST_SCH_COL_DEF = 6;
    static final short ST_SCH_COL_VAL = 7;
    private static int deduceColumnState(char c, int cur_state) {
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

    void parseColumnSchema(String value, CSVParser csv_parser) throws IOException {
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
                  cur_state = deduceColumnState(c, cur_state);
                  if (cur_state == ST_SCH_COL_NAME)
                     col_name.append(c);
                  break;
             case ST_SCH_COL_ALIAS:
                  cur_state = deduceColumnState(c, cur_state);
                  if (cur_state == ST_SCH_COL_ALIAS)
                      col_alias.append(c);
                  break;
             case ST_SCH_COL_LEN:
                  cur_state = deduceColumnState(c, cur_state);
                  if (cur_state == ST_SCH_COL_LEN)
                      col_len.append(c);
                  break;
             case ST_SCH_COL_TYPE:
                  cur_state = deduceColumnState(c, cur_state);
                  if (cur_state == ST_SCH_COL_TYPE)
                      col_type.append(c);
                  break;
             case ST_SCH_COL_DEF:
                     if (col_default == null)
                     col_default = new StringBuffer();
                  cur_state = deduceColumnState(c, cur_state);
                  if (cur_state == ST_SCH_COL_DEF)
                      col_default.append(c);
                  break;
             case ST_SCH_COL_VAL:
                  cur_state = deduceColumnState(c, cur_state);
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
          Counter counter = new Counter();
          CSVParser parser = new CSVParser(counter, new ExceptionHandler(counter));
          while (!csv_parser.isEOS()) {
            String token = parser.parseNextToken(new StringReader(val_csv));
            col_values_list.add(token);
         }
       }
       int c_idx = col_name.indexOf(":");
       if (c_idx == -1) {
          setNs("");
          setName(col_name.toString());
       } else {
          setNs(col_name.substring(0, c_idx));
          setName(col_name.substring(c_idx+1));
       }
       setAlias(col_alias.toString());
       setLen(col_len.toString());
       setType(col_type.toString());
       if (col_default == null)
          setColDefault(null);
       else
          setColDefault(col_default.toString());
       setValues(col_values_list);
    }

}
