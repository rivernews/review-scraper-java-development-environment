package com.shaungc.dataTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * ReviewTextData
 */
public class EmployeeReviewTextData {
    public List<String> rawParagraphs = new ArrayList<String>();
    public String mainText = "";
    public String proText = "";
    public String conText = "";

    public void debug() {
        // System.out.println("mainText:" + mainText);
        // System.out.println("proText:" + proText);
        // System.out.println("conText:" + conText);
        System.out.println("rawParagraphs:" + rawParagraphs);
    }
}