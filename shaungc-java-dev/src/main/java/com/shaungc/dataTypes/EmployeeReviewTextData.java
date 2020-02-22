package com.shaungc.dataTypes;

import com.shaungc.utilities.Logger;
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
        // Logger.debug("mainText:" + mainText);
        // Logger.debug("proText:" + proText);
        // Logger.debug("conText:" + conText);
        Logger.debug("rawParagraphs:" + rawParagraphs);
    }
}
