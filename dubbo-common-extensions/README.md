# Dubbo Common Extensions
[中文](./README_ch.md)

Dubbo common utility class extensions, also providing testing functionalities.

Adding Utils utility class, providing checks for presence of zero-argument constructors and whether a class is provided by JDK.

## How to Use?
### Current Version: 3.2.0

```
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-common-extensions</artifactId>
    <version>${dubbo-api-docs-version}</version>
</dependency>
```
Directly call methods provided by the Utils class:
```
boolean haveZeroArgConstructor = Utils.checkZeroArgConstructor(TestAClass.class)
boolean isJdk = Utils.isJdk(String.class)
```
