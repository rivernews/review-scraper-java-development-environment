package com.shaungc.dataTypes;

import java.util.ArrayList;
import java.util.List;

import com.shaungc.utilities.Logger;


/**
 * ReviewTextData
 */
public class EmployeeReviewTextData {
    public List<String> rawParagraphs = new ArrayList<String>();
    public String mainText = "";
    public String proText = "";
    public String conText = "";

    public void debug() {
        // Logger.info("mainText:" + mainText);
        // Logger.info("proText:" + proText);
        // Logger.info("conText:" + conText);
        Logger.info("rawParagraphs:" + rawParagraphs);
    }
}