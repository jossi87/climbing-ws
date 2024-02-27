package com.buldreinfo.jersey.jaxb.pdf;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfWriter;

public class LinkInCell implements PdfPCellEvent {
    protected String url;
    
    public LinkInCell(String url) {
        this.url = url;
    }
    
    public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
        PdfWriter writer = canvases[0].getPdfWriter();
        PdfAction action = new PdfAction(url);
        PdfAnnotation link = PdfAnnotation.createLink(writer, position, PdfAnnotation.HIGHLIGHT_INVERT, action);
        writer.addAnnotation(link);
    }
}