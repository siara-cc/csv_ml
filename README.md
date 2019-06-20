# Multi-level CSV (CSV_ML)

This is an Advanced Parser for CSV (Comma-separated-value), TSV (Tab-separated-value), TDV (Tab-delimited-value) or even files with custom delimiters such as the Pipe symbol (|).  It supports streams and pull parsing for handling huge data files.  It also supports comments and empty lines within the delimited files for annotation.

In addition to parsing regular tabular formats with or without header, it can also parse structured delimited files and convert them to XML DOM or JSON objects.

This project proposes the idea of using CSV format for defining structured relational data in additional to tabular data. The idea is nick named CSV_ML (Multi-level CSV). CSV_ML attempts to provide a simple unambiguous format for representing structured data that includes schema definition.

# Advantages over XML and JSON

CSV_ML is expected to
- save storage space (about 50% compared to JSON and 60-70% compared to XML)
- increase data transfer speeds
- be faster to parse compared to XML and JSON
- allow full schema definition and validation
- make schema definition simple, lightweight and in-line compared to DTD or XML Schema
- recognize standard data types including text (varchar), integer, real, date, datetime
- allow database binding
- be used in EAI (Application Integration) for import and export of data
- be simpler to parse, allowing data to be available even in low memory devices

# Applications
- Enterprise Application Integration (EAI)
- Lightweight alternative to JSON or XML in Three-tier architecture
- Alternative to XML in transfer of data using AJAX
- Data storage and transfer format for embedded platforms such as Arduino and Raspberry PI.
- Data storage and transfer format for mobile/tablet devices based on Android, Windows or iOS.
- Data transfer format for spreadsheets as Tab delimited values through clipboard or otherwise.
    
For complete documentation and examples, download [Multi-level nested CSV.pdf](http://siara.cc/csv_ml/Multi-level%20nested%20CSV%20TDV.pdf)

# Examples

The examples given in the documentation are available as demo applications:
* For Java Swing demo application (executable jar), download [csv_ml_swing_demo-1.0.0.jar](http://siara.cc/csv_ml/csv_ml_swing_demo-1.0.0.jar)
* For Android demo application, download [csv_ml_android_demo-1.0.0.apk](http://siara.cc/csv_ml/csv_ml_android_demo-1.0.0.apk)
* For online Javascript demo application, [click here](http://siara.cc/csv_ml/csv_ml_js.html)
* For online Java Applet demo application [click here](http://siara.cc/csv_ml/csv_ml_applet_demo.html)

For running Javascript and Java Applet demos, you may have to change security settings in your browser.

The given demos convert between CSV, TDV. XML and JSON (CSV to XML DOM, CSV to JSON, TDV to XML DOM, TDV to JSON, XML to CSV). It is basically a CSV TDV TSV to JSON XML Convertor. It also demonstrates how database binding can be achieved using SQLite db.

# Contact

Create issue here or contact arun@siara.cc for any queries or feedback.
