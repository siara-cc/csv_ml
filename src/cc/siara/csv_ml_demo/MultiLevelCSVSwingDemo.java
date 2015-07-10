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
package cc.siara.csv_ml_demo;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.json.simple.JSONObject;
import org.json.simple.JSONWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import cc.siara.csv_ml.DBBind;
import cc.siara.csv_ml.MultiLevelCSVParser;
import cc.siara.csv_ml.Outputter;
import cc.siara.csv_ml.Util;

/**
 * Swing application to demonstrate the features of MultiLevelCSVParser
 * 
 * @author Arundale R.
 */
public class MultiLevelCSVSwingDemo extends JFrame implements ActionListener,
        Runnable {

    private static final long serialVersionUID = 5786609711433744144L;

    /**
     * Constructor that adds components to the container, sets layout and opens
     * with window
     * 
     * @param container
     */
    public MultiLevelCSVSwingDemo(Container container) {
        if (container == null)
            container = this;
        if (container instanceof JFrame) {
            JFrame frame = (JFrame) container;
            frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            frame.setLocationByPlatform(true);
            frame.setTitle("Demonstration of Multi-level CSV parsing (csv_ml)");
        }
        container.setSize(800, 600);
        container.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
        container.add(lblInput);
        container.add(cbExamples);
        container.add(lblInputSize);
        container.add(tfInputSize);
        container.add(taInputScroll);
        container.add(lblOutput);
        container.add(btnDDLDML);
        container.add(btnJSON);
        container.add(btnXML);
        container.add(btnXPath);
        container.add(tfXPath);
        container.add(cbPretty);
        container.add(taOutputScroll);
        container.add(lblOutputSize);
        container.add(tfOutputSize);
        container.add(btnToCSV);
        taInput.setBorder(BorderFactory.createLineBorder(getForeground()));
        taOutput.setBorder(BorderFactory.createLineBorder(getForeground()));
        setInputText();
        cbExamples.addActionListener(this);
        btnXML.addActionListener(this);
        btnDDLDML.addActionListener(this);
        btnJSON.addActionListener(this);
        btnToCSV.addActionListener(this);
        btnXPath.addActionListener(this);
        tfInputSize.setEditable(false);
        tfOutputSize.setEditable(false);
        setVisible(true);
    }

    /**
     * Sets value to textbox and scrolls to top
     */
    void setInputText() {
        int selectedIdx = cbExamples.getSelectedIndex();
        String egString = aExampleCSV[selectedIdx];
        taInput.setText(egString);
        tfInputSize.setText(String.valueOf(egString.length()));
        taInput.setCaretPosition(0);
        tfXPath.setText(aExampleXPath[selectedIdx]);
    }

    /**
     * Parses string from input textbox and sets to output
     */
    void toXML() {
        Document doc = parseInputToDOM();
        if (doc == null)
            return;
        String xmlString = Util.docToString(doc, cbPretty.isSelected());
        taOutput.setText(xmlString);
        tfOutputSize.setText(String.valueOf(xmlString.length()));
        taOutput.setCaretPosition(0);
    }

    /**
     * Parses string from input textbox into Document object
     * 
     * @return W3C Document object
     */
    private Document parseInputToDOM() {
        MultiLevelCSVParser parser = new MultiLevelCSVParser();
        Document doc = null;
        try {
            doc = parser.parseToDOM(new StringReader(taInput.getText()), false);
            String ex_str = parser.getEx().get_all_exceptions();
            if (ex_str.length() > 0) {
                JOptionPane.showMessageDialog(null, ex_str);
                if (parser.getEx().getErrorCode() > 0)
                    return null;
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        }
        return doc;
    }

    /**
     * Parses input from textbox and generates DDL/DML statements and sets to
     * output text box
     */
    private void toDDLDML() {
        MultiLevelCSVParser parser = new MultiLevelCSVParser();
        Document doc = null;
        try {
            doc = parser.parseToDOM(new StringReader(taInput.getText()), false);
            String ex_str = parser.getEx().get_all_exceptions();
            if (ex_str.length() > 0) {
                JOptionPane.showMessageDialog(null, ex_str);
                if (parser.getEx().getErrorCode() > 0)
                    return;
            }
            StringBuffer out_str = new StringBuffer();
            DBBind.generateDDL(parser.getSchema(), out_str);
            if (out_str.length() > 0) {
                out_str.append("\r\n");
                DBBind.generate_dml_recursively(parser.getSchema(),
                        doc.getDocumentElement(), "", out_str);
            } else {
                out_str.append("No schema");
            }
            taOutput.setText(out_str.toString());
            tfOutputSize.setText(String.valueOf(out_str.length()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Converts XML in output text box back to csv_ml and sets to input text box
     */
    private void xmlToCSV() {
        Document doc = null;
        try {
            doc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(taOutput.getText())));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Could not parse. XML expected in Output text box");
            return;
        }
        String out_str = Outputter.generate(doc);
        taInput.setText(out_str);
        tfInputSize.setText(String.valueOf(out_str.length()));
    }

    /**
     * Evaluates given XPath from Input box against Document generated by
     * parsing csv_ml in input box and sets value or node list to output box.
     */
    private void processXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Document doc = parseInputToDOM();
        if (doc == null)
            return;
        StringBuffer out_str = new StringBuffer();
        try {
            XPathExpression expr = xpath.compile(tfXPath.getText());
            try {
                Document outDoc = Util.parseXMLToDOM("<output></output>");
                Element rootElement = outDoc.getDocumentElement();
                NodeList ret = (NodeList) expr.evaluate(doc,
                        XPathConstants.NODESET);
                for (int i = 0; i < ret.getLength(); i++) {
                    Object o = ret.item(i);
                    if (o instanceof String) {
                        out_str.append(o);
                    } else if (o instanceof Node) {
                        Node n = (Node) o;
                        short nt = n.getNodeType();
                        switch (nt) {
                        case Node.TEXT_NODE:
                        case Node.ATTRIBUTE_NODE:
                        case Node.CDATA_SECTION_NODE: // Only one value gets
                                                      // evaluated?
                            if (out_str.length() > 0)
                                out_str.append(',');
                            if (nt == Node.ATTRIBUTE_NODE)
                                out_str.append(n.getNodeValue());
                            else
                                out_str.append(n.getTextContent());
                            break;
                        case Node.ELEMENT_NODE:
                            rootElement.appendChild(outDoc.importNode(n, true));
                            break;
                        }
                    }
                }
                if (out_str.length() > 0) {
                    rootElement.setTextContent(out_str.toString());
                    out_str.setLength(0);
                }
                out_str.append(Util.docToString(outDoc, true));
            } catch (Exception e) {
                // Thrown most likely because the given XPath evaluates to a
                // string
                out_str.append(expr.evaluate(doc));
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        taOutput.setText(out_str.toString());
        tfOutputSize.setText(String.valueOf(out_str.length()));
    }

    /**
     * Parses csv_ml from input box to JSON object and sets to output text box
     */
    void toJSON() {
        MultiLevelCSVParser parser = new MultiLevelCSVParser();
        try {
            JSONObject jo = parser.parseToJSO(
                    new StringReader(taInput.getText()), false);
            String ex_str = parser.getEx().get_all_exceptions();
            if (ex_str.length() > 0) {
                JOptionPane.showMessageDialog(null, ex_str);
                if (parser.getEx().getErrorCode() > 0)
                    return;
            }
            String outStr;
            if (cbPretty.isSelected()) {
                JSONWriter jw = new JSONWriter();
                try {
                    jo.writeJSONString(jw);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outStr = jw.toString();
            } else
                outStr = jo.toJSONString();
            taOutput.setText(outStr);
            tfOutputSize.setText(String.valueOf(outStr.length()));
            taOutput.setCaretPosition(0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(cbExamples))
            setInputText();
        if (e.getSource().equals(btnXML))
            toXML();
        if (e.getSource().equals(btnJSON))
            toJSON();
        if (e.getSource().equals(btnXPath))
            processXPath();
        if (e.getSource().equals(btnDDLDML))
            toDDLDML();
        if (e.getSource().equals(btnToCSV))
            xmlToCSV();
    }

    // List of example titles (of csv_ml) corresponding to documentation
    String[] aExamples = new String[] { "1.1: Conventional CSV",
            "1.2: Conventional CSV with Header",
            "1.3: Conventional CSV with Header and Node name",
            "1.4: Conventional CSV with Header and Node index",
            "1.5: Multiple nodes under root", "2.1: Multiple level CSV data",
            "2.2: Multiple level CSV data with siblings",
            "3.1: Node attributes", "3.2: Node content", "3.3: Quote handling",
            "3.4: Inline comments and empty lines",
            "3.5.1: Changing root node",
            "3.5.2: Changing root node - data node as root",
            "3.5.3: Changing root node - error case 1",
            "3.5.4: Changing root node - error case 2",
            "3.6.1: Namespaces (1)", "3.6.2: Namespaces (2)",
            "3.6.3: Namespaces (3)", "3.7: Re-using node definitions",
            "4.1: Schema - Specifying type and length",
            "4.2: Schema - Default value", "4.3.1: Schema - Null values (1)",
            "4.3.2: Schema - Null values (2)",
            "4.4: Schema - Precision and Scale", "4.5: Schema - Date and Time",
            "4.6: Schema - Special column 'id'",
            "4.7: Schema - Special column 'parent_id'" };

    // List of example csv_ml corresponding to documentation
    String[] aExampleCSV = new String[] {
            "abc,physics,53\nabc,chemistry,65\nxyz,physics,73\nxyz,chemistry,76",
            "csv_ml,1.0,UTF-8,root,no_node_name,inline\nname,subject,marks\nabc,physics,53\nabc,chemistry,65\nxyz,physics,73\nxyz,chemistry,76",
            "csv_ml,1.0,UTF-8,root,with_node_name,inline\nstudent,name,subject,marks\nend_schema\nstudent,abc,physics,53\nstudent,abc,chemistry,65\nstudent,xyz,physics,73\nstudent,xyz,chemistry,76",
            "csv_ml,1.0\nstudent,name,subject,marks\n1,abc,physics,53\n1,abc,chemistry,65\n1,xyz,physics,73\n1,xyz,chemistry,76",
            "csv_ml,1.0\nstudent,name,subject,marks\nfaculty,name,subject\n1,abc,physics,53\n1,abc,chemistry,65\n1,xyz,physics,73\n1,xyz,chemistry,76\n2,pqr,physics\n2,bcd,chemistry",
            "csv_ml,1.0\nstudent,name,age\n education,course_name,year_passed\n  subject,name,marks\n1,abc,24\n 1,bs,2010\n  1,physics,53\n  1,chemistry,65\n 1,ms,2012\n  1,physics,74\n  1,chemistry,75\n1,xyz,24\n 1,bs,2010\n  1,physics,67\n  1,chemistry,85",
            "csv_ml,1.0\nstudent,name,age\n education,course_name,year_passed\n  subject,name,marks\n references,name,company,designation\n1,abc,24\n 1,bs,2010\n  1,physics,53\n  1,chemistry,65\n 1,ms,2012\n  1,physics,74\n  1,chemistry,75\n 2,pqr,bbb,executive\n 2,mno,bbb,director\n1,xyz,24\n 1,bs,2010\n  1,physics,67\n  1,chemistry,85",
            "csv_ml,1.0\nstudent,name,age\n1,a\n1,b,23,His record is remarkable\n1,c,24,His record is remarkable,His performance is exemplary",
            "csv_ml,1.0\nstudent\n name\n age\n1\n 1,a\n 2,23",
            "csv_ml,1.0\nsample,text\n1,No quote\n1, No quote with preceding space\n1,With quote (\")\n1,\"With quotes, and \"\"comma\"\"\"\n1, \"With quotes, (space ignored)\"\n1, \"\"\"Enclosed, with double quote\"\"\"\n1, \"\"\"Single, preceding double quote\"\n1, \"Double quote, suffix\"\"\"\n1, \"Double quote, (\"\") in the middle\"\n1, \"More\n\nthan\n\none\n\nline\"",
            "/* You can have comments anywhere,\n   even at the beginning\n*/\ncsv_ml,1.0\n\n/* And empty lines like this */\n\nsample,text1,text2\n1,/* This is a comment */ \"hello\", \"world\" /* End of line comment */\n1,/* This is also a comment */, \"/* But this isn't */\"\n\n1,\"third\", \"line\" /* Multiline\ncomment */\n/* Comment at beginning of line */1, \"fourth\" , \"line\"",
            "csv_ml,1.0,UTF-8,data\nstudent,name,age\n1,a,24",
            "csv_ml,1.0,UTF-8,student\nstudent,name,age\n1,a,24",
            "csv_ml,1.0,UTF-8,student\nstudent,name,age\n1,a,24\n1,b,35",
            "csv_ml,1.0,UTF-8,student\nstudent,name,age\nfaculty,name,age\n1,a,24\n2,b,45",
            "csv_ml,1.0\nour:student,his:name,age,xmlns:his,xmlns:our\n1,a,24,http://siara.cc/his,http://siara.cc/our\n1,b,26,http://siara.cc/his,http://siara.cc/our",
            "csv_ml,1.0,UTF-8,root/our='http://siara.cc/our' his='http://siara.cc/his'\nour:student,his:name,age\n1,a,24\n1,b,26",
            "csv_ml,1.0,UTF-8,xsl:stylesheet/xsl='http://www.w3.org/1999/XSL/Transform'\nxsl:stylesheet,version\n xsl:template,match\n  xsl:value-of,select\n1,1.0\n 1,//student\n  1,@name\n  1,@age",
            "csv_ml,1.0,UTF-8,xsl:stylesheet/xsl='http://www.w3.org/1999/XSL/Transform'\n01,xsl:value-of,select\n02,xsl:for-each,select\n 01\nxsl:stylesheet,version\n xsl:template,match\n  01,02\n1,1.0\n 1,//student\n  01,@name\n  01,@age\n  02,education\n   01,@course_name\n   01,@year_passed",
            "csv_ml,1.0\nstudent,name(40)text,subject(30)text,marks(3)integer\n1,abc,physics,53\n1,xyz,physics,73",
            "csv_ml,1.0\nstudent,name(40)text,subject(30)text=physics,marks(3)integer\n1,abc,maths,53\n1,xyz,chemistry,73",
            "csv_ml,1.0\nstudent,name(40)text,nick(30)text=null,subject(30)text,marks(3)integer\n1,abc,pqr,physics,53\n1,xyz,,physics,73",
            "csv_ml,1.0\nstudent,name(40)text,nick(30)text=,subject(30)text,marks(3)integer\n1,abc,pqr,physics,53\n1,xyz,,physics,73",
            "csv_ml,1.0\nstudent,name(40)text,subject(30)text,\"marks(6,2)numeric\"\n1,abc,physics,53.34\n1,xyz,physics,73.5",
            "csv_ml,1.0\nstudent,name,subject,marks,birth_date()date,join_date_time()datetime\n1,abc,physics,53.34,1982-01-23,2014-02-22 09:30:00\n1,xyz,physics,73.5,1985-11-12,2014-02-24 15:45:30",
            "csv_ml,1.0\nstudent,id,name,subject,marks\n1,,abc,physics,53\n1,,abc,chemistry,54\n1,3,xyz,physics,73\n1,*4,xyz,physics,73",
            "csv_ml,1.0\nstudent,name,age\n education,course_name,year_passed\n references,name,company,designation\n1,abc,24\n 1,bs,2010\n 1,ms,2012\n 2,pqr,bbb,executive\n 2,mno,bbb,director's secretary" };

    String[] aExampleXPath = new String[] {
            "concat('Total of xyz:', sum(root/n1[@c1='xyz']/@c3))",
            "concat('Total of xyz:', sum(root/n1[@name='xyz']/@marks))",
            "concat('Total of xyz:', sum(root/student[@name='xyz']/@marks))",
            "concat('Total of xyz:', sum(root/student[@name='xyz']/@marks))",
            "concat('Total of xyz:', sum(root/student[@name='xyz']/@marks))",
            "/root/student[education/subject/@marks > 80]",
            "/root/student[education/subject/@marks > 80]",
            "/root/student[@name='c']/text()", "/root/student/name/text()",
            "/root/sample[3]/@text", "/root/sample[2]/@text2",
            "/data/student/@name", "/student/@name", "/root", "/root",
            "/root/our:student[his:name='b']",
            "/root/our:student[his:name='b']", "/xsl:stylesheet",
            "/xsl:stylesheet", "","","","","","","","","","" };

    // Components
    JLabel lblInput = new JLabel("Input (csv):");
    JComboBox<String> cbExamples = new JComboBox<String>(aExamples);
    JLabel lblInputSize = new JLabel("Input size:");
    JTextField tfInputSize = new JTextField("0", 4);
    JTextArea taInput = new JTextArea(15, 70);
    JScrollPane taInputScroll = new JScrollPane(taInput);
    JLabel lblOutput = new JLabel("Output (csv):");
    JButton btnDDLDML = new JButton("DDL/DML");
    JButton btnJSON = new JButton("JSON");
    JButton btnXML = new JButton("XML");
    JButton btnXPath = new JButton("Apply XPath:");
    JTextField tfXPath = new JTextField("", 20);
    JCheckBox cbPretty = new JCheckBox("Pretty?", true);
    JTextArea taOutput = new JTextArea(15, 70);
    JScrollPane taOutputScroll = new JScrollPane(taOutput);
    JButton btnToCSV = new JButton("XML to CSV");
    JLabel lblOutputSize = new JLabel("Output size:");
    JTextField tfOutputSize = new JTextField("0", 4);

    /**
     * Main method to invoke the application
     * 
     * @param args
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(new MultiLevelCSVSwingDemo(null));
    }

    /* 
     * Thread that initiates the Swing application
     * No code is required here
     * 
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
    }

}
