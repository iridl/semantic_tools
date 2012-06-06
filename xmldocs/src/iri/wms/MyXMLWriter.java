package iri.wms;

import java.io.IOException;
import java.io.OutputStream;

import info.aduna.xml.XMLWriter;

public class MyXMLWriter extends XMLWriter {
	
	public MyXMLWriter(OutputStream arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	private String stylesheetref;
	
    public String getStylesheetRef() {
        return stylesheetref;
    }

    public void setStylesheetRef(String stylesheetref) {
       this.stylesheetref = stylesheetref;
    }

	@Override
    public void startDocument() throws IOException {
        super.startDocument();
        _write("<?xml-stylesheet type=\"text/xsl\"");
        _write(" href=\"" + getStylesheetRef() + "\"");
        _writeLn("?>");
    }
}
