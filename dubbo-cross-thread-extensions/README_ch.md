# Dubbo 跨线程扩展

[English](./README.md)

`dubbo-cross-thread-extensions` 轻量级地复制了 dubbo.tag 的跨线程功能。
它可以与 SkyWalking 和 TTL 一起运行。

## 集成示例
### 使用 Byte Buddy 扫描注解
(您可以使用 ByteBuddyAgent 安装或者使用 `-javaagent=<agentjar>` 进行使用)
```
        Instrumentation instrumentation = ByteBuddyAgent.install();
        RunnableOrCallableActivation.install(instrumentation);
        RpcContext.getClientAttachment().setAttachment(CommonConstants.TAG_KEY, tag);
        Callable<String> callable = CallableWrapper.of(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return RpcContext.getClientAttachment().getAttachment(CommonConstants.TAG_KEY);
            }
        });
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        Future<String> result = threadPool.submit(callable);
```
### 添加注解 @DubboCrossThread

```
@DubboCrossThread
public class TargetClass implements Runnable{
    @Override
    public void run() {
        // ...
    }
}
```
### 包装 Callable 或 Runnable
```
Callable<String> callable = CallableWrapper.of(new Callable<String>() {
    @Override
    public String call() throws Exception {
        return null;
    }
});
```
```
Runnable runnable = RunnableWrapper.of(new Runnable() {
    @Override
    public void run() {
        // ...
    }
});
```
## 与 Spring Boot 集成

### 添加一个监听器
```
public class DubboCrossThreadAnnotationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private Logger logger = LoggerFactory.getLogger(DubboCrossThreadAnnotationListener.class);
    private Instrumentation instrumentation;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
        RunnableOrCallableActivation.install(this.instrumentation);
        logger.info("finished byte buddy installation.");
    }

    public DubboCrossThreadAnnotationListener(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    private DubboCrossThreadAnnotationListener() {

    }
}

```
### 安装 ByteBuddyAgent
```
@SpringBootApplication
@ComponentScan(basePackages = "org.apache.your-package")
public class SpringBootDemoApplication {

    public static void main(String[] args) {
       SpringApplication application = new SpringApplication(SpringBootDemoApplication.class);
       application.addListeners(new DubboCrossThreadAnnotationListener(ByteBuddyAgent.install()));
       application.run(args);
    }
}
```

## 与 SkyWalking 和 TTL 一起运行
JVM 参数:
```
-javaagent:transmittable-thread-local-2.14.2.jar
-Dskywalking.agent.application_code=tracecallable-ltf1
-Dskywalking.agent.service_name=test12
-Dskywalking.collector.backend_service=172.37.66.195:11800
-javaagent:skywalking-agent.jar
```
示例代码:
```
public class MultiAnnotationWithSwTtl {
    private static TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

    public static void main(String[] args) {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        RunnableOrCallableActivation.install(instrumentation);
        context.set("value-set-in-parent with ttl");
        RunnableWrapper runnable = RunnableWrapper.of(new Runnable() {
            @Override
            public void run() {
                System.out.println("parent thread traceId=" + TraceContext.traceId());
                RpcContext.getClientAttachment().setAttachment("dubbo.tag", "tagValue");
                MyRunnable task = new MyRunnable();
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(task);
            }
        });
        runnable.run();
    }

    @TraceCrossThread // copy traceContext (include traceId)
    @DubboCrossThread
    public static class MyRunnable implements Runnable {

        @Override
        public void run() {
            System.out.println("dubbo.tag=" +  RpcContext.getClientAttachment().getAttachment("dubbo.tag"));
            System.out.println("children thread traceId=" + TraceContext.traceId());
            System.out.println("ttl context.get()="+context.get());
        }

    }
}

```
输出:
```
parent thread traceId=60cfc24e245d4389b9f40b5b38c33ef6.1.16910355654660001
dubbo.tag=tagValue
children thread traceId=60cfc24e245d4389b9f40b5b38c33ef6.1.16910355654660001
ttl context.get()=value-set-in-parent with ttl
```


