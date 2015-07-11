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

import java.util.Hashtable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    static final String generalNSURI = "http://siara.cc/ns";

    Document doc = null;
    Node cur_element = null;
    Element last_element = null;
    String currentElementNS = "";

    JSONObject jo = null;
    JSONObject cur_jo = null;
    JSONObject last_jo = null;

    short target = TARGET_JSON_OBJ;
    String csv_ml_root = null;
    String csv_ml_encoding = null;
    ExceptionHandler ex = null;

    Hashtable<String, String> nsMap = new Hashtable<String, String>();
    Hashtable<String, String> pendingAttributes = new Hashtable<String, String>();

    /**
     * Initialize to build Document object
     * 
     * @param csv_ml_root
     *            Root node name to build initial Document
     * @param ns_uri
     *            Any global namespaces to build initial Document
     * @param ex
     */
    public ParsedObject(String csv_ml_root, String csv_ml_encoding,
            ExceptionHandler ex, short targetObject) {

        this.target = targetObject;
        this.ex = ex;
        this.csv_ml_root = csv_ml_root;
        this.csv_ml_encoding = csv_ml_encoding;
        if (target == TARGET_JSON_OBJ) {
            jo = new JSONObject();
            cur_jo = jo;
            last_jo = cur_jo;
        } else {

            // Look for namespace in the csv_ml_root directive
            // (If namespaces need to be defined at the root they
            // can be enumerated after the root delimited with
            // space).
            String namespace = "";
            String[] ns_uri = new String[0];
            int e_idx = csv_ml_root.indexOf("/");
            if (e_idx > 0) {
                ns_uri = csv_ml_root.substring(e_idx + 1).split(" ");
                csv_ml_root = csv_ml_root.substring(0, e_idx);
            }

            // Check whether root element has a prefix
            String rootNSPrefix = "";
            int cIdx = csv_ml_root.indexOf(":");
            if (cIdx > 0)
                rootNSPrefix = csv_ml_root.substring(0, cIdx);
            boolean rootNSURIFound = false;

            // Build XML String for obtaining initial Document object
            StringBuffer xml_str = new StringBuffer();
            xml_str.append("<").append(csv_ml_root);
            for (int j = 0; j < ns_uri.length; j++) {
                int equalIdx = ns_uri[j].indexOf('=');
                if (equalIdx == -1)
                    break; // error condition. report?
                String ns_prefix = ns_uri[j].substring(0, equalIdx);
                nsMap.put(ns_prefix, ns_uri[j].substring(equalIdx + 1));
                if (ns_prefix.equals(ns_prefix))
                    rootNSURIFound = true;
                xml_str.append(" xmlns:").append(ns_uri[j]);
            }
            // Use General Namespace prefix if one not defined.
            if (rootNSPrefix.length() > 0 && !rootNSURIFound) {
                xml_str.append(" xmlns:").append(rootNSPrefix).append("='")
                        .append(generalNSURI).append("'");
            }
            xml_str.append("></").append(csv_ml_root).append(">");

            // Parse and return document
            doc = Util.parseXMLToDOM(xml_str.toString());
            last_element = doc.getDocumentElement();
            cur_element = last_element;
            this.csv_ml_root = csv_ml_root;
            // TODO: How to set document encoding? Is it automatic?

        }

    }

    /**
     * Associates URI with a namespace prefix
     * 
     * @param nsPrefix
     *            Namespace prefix
     * @param nsURI
     *            URI to be associated with prefix
     * @param ex
     */
    public void setNSMap(String nsPrefix, String nsURI) {
        nsMap.put(nsPrefix, nsURI);
    }

    /**
     * Adds new W3C Element object and makes it current
     * 
     * @param node_name
     *            Name of new element to be created
     */
    private void addNewElement(String node_name) {
        Element new_node = null;
        if (cur_element.getNodeType() == Node.DOCUMENT_NODE) {
            // If given input tries to add more than one root, throw error
            ex.set_err(MultiLevelCSVParser.E_ONLY_ONE_ROOT);
            return;
        } else {
            // If directive specifies the first element as root,
            // do not add new. Otherwise add new Element
            if (!cur_element.equals(doc.getDocumentElement())
                    || !node_name.equals(csv_ml_root)) {
                int cIdx = node_name.indexOf(':');
                if (cIdx != -1) {
                    currentElementNS = node_name.substring(0, cIdx);
                    node_name = node_name.substring(cIdx + 1);
                } else
                    currentElementNS = "";
                new_node = doc.createElement(node_name);
                cur_element.appendChild(new_node);
            }
        }
        if (new_node != null) {
            last_element = new_node;
            cur_element = last_element;
        }
    }

    /**
     * Performas any pending activity against an element to finalize it. In this
     * case, it adds any pending attributes needing namespace mapping.
     * 
     * Also if the node has a namespace attached, it recreates the node with
     * specific URI.
     */
    public void finalizeElement() {
        // Add all remaining attributes
        for (String col_name : pendingAttributes.keySet()) {
            String value = pendingAttributes.get(col_name);
            int cIdx = col_name.indexOf(':');
            String ns = col_name.substring(0, cIdx);
            String nsURI = nsMap.get(ns);
            if (nsURI == null)
                nsURI = generalNSURI;
            Attr attr = doc.createAttributeNS(nsURI,
                    col_name.substring(cIdx + 1));
            attr.setPrefix(ns);
            attr.setValue(value);
            ((Element) cur_element).setAttributeNodeNS(attr);
        }
        // If the element had a namespace prefix, it has to be recreated.
        if (!currentElementNS.equals("")
                && !doc.getDocumentElement().equals(cur_element)) {
            Node parent = cur_element.getParentNode();
            Element cur_ele = (Element) parent.removeChild(cur_element);
            String node_name = cur_ele.getNodeName();
            String nsURI = nsMap.get(currentElementNS);
            if (nsURI == null)
                nsURI = generalNSURI;
            Element new_node = doc.createElementNS(nsURI, currentElementNS+":"+node_name);
            parent.appendChild(new_node);
            // Add all attributes
            NamedNodeMap attrs = cur_ele.getAttributes();
            while (attrs.getLength() > 0) {
                Attr attr = (Attr) attrs.item(0);
                cur_ele.removeAttributeNode(attr);
                nsURI = attr.getNamespaceURI();
                new_node.setAttributeNodeNS(attr);
            }
            // Add all CData sections
            NodeList childNodes = cur_ele.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.CDATA_SECTION_NODE)
                    new_node.appendChild(node);
            }
            cur_element = new_node;
        }
        pendingAttributes = new Hashtable<String, String>();
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
        last_jo = cur_jo;
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
            if (cur_element.getNodeType() == Node.ELEMENT_NODE) {
                int cIdx = col_name.indexOf(':');
                if (cIdx == -1)
                    ((Element) cur_element).setAttribute(col_name, value);
                else {
                    String ns = col_name.substring(0, cIdx);
                    if (ns.equals("xmlns")) {
                        nsMap.put(col_name.substring(cIdx + 1), value);
                    } else {
                        String nsURI = nsMap.get(ns);
                        if (nsURI == null) {
                            pendingAttributes.put(col_name, value);
                        } else {
                            Attr attr = doc.createAttributeNS(nsURI,
                                    col_name.substring(cIdx + 1));
                            attr.setPrefix(ns);
                            attr.setValue(value);
                            ((Element) cur_element).setAttributeNodeNS(attr);
                        }
                    }
                }
            }
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
            if (cur_element.getNodeType() != Node.DOCUMENT_NODE) {
                cur_element = cur_element.getParentNode();
            }
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

    /**
     * Getter for currently constructed object
     * 
     * @return JSONObject
     */
    public JSONObject getCurrentJSO() {
        return last_jo;
    }

    /**
     * Getter for currently constructed object
     * 
     * @return Element
     */
    public Element getCurrentElement() {
        return last_element;
    }

}
