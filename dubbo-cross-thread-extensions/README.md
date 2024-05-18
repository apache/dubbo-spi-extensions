# Dubbo Cross Thread Extensions
[中文](./README_ch.md)

`dubbo-cross-thread-extensions` copy dubbo.tag cross thread lightly . 
it can run with skywalking and ttl . 

## Integrate example
### scan annotation by byte-buddy
(you can install with ByteBuddyAgent or use it with `-javaagent=<agentjar>`)
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
### add annotation @DubboCrossThread

```
@DubboCrossThread
public class TargetClass implements Runnable{
    @Override
    public void run() {
        // ...
    }
}
```
### wrap Callable or Runnable
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
## Integrate with spring boot

### add a listener
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
### install ByteBuddyAgent
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

## run with skywalking and ttl
jvm arguments:
```
-javaagent:transmittable-thread-local-2.14.2.jar
-Dskywalking.agent.application_code=tracecallable-ltf1
-Dskywalking.agent.service_name=test12
-Dskywalking.collector.backend_service=172.37.66.195:11800
-javaagent:skywalking-agent.jar
```
example code:
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
output:
```
parent thread traceId=60cfc24e245d4389b9f40b5b38c33ef6.1.16910355654660001
dubbo.tag=tagValue
children thread traceId=60cfc24e245d4389b9f40b5b38c33ef6.1.16910355654660001
ttl context.get()=value-set-in-parent with ttl
```

