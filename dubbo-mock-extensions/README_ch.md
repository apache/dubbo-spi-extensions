# Dubbo Mock Extension

[English](README.md)
> Dubbo Mock Extension是提供给[dubbo](https://github.com/apache/dubbo)用户用于在没有服务提供者的情况下模拟返回数据的模块，只需要配合[dubbo-admin](https://github.com/apache/dubbo-admin)就可以在没有服务提供者的情况下轻松模拟返回数据。
> 在开发过程中无需等待服务提供方将服务提供者实现开发和部署完成才能进行后续开发和测试，减少对服务提供者的依赖造成的阻塞，提升开发效率。

## 如何使用

- 引入依赖

```xml
<denpendency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-mock-admin</artifactId>
    <version>last</version>
</denpendency>
```

- 开启模拟返回开关 将``` -Denable.dubbo.admin.mock=true ```添加到JVM启动参数中。

- 配置模拟规则 在[dubbo-admin](https://github.com/apache/dubbo-admin)的服务Mock菜单栏进行模拟规则的配置。
