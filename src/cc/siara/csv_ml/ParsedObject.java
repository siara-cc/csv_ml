package cc.siara.csv_ml;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cc.siara.csv.ExceptionHandler;

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

    public ParsedObject(ExceptionHandler ex) {
        this.ex = ex;
        jo = new JSONObject();
        cur_jo = jo;
    }

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

    private void addNewElement(String node_name) {
        Node new_node = null;
        if (cur_element.getNodeType() == Node.DOCUMENT_NODE) {
            ex.set_err(ExceptionHandler.E_ONLY_ONE_ROOT);
            return;
        } else {
            if (!cur_element.equals(doc.getDocumentElement())
                    || !node_name.equals(csv_ml_root)) {
               new_node = doc.createElement(node_name);
               cur_element.appendChild(new_node);
            }
        }
        if (new_node != null)
            cur_element = new_node;
    }

    private void addNewJO(String node_name) {
        JSONObject new_node = null;
        if (cur_jo.get(node_name) == null) {
            cur_jo.put(node_name, new JSONArray());
        }
        JSONArray ja = (JSONArray) cur_jo.get(node_name);
        new_node = new JSONObject();
        new_node.put("parent_ref", cur_jo);
        ja.add(new_node);
        cur_jo = new_node;
    }

    public void addNewNode(String node_name) {
        if (target == TARGET_W3C_DOC)
            addNewElement(node_name);
        else
            addNewJO(node_name);
    }

    public void setAttribute(String col_name, String value) {
        if (target == TARGET_W3C_DOC) {
            if (cur_element.getNodeType() == Node.ELEMENT_NODE
                    && col_name.indexOf("xmlns:") != 0)
                ((Element) cur_element).setAttribute(col_name, value);
        } else
            cur_jo.put(col_name, value);
    }

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

    public void traverseToParent() {
        if (target == TARGET_W3C_DOC) {
           if (cur_element.getNodeType() != Node.DOCUMENT_NODE)
               cur_element = cur_element.getParentNode();
        } else {
           JSONObject parent_ref = (JSONObject) cur_jo.get("parent_ref");
           cur_jo = parent_ref;
        }
    }

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

    public short getTarget() {
        return target;
    }

    public Document getDocument() {
        return doc;
    }

    public JSONObject getJSONObject() {
        return jo;
    }

}
