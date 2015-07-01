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

public class Util {

	public static Object parseXMLToDOM(String xml_str) {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder()
					.parse(new InputSource(new StringReader(xml_str)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return doc;
	}

	public static String docToString(Document doc, boolean isPretty) {
		try {
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			if (isPretty) {
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
}
