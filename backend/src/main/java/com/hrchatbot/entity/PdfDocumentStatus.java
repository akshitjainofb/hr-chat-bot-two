package com.hrchatbot.entity;

public enum PdfDocumentStatus {
    PROCESSING("Processing"),
    INDEXED("Indexed"),
    FAILED("Failed");
    
    private final String displayName;
    
    PdfDocumentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
