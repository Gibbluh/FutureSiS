package com.example.auth1.model;

public enum YearLevel {
    FRESHMAN("1st Year - Freshman"),
    SOPHOMORE("2nd Year - Sophomore"),
    JUNIOR("3rd Year - Junior"),
    SENIOR("4th Year - Senior");
    
    private final String displayName;
    
    YearLevel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}