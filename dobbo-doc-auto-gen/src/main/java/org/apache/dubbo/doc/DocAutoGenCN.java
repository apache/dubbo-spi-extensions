package org.apache.dubbo.doc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class DocAutoGenCN {
    public static void main(String[] args) throws IOException {
        String filePath = System.getProperty("user.dir");
        File file = new File(filePath);
        int level = 0;
        String parentPath = "";
        System.setOut(new PrintStream(filePath + "/" + "README_CN.md"));
        String title = "# dubbo-spi-extensions";
        System.out.println(title);
        String x = "[![Build Status](https://travis-ci.org/apache/dubbo-spi-extensions.svg?branch=master)](https://travis-ci.org/apache/dubbo-spi-extensions)\n" +
            "[![codecov](https://codecov.io/gh/apache/dubbo-spi-extensions/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/dubbo-spi-extensions)\n" +
            "[![Maven Central](https://img.shields.io/maven-central/v/org.apache.dubbo/dubbo-spi-extensions.svg)](https://search.maven.org/search?q=g:org.apache.dubbo%20AND%20a:dubbo-spi-extensions)\n" +
            "[![GitHub release](https://img.shields.io/github/release/apache/dubbo-spi-extensions.svg)]";
        System.out.println(x);
        System.out.println();
        String chineseFile = "[English](./README.md)\n";
        System.out.println(chineseFile);
        String description = "dubbo-spi-extensions的目的是提供开放的、社区驱动的、可重用的组件，用于构建具有不同需求的微服务程序。这些组件扩展了Apache Dubbo项目的核心，但它们是分离的且解耦的。";
        System.out.println(description);

        System.out.println();
        String usage = "开发者可以根据自己的需求灵活选择所需的扩展依赖，开发基于微服务的程序。现有的扩展如下：开发者可以根据自己的需求灵活选择所需的扩展依赖，开发基于微服务的程序。";
        System.out.println(usage);
        System.out.println();
        System.out.println("有关版本发布说明，请参阅文档：");
        System.out.println("- [Release](https://cn.dubbo.apache.org/zh-cn/download/spi-extensions/)");
        System.out.println("- [Reference](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/reference-manual/spi/overview/)");
        System.out.println();

        String asFollow = "现有的扩展如下：";
        System.out.println(asFollow);
        System.out.println();

        DocAutoGen.visitFile(file, parentPath, level);
        System.out.println();
        String contributorTitle = "## 贡献\n";
        String thanks = "感谢所有为此做出贡献的人！\n";
        String contributorImg =
            "<a href=\"https://github.com/apache/dubbo-spi-extensions/graphs/contributors\">\n" +
                "  <img src=\"https://contributors-img.web.app/image?repo=apache/dubbo-spi-extensions\" />\n" +
                "</a>\n";
        System.out.println(contributorTitle);
        System.out.println();
        System.out.println(thanks);
        System.out.println();
        System.out.println(contributorImg);
        System.out.println();
    }

}
