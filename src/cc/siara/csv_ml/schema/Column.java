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
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import cc.siara.csv.CSVParser;
import cc.siara.csv.Counter;
import cc.siara.csv.ExceptionHandler;

/**
 * Represents schema of a column which is part of the multi-level schema
 * 
 * @author Arundale R.
 */
public class Column {

    // Members
    String ns;
    String name;
    String alias;
    String len;
    String type;
    String col_default;
    List<String> values;

    /**
     * Getter for Namespace prefix
     * 
     * @return Namespace
     */
    public String getNs() {
        return ns;
    }

    /**
     * Setter for Namespace
     * 
     * @param ns
     */
    public void setNs(String ns) {
        this.ns = ns;
    }

    /**
     * Getter for column name
     * 
     * @return Column name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for column name
     * 
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for column alias
     * 
     * @return Column alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Setter for column alias
     * 
     * @param alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Gets column length
     * 
     * @return Column length
     */
    public String getLen() {
        return len;
    }

    /**
     * Sets column length
     * 
     * @param len
     */
    public void setLen(String len) {
        this.len = len;
    }

    /**
     * Gets column type
     * 
     * @return Column type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets column type
     * 
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets column default value
     * 
     * @return Default value
     */
    public String getColDefault() {
        return col_default;
    }

    /**
     * Sets column default
     * 
     * @param col_default
     */
    public void setColDefault(String col_default) {
        this.col_default = col_default;
    }

    /**
     * Gets possible values
     * 
     * @return List of values
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * Sets List of possible values
     * 
     * @param values
     */
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

    /**
     * Determines parsing state based on current character
     * 
     * @param c
     *            Current character
     * @param cur_state
     *            Current State
     * @return New state if wny
     */
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

    /**
     * Parses the member variables from given string
     * 
     * @param input
     *            Given string to parse
     * @throws IOException
     */
    void parseColumnSchema(String input) throws IOException {

        // Parse the given string and appends to corresponding StringBuffer
        int cur_state = ST_SCH_COL_NAME;
        StringBuffer col_name = new StringBuffer();
        StringBuffer col_alias = new StringBuffer();
        StringBuffer col_len = new StringBuffer();
        StringBuffer col_type = new StringBuffer();
        StringBuffer col_default = null;
        StringBuffer col_values = new StringBuffer();
        for (int j = 0; j < input.length(); j++) {
            char c = input.charAt(j);
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

        // Finalize StringBuffer values and set to corresponding members
        if (cur_state == ST_SCH_COL_DEF && col_default == null)
            col_default = new StringBuffer();
        List<String> col_values_list = new LinkedList<String>();
        if (col_values.length() == 0) {
            col_values_list.add("");
        } else {
            String val_csv = col_values.toString();
            Counter counter = new Counter();
            CSVParser parser = new CSVParser(counter, new ExceptionHandler(
                    counter));
            while (!parser.isEOS()) {
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
            setName(col_name.toString());
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
