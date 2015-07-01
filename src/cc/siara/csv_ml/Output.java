package cc.siara.csv_ml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cc.siara.csv.CSVParser;

public class Output {

	public static String generate(Document dom) {
	   StringBuffer schema = new StringBuffer("csv_ml,1.0\n");
	   StringBuffer data = new StringBuffer();
	   outputCSVRecursively(dom.getDocumentElement(), schema, data, "");
	   return schema.append("end_schema\n").append(data).toString();
    }
	private static void outputCSVRecursively(Element ele, StringBuffer schema, StringBuffer data, String hierarchy_space_prefix) {
	   String node_name = ele.getNodeName();
	   boolean is_schema_updated = false;
	   if (schema.indexOf("\n"+hierarchy_space_prefix+node_name+",") != -1)
	      is_schema_updated = true;
	   if (ele.getParentNode().getParentNode() != null) {
	      if (!is_schema_updated) schema.append(hierarchy_space_prefix).append(node_name);
	      data.append(hierarchy_space_prefix).append(node_name);
	      NamedNodeMap attributes = ele.getAttributes();
	      for (int j=0; j<attributes.getLength(); j++) {
	         if (!is_schema_updated)
	            schema.append(",").append(attributes.item(j).getNodeName());
	         data.append(",").append(CSVParser.encodeToCSVText(attributes.item(j).getNodeValue()));
	      }
	      String tc = ele.getTextContent();
	      if (tc != null && !tc.equals(""))
	         data.append(",").append(CSVParser.encodeToCSVText(tc));
	      if (!is_schema_updated)
	         schema.append("\n");
	      data.append("\n");
	      hierarchy_space_prefix += " ";
	   }
	   NodeList childNodes = ele.getChildNodes();
	   for (int i=0; i<childNodes.getLength(); i++) {
	      if (childNodes.item(i).getNodeType() != Node.ELEMENT_NODE)
	         continue;
	      outputCSVRecursively((Element)childNodes.item(i), schema, data, hierarchy_space_prefix);
	   }
	}

}
