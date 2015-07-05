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

import java.util.LinkedList;
import java.util.List;

public class Node {
    String name;
    String path;
    List<Column> columns = new LinkedList<Column>();
    String seq_path;
    List<String> zero_children = new LinkedList<String>();
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public List<Column> getColumns() {
        return columns;
    }
    public void addColumn(Column column) {
        this.columns.add(column);
    }
    public String getSeqPath() {
        return seq_path;
    }
    public void setSeqPath(String seq_path) {
        this.seq_path = seq_path;
    }
    public List<String> getZeroChildren() {
        return zero_children;
    }
    public void addZeroChild(String zero_child) {
        this.zero_children.add(zero_child);
    }
}
