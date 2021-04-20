package io.appform.idman.server.handlebars;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class HandlebarsHelpers {

    private static final String STD_DATE_FORMAT = "dd-MMMMM-yyyy hh:mm:ss aaa";
    public CharSequence progressBarWidth(int total, int count) {
        if(total == 0) return "0%";
        return "" + ((double)count * 100 / total) + "%";
    }
}
