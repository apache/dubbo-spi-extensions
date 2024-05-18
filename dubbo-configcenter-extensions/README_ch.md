# Dubbo 配置中心扩展

[English](README.md)
> Dubbo Configcenter Extension是提供给[dubbo](https://github.com/apache/dubbo)支持的配置中心之外的如consul、etcd等注册中心模块的适配。

## 集成示例

### Consul


#### interface端添加
```java
public interface DemoService {

    String sayHello(String name);

}

```
#### provider端添加配置
- 引入依赖

```xml
<dependency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-configcenter-consul</artifactId>
    <version>3.2.0</version>
</dependency>
```
- `spring/configcenter-provider.xml`
```xml
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans" xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
       http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    <context:property-placeholder/>

    <dubbo:application name="dubbo-configcenter-consul-provider"/>

    <dubbo:config-center address="consul://${consul.address:localhost}:8500"/>**

    <bean id="demoService" class="org.apache.dubbo.samples.configcenter.impl.DemoServiceImpl"/>

    <dubbo:service interface="org.apache.dubbo.samples.configcenter.api.DemoService" ref="demoService"/>
</beans>
```
- 实现接口
```java
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name +
                ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress();
    }

}

```

- 暴露服务
```java
public class ConsulProvider {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/configcenter-provider.xml"});
        context.start();

        System.out.println("dubbo service started");
        new CountDownLatch(1).await();
    }

}

```

#### consumer端添加配置
- 引入依赖

```xml
<dependency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-configcenter-consul</artifactId>
    <version>3.2.0</version>
</dependency>
```
- `spring/configcenter-consumer.xml`
```xml
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans" xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
       http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    <context:property-placeholder/>

    <dubbo:application name="dubbo-configcenter-consul-consumer"/>

    <dubbo:config-center highest-priority="false" protocol="consul" address="${consul.address:localhost}:8500"/>

    <dubbo:reference id="demoService" interface="org.apache.dubbo.samples.configcenter.api.DemoService"/>

</beans>
```
- 调用服务
```java
public class ConsulConsumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/configcenter-consumer.xml"});
        context.start();
        DemoService demoService = context.getBean("demoService", DemoService.class);

        String hello = demoService.sayHello("world");
        System.out.println(hello);

    }
}

```


### Etcd


#### interface端添加
```java
public interface DemoService {

    String sayHello(String name);

}

```
#### provider端添加配置
- 引入依赖

```xml
<dependency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-configcenter-etcd</artifactId>
    <version>3.2.0</version>
</dependency>
```
- `spring/configcenter-provider.xml`
```xml
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans" xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
       http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    <context:property-placeholder/>

    <dubbo:application name="dubbo-configcenter-etcd-provider"/>

    <dubbo:config-center address="etcd3://${etcd.address:localhost}:2379"/>**

    <bean id="demoService" class="org.apache.dubbo.samples.configcenter.impl.DemoServiceImpl"/>

    <dubbo:service interface="org.apache.dubbo.samples.configcenter.api.DemoService" ref="demoService"/>
</beans>
```
- 实现接口
```java
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name +
                ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress();
    }

}

```

- 暴露服务
```java
public class EtcdProvider {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/configcenter-provider.xml"});
        context.start();

        System.out.println("dubbo service started");
        new CountDownLatch(1).await();
    }

}

```

#### consumer端添加配置
- 引入依赖

```xml
<dependency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-configcenter-consul</artifactId>
    <version>3.2.0</version>
</dependency>
```
- `spring/configcenter-consumer.xml`
```xml
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans" xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
       http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    <context:property-placeholder/>

    <dubbo:application name="dubbo-configcenter-etcd-consumer"/>

    <dubbo:config-center highest-priority="false" protocol="etcd3" address="${etcd.address:localhost}:2379"/>

    <dubbo:reference id="demoService" interface="org.apache.dubbo.samples.configcenter.api.DemoService"/>

</beans>
```
- 调用服务
```java
public class EtcdConsumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/configcenter-consumer.xml"});
        context.start();
        DemoService demoService = context.getBean("demoService", DemoService.class);

        String hello = demoService.sayHello("world");
        System.out.println(hello);

    }
}

```
