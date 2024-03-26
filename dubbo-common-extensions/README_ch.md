# dubbo common extensions

[English](./README.md)

dubbo 共用工具类扩展,并提供测试功能.

增加Utils工具类，提供是否有无参构造方法和是否是JDK提供类的检查

## 如何使用?

### 当前版本: 3.2.0

```
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-common-extensions</artifactId>
    <version>${dubbo-api-docs-version}</version>
</dependency>
```
直接调用Utils类提供的方法：
```
boolean haveZeroArgConstructor = Utils.checkZeroArgConstructor(TestAClass.class)
boolean isJdk = Utils.isJdk(String.class)
```
