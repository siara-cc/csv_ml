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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cc.siara.csv.ExceptionHandler;

/**
 * Encapsulates the DOM or JSON object being built and updates either of them
 * accordingly.
 * 
 * @author Arundale R.
 * @since 1.0
 */
public class ParsedObject {

    public static final short TARGET_W3C_DOC = 1;
    public static final short TARGET_JSON_OBJ = 2;

    Document doc = null;
    Node cur_element = null;

    JSONObject jo = null;
    JSONObject cur_jo = null;

    short target = TARGET_JSON_OBJ;
    String csv_ml_root = null;
    ExceptionHandler ex = null;

    /**
     * Initialize object sets as JSON object by default Need to call the other
     * constructor for Document object
     * 
     * @param ex
     */
    public ParsedObject(ExceptionHandler ex) {
        this.ex = ex;
        jo = new JSONObject();
        cur_jo = jo;
    }

    /**
     * Initialize to build Document object
     * 
     * @param csv_ml_root
     *            Root node name to build initial Document
     * @param ns_uri
     *            Any global namespaces to build initial Document
     * @param ex
     */
    public ParsedObject(String csv_ml_root, String[] ns_uri, ExceptionHandler ex) {
        String xml_str = "<" + csv_ml_root;
        for (int j = 0; j < ns_uri.length; j++)
            xml_str += (" xmlns:" + ns_uri[j]);
        xml_str += ("></" + csv_ml_root + ">");
        doc = Util.parseXMLToDOM(xml_str);
        cur_element = doc.getDocumentElement();
        this.ex = ex;
        this.csv_ml_root = csv_ml_root;
        this.target = TARGET_W3C_DOC;
    }

    /**
     * Adds new W3C Element object and makes it current
     * 
     * @param node_name
     *            Name of new element to be created
     */
    private void addNewElement(String node_name) {
        Node new_node = null;
        if (cur_element.getNodeType() == Node.DOCUMENT_NODE) {
            // If given input tries to add more than one root, throw error
            ex.set_err(MultiLevelCSVParser.E_ONLY_ONE_ROOT);
            return;
        } else {
            // If directive specifies the first element as root,
            // do not add new. Otherwise add new Element
            if (!cur_element.equals(doc.getDocumentElement())
                    || !node_name.equals(csv_ml_root)) {
                new_node = doc.createElement(node_name);
                cur_element.appendChild(new_node);
            }
        }
        if (new_node != null)
            cur_element = new_node;
    }

    /**
     * Adds new JSONObject and makes it current
     * 
     * @param node_name
     *            Name of new JSONObject to be created
     */
    private void addNewJO(String node_name) {
        JSONObject new_node = null;
        if (cur_jo.get(node_name) == null) {
            cur_jo.put(node_name, new JSONArray());
        }
        // Each node is assumed to be an array having multiple data elements
        JSONArray ja = (JSONArray) cur_jo.get(node_name);
        new_node = new JSONObject();
        // Keep a reference to the parent, so that we can go up the level
        new_node.put("parent_ref", cur_jo);
        ja.add(new_node);
        cur_jo = new_node;
    }

    /**
     * Adds new node depending on target type
     * 
     * @param node_name
     *            Name of new node to be created.
     */
    public void addNewNode(String node_name) {
        if (target == TARGET_W3C_DOC)
            addNewElement(node_name);
        else
            addNewJO(node_name);
    }

    /**
     * Sets data attribute. In case of W3C Document, sets value of W3C
     * Attribute. In case of JSON, sets new JSONString
     * 
     * @param col_name
     *            Name of attribute
     * @param value
     *            Value of attribute
     */
    public void setAttribute(String col_name, String value) {
        if (target == TARGET_W3C_DOC) {
            if (cur_element.getNodeType() == Node.ELEMENT_NODE
                    && col_name.indexOf("xmlns:") != 0)
                ((Element) cur_element).setAttribute(col_name, value);
        } else
            cur_jo.put(col_name, value);
    }

    /**
     * Sets node content. In case of W3C Document, creates W3C CData section. In
     * case of JSON, sets new JSONString "_content".
     * 
     * @param content
     *            Value to be added
     */
    public void addContent(String content) {
        if (target == TARGET_W3C_DOC) {
            cur_element.appendChild(doc.createCDATASection(content));
        } else {
            String cur_val = (String) cur_jo.get("_content");
            if (cur_val == null)
                cur_jo.put("_content", content);
            else
                cur_jo.put("_content", cur_val + "," + content);
        }

    }

    /**
     * Traverse to parent element to go up one level
     */
    public void traverseToParent() {
        if (target == TARGET_W3C_DOC) {
            if (cur_element.getNodeType() != Node.DOCUMENT_NODE)
                cur_element = cur_element.getParentNode();
        } else {
            JSONObject parent_ref = (JSONObject) cur_jo.get("parent_ref");
            cur_jo = parent_ref;
        }
    }

    /**
     * Deletes all attributes with name "parent_ref" in all levels in a
     * JSONObject hierarchy recursively.
     */
    public void deleteParentRefs() {
        if (target != TARGET_JSON_OBJ)
            return;
        deleteParentRefsRecursively(jo);
    }

    /**
     * Deletes all attributes with name "parent_ref" in all levels in a
     * JSONObject hierarchy recursively.
     * 
     * @param obj
     *            Initially pass the root object
     */
    private void deleteParentRefsRecursively(JSONObject obj) {
        if (obj.containsKey("parent_ref"))
            obj.remove("parent_ref");
        // Go through each child object or array
        // recursively
        for (Object set : obj.keySet()) {
            String key = (String) set;
            Object child_obj = obj.get(key);
            if (child_obj instanceof JSONArray) {
                JSONArray ja = (JSONArray) child_obj;
                for (Object ele : ja) {
                    if (ele instanceof JSONObject)
                        deleteParentRefsRecursively((JSONObject) ele);
                }
            } else if (child_obj instanceof JSONObject) {
                deleteParentRefsRecursively((JSONObject) child_obj);
            }
        }
    }

    /**
     * Getter for Target type
     * 
     * @return Target type
     */
    public short getTarget() {
        return target;
    }

    /**
     * Getter for W3C Document
     * 
     * @return W3C Document
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * Getter for JSONObject
     * 
     * @return JSONObject
     */
    public JSONObject getJSONObject() {
        return jo;
    }

}
