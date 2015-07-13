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

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cc.siara.csv.CSVParser;

/**
 * Constructs csv_ml string from Document object
 * 
 * @author Arundale R.
 * @since 1.0
 */
public class Outputter {

    /**
     * Constructs csv_ml string from Document object
     * 
     * @param dom
     *            Input document
     * @return output csv_ml string
     */
    public static String generate(Document dom) {

        // Initialize schema with default directive. TODO: to handle encoding,
        // root and namespaces
        StringBuffer schema = new StringBuffer("csv_ml,1.0\n");

        StringBuffer data = new StringBuffer();
        outputCSVRecursively(dom.getDocumentElement(), schema, data, "");
        return schema.append("end_schema\n").append(data).toString();

    }

    /**
     * Builds the csv_ml string starting at the root node
     * 
     * @param ele
     *            Initially the root element
     * @param schema
     * @param data
     * @param hierarchy_space_prefix
     */
    private static void outputCSVRecursively(Element ele, StringBuffer schema,
            StringBuffer data, String hierarchy_space_prefix) {

        String node_name = ele.getNodeName();

        boolean is_schema_updated = false;
        if (schema.indexOf("\n" + hierarchy_space_prefix + node_name + ",") != -1)
            is_schema_updated = true;

        // Export node
        // TODO: does not generate data for root node
        if (ele.getParentNode().getParentNode() != null) {

            // Export node
            if (!is_schema_updated)
                schema.append(hierarchy_space_prefix).append(node_name);
            data.append(hierarchy_space_prefix).append(node_name);

            // Export attributes
            NamedNodeMap attributes = ele.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                if (!is_schema_updated)
                    schema.append(",").append(attributes.item(j).getNodeName());
                data.append(",")
                        .append(Util.encodeToCSVText(attributes.item(j)
                                .getNodeValue()));
            }

            // TODO: Is text content to be exported?
            // String tc = ele.getTextContent();
            // if (tc != null && !tc.equals(""))
            // data.append(",").append(encodeToCSVText(tc));

            // Export CData sections
            NodeList childNodes = ele.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i).getNodeType() != Node.CDATA_SECTION_NODE)
                    continue;
                CDATASection cdata_section = (CDATASection) childNodes.item(i);
                data.append(",").append(
                        Util.encodeToCSVText(cdata_section.getData()));
            }
            if (!is_schema_updated)
                schema.append("\n");
            data.append("\n");
            hierarchy_space_prefix += " ";

        }

        // Recurse children
        NodeList childNodes = ele.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() != Node.ELEMENT_NODE)
                continue;
            outputCSVRecursively((Element) childNodes.item(i), schema, data,
                    hierarchy_space_prefix);
        }

    }

}
