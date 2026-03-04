package com.stipter.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportUtil {

    public static void setCucumberReport() {

        String tag = System.getProperty("cucumber.filter.tags", "local");
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String plugin =
                "pretty,html:target/cucumber-reports/" + tag + "_" + time + ".html";

        System.setProperty("cucumber.plugin", plugin);
    }
}
