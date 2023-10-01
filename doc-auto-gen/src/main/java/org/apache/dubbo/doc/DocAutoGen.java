package org.apache.dubbo.doc;

import java.io.*;
import java.util.Arrays;

public class DocAutoGen {
    public static void main(String[] args) throws IOException {
        String filePath = System.getProperty("user.dir");
        File file = new File(filePath);
        int level = 0;
        String parentPath = "";
        System.setOut(new PrintStream(filePath + "/" + "README.md"));
        System.out.println("# dubbo-spi-extensions");
        System.out.println("[![Build Status](https://travis-ci.org/apache/dubbo-spi-extensions.svg?branch=master)](https://travis-ci.org/apache/dubbo-spi-extensions)\n" +
            "[![codecov](https://codecov.io/gh/apache/dubbo-spi-extensions/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/dubbo-spi-extensions)\n" +
            "[![Maven Central](https://img.shields.io/maven-central/v/org.apache.dubbo/dubbo-spi-extensions.svg)](https://search.maven.org/search?q=g:org.apache.dubbo%20AND%20a:dubbo-spi-extensions)\n" +
            "[![GitHub release](https://img.shields.io/github/release/apache/dubbo-spi-extensions.svg)]");
        visitFile(file, parentPath, level);
    }

    private static void visitFile(File file, String parentPath, int level) {
        File[] files = file.listFiles();
        // gen code sort by file name
        Arrays.sort(files, (o1, o2) -> {
            if (o1.isDirectory() && o2.isFile()) {
                return -1;
            }
            if (o1.isFile() && o2.isDirectory()) {
                return 1;
            }
            return o1.getName().compareTo(o2.getName());
        });
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = f.getName();
            if (name.startsWith("dubbo-")) {
                String blank = "";
                for (int j = 0; j < level; j++) {
                    blank += "  ";
                }

                String currentPath = level == 0 ? name : parentPath + "/" + name;
                System.out.println(blank + "- [" + currentPath + "]" + "(" + name + ")");
                visitFile(f, currentPath, level + 1);
            }
        }
    }
}
