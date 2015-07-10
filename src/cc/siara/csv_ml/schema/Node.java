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

import java.util.LinkedList;
import java.util.List;

/**
 * Represents schema of a node which is part of the multi-level schema
 * 
 * @author Arundale R.
 */
public class Node {

    // Members
    String name;
    String path;
    List<Column> columns = new LinkedList<Column>();
    String seq_path;
    List<String> zero_children = new LinkedList<String>();

    /**
     * Getter for node name
     * 
     * @return Node name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for node name
     * 
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Setter for node path
     * 
     * @return Node path
     */
    public String getPath() {
        return path;
    }

    /**
     * Setter for node path
     * 
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Getter for List of columns
     * 
     * @return List of columns
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Adds a parsed column object
     * 
     * @param column
     *            Column to add
     */
    public void addColumn(Column column) {
        this.columns.add(column);
    }

    /**
     * Getter for Sequence Path
     * 
     * @return Sequence Path
     */
    public String getSeqPath() {
        return seq_path;
    }

    /**
     * Sets sequence path
     * 
     * @param seq_path
     */
    public void setSeqPath(String seq_path) {
        this.seq_path = seq_path;
    }

    /**
     * Get Re-usable node list (Zero Children) tha could be children of this
     * node (used for validation only).
     * 
     * @return List of Re-usable nodes
     */
    public List<String> getZeroChildren() {
        return zero_children;
    }

    /**
     * Adds a re-usable node (used for validation only)
     * @param zero_child
     */
    public void addZeroChild(String zero_child) {
        this.zero_children.add(zero_child);
    }

}
