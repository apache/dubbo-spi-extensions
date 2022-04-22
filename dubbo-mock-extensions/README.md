# Dubbo Mock Extension

[中文](README_ch.md)
> Dubbo Mock Extension is a module provided to [dubbo](https://github.com/apache/dubbo) users to simulate returning data without a service provider. It only needs to cooperate with [dubbo-admin](https: //github.com/apache/dubbo-admin) can easily simulate the returned data without a service provider.
> During the development process, there is no need to wait for the service provider to complete the development and deployment of the service provider before subsequent development and testing can be carried out, which reduces the obstruction caused by the dependence on the service provider and improves the development efficiency.

## How to use.

- Add the dependency.

```xml
<denpendency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-mock-admin</artifactId>
    <version>last</version>
</denpendency>
```

- enable the mock flag. Add the ``` -Denable.dubbo.admin.mock=true ``` into your JVM parameter to enable this function.

- config the mock rule. Config mock rule in Dubbo Admin, Service Mock tab.
