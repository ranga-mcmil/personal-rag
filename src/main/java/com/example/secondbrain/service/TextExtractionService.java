package com.example.secondbrain.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service 
public class TextExtractionService {

    public String extractText(String filename, byte[] content) throws IOException {
        if (filename.toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(content)) {
                return new PDFTextStripper().getText(document);
            }
        }
        return new String(content);
    }
}
