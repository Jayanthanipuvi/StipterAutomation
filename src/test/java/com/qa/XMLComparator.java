package com.qa;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.io.File;

public class XMLComparator {
    public static boolean compareXML(File file1, File file2) {
        Diff diff = DiffBuilder.compare(file1)
                .withTest(file2)
                .ignoreWhitespace()
                .checkForSimilar()
                .build();
        return !diff.hasDifferences(); // Returns true if XMLs are similar
    }
}

