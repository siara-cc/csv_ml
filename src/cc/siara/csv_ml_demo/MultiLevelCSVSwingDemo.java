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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
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
import cc.siara.csv_ml.ParsedObject;
import cc.siara.csv_ml.Util;

/**
 * Swing application to demonstrate the features of MultiLevelCSVParser
 * 
 * @author Arundale R.
 */
public class MultiLevelCSVSwingDemo extends JFrame implements ActionListener,
        Runnable {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private static final long serialVersionUID = 5786609711433744144L;
    MultiLevelCSVParser parser = new MultiLevelCSVParser();
    private boolean isInputChanged = false;

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
        container.add(lblDelimiter);
        container.add(cbDelimiter);
        container.add(tfDelimiter);
        container.add(btnAbout);
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
        container.add(lblJDBCURL);
        container.add(tfDBURL);
        container.add(btnRunDDL);
        container.add(lblID);
        container.add(tfID);
        container.add(btnGetData);
        JTextField tfID = new JTextField("1", 4);
        taInput.setBorder(BorderFactory.createLineBorder(getForeground()));
        taOutput.setBorder(BorderFactory.createLineBorder(getForeground()));
        setInputText();
        cbExamples.addActionListener(this);
        cbDelimiter.addActionListener(this);
        btnAbout.addActionListener(this);
        btnXML.addActionListener(this);
        btnDDLDML.addActionListener(this);
        btnJSON.addActionListener(this);
        btnToCSV.addActionListener(this);
        btnXPath.addActionListener(this);
        btnRunDDL.addActionListener(this);
        btnGetData.addActionListener(this);
        taInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent arg0) {
                isInputChanged = true;
            }

            public void keyReleased(KeyEvent arg0) {
            }

            public void keyPressed(KeyEvent arg0) {
            }
        });
        tfInputSize.setEditable(false);
        tfOutputSize.setEditable(false);
        setVisible(true);
    }

    private static final short DM_COMMA = 0;
    private static final short DM_TAB = 1;
    private static final short DM_OTHER = 2;

    /**
     * Sets value to textbox and scrolls to top
     */
    void setDelimiter() {
        int selectedIdx = cbDelimiter.getSelectedIndex();
        switch (selectedIdx) {
        case DM_COMMA:
            parser.setDelimiter(',');
            break;
        case DM_TAB:
            parser.setDelimiter('\t');
            break;
        case DM_OTHER:
            String d = tfDelimiter.getText();
            if (d.length() == 1) {
                char c = d.charAt(0);
                if ("\"\n\r".indexOf(c) == -1)
                    parser.setDelimiter(c);
                else {
                    parser.setDelimiter('\t');
                    cbDelimiter.setSelectedIndex(DM_TAB);
                    JOptionPane.showMessageDialog(null,
                            "Illegal character for delimiter");
                }
            } else {
                parser.setDelimiter('\t');
                cbDelimiter.setSelectedIndex(DM_TAB);
                parser.setDelimiter('\t');
                JOptionPane.showMessageDialog(null,
                        "Delimiter should be single lettered");
            }
            break;
        }
        if (!isInputChanged)
            setInputText();
    }

    /**
     * Sets value to textbox and scrolls to top
     */
    void setInputText() {
        int selectedIdx = cbExamples.getSelectedIndex();
        int delimIdx = cbDelimiter.getSelectedIndex();
        String egString = null;
        switch (delimIdx) {
        case DM_COMMA:
            egString = aExampleCSV[selectedIdx];
            break;
        case DM_TAB:
            egString = aExampleTDV[selectedIdx];
            break;
        case DM_OTHER:
            egString = aExampleTDV[selectedIdx];
            String d = tfDelimiter.getText();
            if (d.length() == 1) {
                egString = egString.replace('\t', d.charAt(0));
            } else {
                egString = aExampleCSV[selectedIdx];
                tfDelimiter.setText(",");
            }
            break;
        }
        taInput.setText(egString);
        try {
            // Load schema corresponding to Example so that Retrieve button can
            // be clicked
            parser.initParse(
                    ParsedObject.TARGET_W3C_DOC,
                    new cc.siara.csv_ml.InputSource(new StringReader(egString)),
                    false);
            String selectedItem = cbExamples.getSelectedItem().toString();
            String exampleSectionNumber = selectedItem.substring(0,
                    selectedItem.indexOf(':'));
            tfDBURL.setText("jdbc:sqlite:test" + exampleSectionNumber + ".db");
            if (exampleSectionNumber.equals("1.5"))
                tfID.setText("1,1");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "IOException");
            e.printStackTrace();
        }
        tfInputSize.setText(String.valueOf(egString.length()));
        taInput.setCaretPosition(0);
        tfXPath.setText(aExampleXPath[selectedIdx]);
        isInputChanged = false;
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
        Document doc = null;
        try {
            InputStream is = new ByteArrayInputStream(taInput.getText()
                    .getBytes());
            doc = parser.parseToDOM(is, false);
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
        Document doc = null;
        try {
            InputStream is = new ByteArrayInputStream(taInput.getText()
                    .getBytes());
            doc = parser.parseToDOM(is, false);
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

    /**
     * Runs each statement in the Output textbox
     */
    void runDDL() {
        Connection con = null;
        try {
            con = DriverManager.getConnection(tfDBURL.getText());
            String out_str = DBBind.runStatements(taOutput.getText(), con);
            taOutput.setText(out_str);
            if (out_str.indexOf("\nERROR:") != -1) {
                JOptionPane
                        .showMessageDialog(null,
                                "Error(s) found. Please check Output box for error messages");
            }
            tfOutputSize.setText(String.valueOf(out_str.length()));
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "SQL Exception" + e.getMessage());
            return;
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runs each statement in the Output textbox
     */
    void runSQL() {

        if (parser.getSchema().getSeqPathNodeMap().size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "No schema. First parse a valid csv_ml");
            return;
        }
        Connection con = null;
        try {
            con = DriverManager.getConnection(tfDBURL.getText());
            StringBuffer out_str = new StringBuffer();
            String id = tfID.getText();
            String[] arr_id = null;
            if (id.indexOf(',') == -1)
                arr_id = new String[] { id };
            else
                arr_id = id.split(",");
            DBBind.generateSQL(parser.getSchema(), out_str, con, arr_id);
            String end_schema = "end_schema\n";
            if (out_str.lastIndexOf(end_schema) == out_str.length()
                    - end_schema.length()) {
                JOptionPane.showMessageDialog(null,
                        "No data found.  Please run DDL/DML first");
            } else {
                taInput.setText(out_str.toString());
                tfInputSize.setText(String.valueOf(out_str.length()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "SQL Exception" + e.getMessage());
            return;
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        Object src_obj = e.getSource();
        if (src_obj.equals(cbExamples))
            setInputText();
        if (src_obj.equals(cbDelimiter))
            setDelimiter();
        if (src_obj.equals(btnXML))
            toXML();
        if (src_obj.equals(btnJSON))
            toJSON();
        if (src_obj.equals(btnXPath))
            processXPath();
        if (src_obj.equals(btnDDLDML))
            toDDLDML();
        if (src_obj.equals(btnToCSV))
            xmlToCSV();
        if (src_obj.equals(btnRunDDL))
            runDDL();
        if (src_obj.equals(btnGetData))
            runSQL();
        if (src_obj.equals(btnAbout)) {
            JOptionPane.showMessageDialog(null,
                    "Multi-level (nested) CSV Demo Application\n\n"
                            + "(c) Siara Logics (cc)\n"
                            + "http://siara.cc/csv_ml\n\n"
                            + "License: Apache License 2.0"
                            );
        }
    }

    // List of example titles (of csv_ml) corresponding to documentation
    String[] aDelimiter = new String[] { "Comma", "Tab", "Other:" };

    // List of example titles (of csv_ml) corresponding to documentation
    String[] aExamples = new String[] { "1.1: Conventional Table data",
            "1.2: Table data without Header",
            "1.3: Table data with Header and Node name",
            "1.4: Table data with Header and Node index",
            "1.5: Multiple nodes under root", "2.1: Multiple level data",
            "2.2: Multiple level data with siblings",
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

    // List of example csv_ml corresponding to documentation in CSV format
    String[] aExampleCSV = new String[] {
            "name,subject,marks\nabc,physics,53\nabc,chemistry,65\nxyz,physics,73\nxyz,chemistry,76",
            "csv_ml,1.0,UTF-8,root,no_node_name,no_schema\nabc,physics,53\nabc,chemistry,65\nxyz,physics,73\nxyz,chemistry,76",
            "csv_ml,1.0,UTF-8,root,with_node_name,inline\nstudent,name,subject,marks\nend_schema\nstudent,abc,physics,53\nstudent,abc,chemistry,65\nstudent,xyz,physics,73\nstudent,xyz,chemistry,76",
            "csv_ml,1.0\nstudent,name,subject,marks\n1,abc,physics,53\n1,abc,chemistry,65\n1,xyz,physics,73\n1,xyz,chemistry,76",
            "csv_ml,1.0\nstudent,name,subject,marks\nfaculty,name,subject\n1,abc,physics,53\n1,abc,chemistry,65\n1,xyz,physics,73\n1,xyz,chemistry,76\n2,pqr,physics\n2,bcd,chemistry",
            "csv_ml,1.0\nstudent,name,age\n education,course_name,year_passed\n  subject,name,marks\n1,abc,24\n 1,bs,2010\n  1,physics,53\n  1,chemistry,65\n 1,ms,2012\n  1,physics,74\n  1,chemistry,75\n1,xyz,24\n 1,bs,2010\n  1,physics,67\n  1,chemistry,85",
            "csv_ml,1.0\nstudent,name,age\n education,course_name,year_passed\n  subject,name,marks\n reference,name,company,designation\n1,abc,24\n 1,bs,2010\n  1,physics,53\n  1,chemistry,65\n 1,ms,2012\n  1,physics,74\n  1,chemistry,75\n 2,pqr,bbb,executive\n 2,mno,bbb,director\n1,xyz,24\n 1,bs,2010\n  1,physics,67\n  1,chemistry,85",
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
            "csv_ml,1.0\nstudent,name,subject,marks,birth_date()date,join_date_time()datetime\n1,abc,physics,53.34,19820123,20140222093000\n1,xyz,physics,73.5,19851112,20140224154530",
            "csv_ml,1.0\nstudent,id,name,subject,marks\n1,,abc,physics,53\n1,,abc,chemistry,54\n1,3,xyz,physics,73\n1,*4,xyz,physics,73",
            "csv_ml,1.0\nstudent,name,age\n education,course_name,year_passed\n reference,name,company,designation\n1,abc,24\n 1,bs,2010\n 1,ms,2012\n 2,pqr,bbb,executive\n 2,mno,bbb,director's secretary" };

    // List of example csv_ml corresponding to documentation in Tab delimited
    // format
    String[] aExampleTDV = new String[] {
            "name\tsubject\tmarks\nabc\tphysics\t53\nabc\tchemistry\t65\nxyz\tphysics\t73\nxyz\tchemistry\t76",
            "csv_ml\t1.0\tUTF-8\troot\tno_node_name\tno_schema\nabc\tphysics\t53\nabc\tchemistry\t65\nxyz\tphysics\t73\nxyz\tchemistry\t76",
            "csv_ml\t1.0\tUTF-8\troot\twith_node_name\tinline\nstudent\tname\tsubject\tmarks\nend_schema\nstudent\tabc\tphysics\t53\nstudent\tabc\tchemistry\t65\nstudent\txyz\tphysics\t73\nstudent\txyz\tchemistry\t76",
            "csv_ml\t1.0\nstudent\tname\tsubject\tmarks\n1\tabc\tphysics\t53\n1\tabc\tchemistry\t65\n1\txyz\tphysics\t73\n1\txyz\tchemistry\t76",
            "csv_ml\t1.0\nstudent\tname\tsubject\tmarks\nfaculty\tname\tsubject\n1\tabc\tphysics\t53\n1\tabc\tchemistry\t65\n1\txyz\tphysics\t73\n1\txyz\tchemistry\t76\n2\tpqr\tphysics\n2\tbcd\tchemistry",
            "csv_ml\t1.0\nstudent\tname\tage\n education\tcourse_name\tyear_passed\n  subject\tname\tmarks\n1\tabc\t24\n 1\tbs\t2010\n  1\tphysics\t53\n  1\tchemistry\t65\n 1\tms\t2012\n  1\tphysics\t74\n  1\tchemistry\t75\n1\txyz\t24\n 1\tbs\t2010\n  1\tphysics\t67\n  1\tchemistry\t85",
            "csv_ml\t1.0\nstudent\tname\tage\n education\tcourse_name\tyear_passed\n  subject\tname\tmarks\n reference\tname\tcompany\tdesignation\n1\tabc\t24\n 1\tbs\t2010\n  1\tphysics\t53\n  1\tchemistry\t65\n 1\tms\t2012\n  1\tphysics\t74\n  1\tchemistry\t75\n 2\tpqr\tbbb\texecutive\n 2\tmno\tbbb\tdirector\n1\txyz\t24\n 1\tbs\t2010\n  1\tphysics\t67\n  1\tchemistry\t85",
            "csv_ml\t1.0\nstudent\tname\tage\n1\ta\n1\tb\t23\tHis record is remarkable\n1\tc\t24\tHis record is remarkable\tHis performance is exemplary",
            "csv_ml\t1.0\nstudent\n name\n age\n1\n 1\ta\n 2\t23",
            "csv_ml\t1.0\nsample\ttext\n1\tNo quote\n1\t No quote with preceding space\n1\tWith quote (\")\n1\t\"With quotes\t and \"\"comma\"\"\"\n1\t \"With quotes\t (space ignored)\"\n1\t \"\"\"Enclosed\t with double quote\"\"\"\n1\t \"\"\"Single\t preceding double quote\"\n1\t \"Double quote\t suffix\"\"\"\n1\t \"Double quote\t (\"\") in the middle\"\n1\t \"More\n\nthan\n\none\n\nline\"",
            "/* You can have comments anywhere\t\n   even at the beginning\n*/\ncsv_ml\t1.0\n\n/* And empty lines like this */\n\nsample\ttext1\ttext2\n1\t/* This is a comment */ \"hello\"\t \"world\" /* End of line comment */\n1\t/* This is also a comment */\t \"/* But this isn't */\"\n\n1\t\"third\"\t \"line\" /* Multiline\ncomment */\n/* Comment at beginning of line */1\t \"fourth\" \t \"line\"",
            "csv_ml\t1.0\tUTF-8\tdata\nstudent\tname\tage\n1\ta\t24",
            "csv_ml\t1.0\tUTF-8\tstudent\nstudent\tname\tage\n1\ta\t24",
            "csv_ml\t1.0\tUTF-8\tstudent\nstudent\tname\tage\n1\ta\t24\n1\tb\t35",
            "csv_ml\t1.0\tUTF-8\tstudent\nstudent\tname\tage\nfaculty\tname\tage\n1\ta\t24\n2\tb\t45",
            "csv_ml\t1.0\nour:student\this:name\tage\txmlns:his\txmlns:our\n1\ta\t24\thttp://siara.cc/his\thttp://siara.cc/our\n1\tb\t26\thttp://siara.cc/his\thttp://siara.cc/our",
            "csv_ml\t1.0\tUTF-8\troot/our='http://siara.cc/our' his='http://siara.cc/his'\nour:student\this:name\tage\n1\ta\t24\n1\tb\t26",
            "csv_ml\t1.0\tUTF-8\txsl:stylesheet/xsl='http://www.w3.org/1999/XSL/Transform'\nxsl:stylesheet\tversion\n xsl:template\tmatch\n  xsl:value-of\tselect\n1\t1.0\n 1\t//student\n  1\t@name\n  1\t@age",
            "csv_ml\t1.0\tUTF-8\txsl:stylesheet/xsl='http://www.w3.org/1999/XSL/Transform'\n01\txsl:value-of\tselect\n02\txsl:for-each\tselect\n 01\nxsl:stylesheet\tversion\n xsl:template\tmatch\n  01\t02\n1\t1.0\n 1\t//student\n  01\t@name\n  01\t@age\n  02\teducation\n   01\t@course_name\n   01\t@year_passed",
            "csv_ml\t1.0\nstudent\tname(40)text\tsubject(30)text\tmarks(3)integer\n1\tabc\tphysics\t53\n1\txyz\tphysics\t73",
            "csv_ml\t1.0\nstudent\tname(40)text\tsubject(30)text=physics\tmarks(3)integer\n1\tabc\tmaths\t53\n1\txyz\tchemistry\t73",
            "csv_ml\t1.0\nstudent\tname(40)text\tnick(30)text=null\tsubject(30)text\tmarks(3)integer\n1\tabc\tpqr\tphysics\t53\n1\txyz\t\tphysics\t73",
            "csv_ml\t1.0\nstudent\tname(40)text\tnick(30)text=\tsubject(30)text\tmarks(3)integer\n1\tabc\tpqr\tphysics\t53\n1\txyz\t\tphysics\t73",
            "csv_ml\t1.0\nstudent\tname(40)text\tsubject(30)text\t\"marks(6,2)numeric\"\n1\tabc\tphysics\t53.34\n1\txyz\tphysics\t73.5",
            "csv_ml\t1.0\nstudent\tname\tsubject\tmarks\tbirth_date()date\tjoin_date_time()datetime\n1\tabc\tphysics\t53.34\t19820123\t20140222093000\n1\txyz\tphysics\t73.5\t19851112\t20140224154530",
            "csv_ml\t1.0\nstudent\tid\tname\tsubject\tmarks\n1\t\tabc\tphysics\t53\n1\t\tabc\tchemistry\t54\n1\t3\txyz\tphysics\t73\n1\t*4\txyz\tphysics\t73",
            "csv_ml\t1.0\nstudent\tname\tage\n education\tcourse_name\tyear_passed\n reference\tname\tcompany\tdesignation\n1\tabc\t24\n 1\tbs\t2010\n 1\tms\t2012\n 2\tpqr\tbbb\texecutive\n 2\tmno\tbbb\tdirector's secretary" };

    String[] aExampleXPath = new String[] {
            "concat('Total of xyz:', sum(root/n1[@name='xyz']/@marks))",
            "concat('Total of xyz:', sum(root/n1[@c1='xyz']/@c3))",
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
            "/xsl:stylesheet", "", "", "", "", "", "", "", "", "", "" };

    // Components
    JLabel lblInput = new JLabel("Input");
    JComboBox<String> cbExamples = new JComboBox<String>(aExamples);
    JLabel lblInputSize = new JLabel("Size");
    JTextField tfInputSize = new JTextField("0", 3);
    JLabel lblDelimiter = new JLabel("Delimiter:");
    JComboBox<String> cbDelimiter = new JComboBox<String>(aDelimiter);
    JTextField tfDelimiter = new JTextField("|", 1);
    JButton btnAbout = new JButton("About");
    JTextArea taInput = new JTextArea(15, 70);
    JScrollPane taInputScroll = new JScrollPane(taInput);
    JLabel lblOutput = new JLabel("Output");
    JButton btnDDLDML = new JButton("DDL/DML");
    JButton btnJSON = new JButton("JSON");
    JButton btnXML = new JButton("XML");
    JButton btnXPath = new JButton("XPath:");
    JTextField tfXPath = new JTextField("", 28);
    JCheckBox cbPretty = new JCheckBox("Pretty?", true);
    JTextArea taOutput = new JTextArea(15, 70);
    JScrollPane taOutputScroll = new JScrollPane(taOutput);
    JButton btnToCSV = new JButton("XML->CSV");
    JLabel lblOutputSize = new JLabel("Output size:");
    JTextField tfOutputSize = new JTextField("0", 3);
    JLabel lblJDBCURL = new JLabel("  DB URL:");
    JTextField tfDBURL = new JTextField("jdbc:sqlite:test.db", 13);
    JButton btnRunDDL = new JButton("Run DDL/DML");
    JLabel lblID = new JLabel("id:");
    JTextField tfID = new JTextField("1", 4);
    JButton btnGetData = new JButton("Retrieve");

    /**
     * Main method to invoke the application
     * 
     * @param args
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(new MultiLevelCSVSwingDemo(null));
    }

    /*
     * Thread that initiates the Swing application No code is required here
     * 
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
    }

}
