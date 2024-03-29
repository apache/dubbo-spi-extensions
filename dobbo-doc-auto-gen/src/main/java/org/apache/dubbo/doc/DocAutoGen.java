/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.doc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class DocAutoGen {
    public static void main(String[] args) throws IOException {
        String filePath = System.getProperty("user.dir");
        File file = new File(filePath);
        int level = 0;
        String parentPath = "";
        System.setOut(new PrintStream(filePath + "/" + "README.md"));
        String title = "# dubbo-spi-extensions";
        System.out.println(title);
        String x = "[![Build Status](https://travis-ci.org/apache/dubbo-spi-extensions.svg?branch=master)](https://travis-ci.org/apache/dubbo-spi-extensions)\n" +
            "[![codecov](https://codecov.io/gh/apache/dubbo-spi-extensions/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/dubbo-spi-extensions)\n" +
            "[![Maven Central](https://img.shields.io/maven-central/v/org.apache.dubbo/dubbo-spi-extensions.svg)](https://search.maven.org/search?q=g:org.apache.dubbo%20AND%20a:dubbo-spi-extensions)\n" +
            "[![GitHub release](https://img.shields.io/github/release/apache/dubbo-spi-extensions.svg)]";
        System.out.println(x);
        System.out.println();
        String chineseFile = "[中文](./README_CN.md)\n";
        System.out.println(chineseFile);
        String description = "The purpose of dubbo-spi-extensions is to provide open, community-driven, reusable components to build microservice programs with different needs. These components extend the core of the Apache Dubbo project, but they are separated and decoupled.";
        System.out.println(description);

        System.out.println();
        String usage = "Developers can flexibly choose the required extension dependencies to develop microservice programs based on their needs. The available extensions are as follows:Developers can flexibly choose the required extension dependencies to develop microservice programs based on their needs. ";
        System.out.println(usage);
        System.out.println();
        System.out.println("For version release notes, please refer to the documentation:");
        System.out.println("- [Release](https://cn.dubbo.apache.org/zh-cn/download/spi-extensions/)");
        System.out.println("- [Reference](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/reference-manual/spi/overview/)");
        System.out.println();

        String asFollow = "The available extensions are as follows:";
        System.out.println(asFollow);
        System.out.println();

        visitFile(file, parentPath, level);
        System.out.println();
        String contributorTitle = "## Contribution\n";
        String thanks = "Thanks to everyone who has contributed!\n";
        String contributorImg =
            "<a href=\"https://github.com/apache/dubbo-spi-extensions/graphs/contributors\">\n" +
                "  <img src=\"https://contributors-img.web.app/image?repo=apache/dubbo-spi-extensions\" />\n" +
                "</a>\n" ;
        System.out.println(contributorTitle);
        System.out.println();
        System.out.println(thanks);
        System.out.println();
        System.out.println(contributorImg);
        System.out.println();
    }

    public static void visitFile(File file, String parentPath, int level) {
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
                System.out.println(blank + "- [" + name + "]" + "(" + currentPath + ")");
                visitFile(f, currentPath, level + 1);
            }
        }
    }
}
