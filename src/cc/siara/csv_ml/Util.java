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

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * All miscellaneous static utility functions
 * 
 * @author Arundale R.
 * @since 1.0
 */
public class Util {

    /**
     * Creates a Document object from initial XML given. This is required as it
     * is not possible to instantiate a W3C Document interface using new.
     * 
     * @param xml_str
     * @return
     */
    public static Document parseXMLToDOM(String xml_str) {
        Document doc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml_str)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

    /**
     * Converts W3C Document object to XML String
     * 
     * @param doc
     *            Document object
     * @param isPretty
     *            Whether to indent and add new lines
     * @return XML String
     */
    public static String docToString(Document doc, boolean isPretty) {
        try {
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer();
            if (isPretty) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(
                        "{http://xml.apache.org/xslt}indent-amount", "2");
            }
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            String xmlString = result.getWriter().toString();
            return xmlString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Removes the last number from current sequence path and returns it. If the
     * path is 1.2.1, then this method returns 1.2.
     * 
     * Before removing, it extracts the last number as cur_sibling and
     * increments it, so that it can be used to form the next path
     * 
     * @param cur_sequence_path
     *            Path from which to remove
     * @param csv_ml_schema
     *            From directive - whether schema is present or not
     */
    public static int removeFromSeqPath(StringBuffer cur_sequence_path,
            String csv_ml_schema, String csv_ml_node_name, int cur_sibling) {
        int idx = cur_sequence_path.lastIndexOf(".");
        if (idx == -1) {
            cur_sequence_path.setLength(0);
            // If no schema or no node name indicator is present
            // no siblings would be present
            // In other words, increment cur_sibling
            // only if schema is present or node_name indicator
            // is present.
            if (!csv_ml_schema.equals("no_schema")
                    || csv_ml_node_name.equals("with_node_name"))
                cur_sibling++;
        } else {
            // Same logic as above, except in this case,
            // sibling is extracted from the given path.
            if (!csv_ml_schema.equals("no_schema")
                    || csv_ml_node_name.equals("with_node_name"))
                cur_sibling = 1 + Integer.parseInt(cur_sequence_path.substring(
                        idx + 1).trim());
            cur_sequence_path.setLength(idx);
        }
        return cur_sibling;
    }

    /**
     * Removes the last node from current path and returns it. If the path is
     * a.b.c, then this method returns a.b.
     * 
     * @param cur_path
     */
    public static void removeFromPath(StringBuffer cur_path) {
        int idx = cur_path.lastIndexOf(".");
        if (idx == -1)
            cur_path.setLength(0);
        else
            cur_path.setLength(idx);
    }

    /**
     * Appends number to current sequence path. If the path is 1.2, then this
     * method returns 1.2.1
     * 
     * @param cur_sequence_path
     *            The path to which number to be appended
     * @param cur_sibling
     *            The number to be appended
     */
    public static void addToSeqPath(StringBuffer cur_sequence_path,
            int cur_sibling) {
        if (cur_sequence_path.length() > 0) {
            cur_sequence_path.append(".");
        }
        cur_sequence_path.append(cur_sibling);
    }

    /**
     * Appends node_name to current path. If the path is abc.pqr and node name
     * is xyz then this method makes it abc.pqr.xyz
     * 
     * @param cur_path
     *            The path to which to append
     * @param node_name
     *            The node_name to append
     */
    public static void addToPath(StringBuffer cur_path, String node_name) {
        if (cur_path.length() > 0) {
            cur_path.append(".");
        }
        cur_path.append(node_name);
    }

    /**
     * Checks whether encoding is necessary and encodes by enclosing in double
     * quotes, in which case, any double quotes appearing in data need to be
     * escaped.
     * 
     * @param value
     * @return
     */
    public static String encodeToCSVText(String value) {
        if (value == null)
            return value;
        if (value.indexOf(',') != -1 || value.indexOf('\n') != -1
                || value.indexOf("/*") != -1) {
            if (value.indexOf('"') != -1)
                value = value.replace("\"", "\"\"");
            value = ("\"" + value + "\"");
        }
        return value;
    }

}
