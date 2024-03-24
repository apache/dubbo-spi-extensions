# dubbo WASM api

[English](./README.md)

目前，Dubbo的SPI扩展只能使用Java语言编写，dubbo-wasm模块旨在克服这一限制。

WASM（WebAssembly）字节码设计为以大小和高效加载时间的二进制格式编码。WASM旨在利用各种平台上可用的通用硬件特性，在浏览器中以机器码速度执行。

WASI（WebAssembly系统接口）为在受限沙箱环境中运行的应用程序提供了一个可移植接口，这使得WASM可以在非浏览器环境（如Linux）中运行。它具有可移植性和安全性。

我们使用 [wasmtime-java](https://github.com/kawamuray/wasmtime-java) 在Java中运行WASI，只要语言可以编译成WASM字节码，就可以用于编写dubbo的SPI扩展。

## 模块结构

* dubbo-wasm-api：负责定义基本异常并提供执行WASM字节码的运行时（Dubbo API）
* dubbo-wasm-cluster-api：提供dubbo-cluster模块SPI的WASM版本
* dubbo-wasm-common-api：提供dubbo-common模块SPI的WASM版本
* dubbo-wasm-metrics-api（待办）：提供dubbo-metrics模块SPI的WASM版本
* dubbo-wasm-registry-api：提供dubbo-registry-api模块SPI的WASM版本
* dubbo-wasm-remoting-api（待办）：提供dubbo-remoting-api模块SPI的WASM版本
* dubbo-wasm-rpc-api：提供dubbo-rpc-api模块SPI的WASM版本

## 注意事项

由于WASM对参数类型有严格要求，并且为了简单起见，除了 `java.lang.Long/java.lang.Integer` 之外的类型不用作参数或返回值。

## 如何使用

以下是在rust中实现 `org.apache.dubbo.rpc.cluster.LoadBalance` SPI的示例。


### 1.用其他语言创建子项目

```shell
cargo new --lib your_subproject_name
```

### 2.在lib.rs中添加方法

```rust
// Adding `#[no_mangle]` to prevent the Rust compiler from modifying method names is mandatory
#[no_mangle]
pub unsafe extern "C" fn doSelect(_arg_id: i64) -> i32 {
    1
}
```

###  3.在 `Cargo.toml` 中添加 `[lib]` 并将 `crate-type` 更改为 `["cdylib"]`。最终，你的 `Cargo.toml` 应如下所示：


```toml
[package]
name = "your_subproject_name"
version = "0.1.0"
edition = "2021"

[dependencies]
# ......

[lib]
crate-type = ["cdylib"]
```

### 4.生成 wasm 文件

```shell
cargo build --target wasm32-wasi --release
```

你可以看到 `{your_subproject_name}/target/wasm32-wasi/release/{your_subproject_name}.wasm`.

### 5.添加依赖到 `pom.xml`

```xml
<dependency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-wasm-cluster-api</artifactId>
    <version>${revision}</version>
</dependency>
```

### 6.添加 `x.y.z.RustLoadBalance.java`

```java
package x.y.z;

public class RustLoadBalance extends AbstractWasmLoadBalance {
    public static final String NAME = "rust";
}
```

### 7.添加 `resources/META-INF/dubbo/org.apache.dubbo.rpc.cluster.LoadBalance`

```text
rust=x.y.z.RustLoadBalance
```

### 8.重命名 WASM 文件

由于类 `x.y.z.RustLoadBalance.java`，最终的wasm文件名应该是 `x.y.z.RustLoadBalance.wasm`，最后将 wasm 文件放在你模块的 `resources` 文件夹中。

## 与Java通信

以下是使用rust的示例。

### 将参数传递给另一种语言

#### 1.将方法导出到另一种语言

```java
public class RustLoadBalance extends AbstractWasmLoadBalance {
    //......
    @Override
    protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> supplier) {
        Map<String, Func> funcMap = new HashMap<>();
        //......
        funcMap.put("get_args", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32, 
            (argId, addr, len) -> {
                String config = "hello from java " + argId;
                System.out.println("java side->" + config);
                assertEquals("hello from java 0", config);
                ByteBuffer buf = supplier.get();
                for (int i = 0; i < len && i < config.length(); i++) {
                    buf.put(addr.intValue() + i, (byte) config.charAt(i));
                }
                return Math.min(config.length(), len);
            }));
        //......
        return funcMap;
    }
}
```

#### 2.在另一种语言中导入方法

```rust
#[link(wasm_import_module = "dubbo")]
extern "C" {
    fn get_args(arg_id: i64, addr: i64, len: i32) -> i32;
}
```

#### 3.在另一种语言中获取参数

```rust
#[no_mangle]
pub unsafe extern "C" fn doSelect(arg_id: i64) -> i32 {
    //......
    let mut buf = [0u8; 32];
    let buf_ptr = buf.as_mut_ptr() as i64;
    eprintln!("rust side-> buffer base address: {}", buf_ptr);
    // get arg from java
    let len = get_args(arg_id, buf_ptr, buf.len() as i32);
    let java_arg = std::str::from_utf8(&buf[..len as usize]).unwrap();
    eprintln!("rust side-> recv:{}", java_arg);
    //......
}
```

### 将结果传递给Java

以下是使用rust的示例。

#### 1.将方法导出到另一种语言

```java
public class RustLoadBalance extends AbstractWasmLoadBalance {
    //......
    
    private static final Map<Long, String> RESULTS = new ConcurrentHashMap<>();
    
    @Override
    protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store, Supplier<ByteBuffer> supplier) {
        Map<String, Func> funcMap = new HashMap<>();
        //......
        funcMap.put("put_result", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
            (argId, addr, len) -> {
                ByteBuffer buf = supplier.get();
                byte[] bytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    bytes[i] = buf.get(addr.intValue() + i);
                }
                String result = new String(bytes, StandardCharsets.UTF_8);
                assertEquals("rust result", result);
                RESULTS.put(argId, result);
                System.out.println("java side->" + result);
                return 0;
            }));
        //......
        return funcMap;
    }
}
```

#### 2.在另一种语言中导入方法

```rust
#[link(wasm_import_module = "dubbo")]
extern "C" {
    fn put_result(arg_id: i64, addr: i64, len: i32) -> i32;
}
```

#### 3.在另一种语言中传递结果

```rust
#[no_mangle]
pub unsafe extern "C" fn doSelect(arg_id: i64) -> i32 {
    //......
    let rust_result = "rust result".as_bytes();
    let result_ptr = rust_result.as_ptr() as i64;
    _ = put_result(arg_id, result_ptr, rust_result.len() as i32);
    //......
}
```

#### 4.在Java中获取结果

```java
public class RustLoadBalance extends AbstractWasmLoadBalance {
    //......
    
    private static final Map<Long, String> RESULTS = new ConcurrentHashMap<>();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        return wasmLoader.getWasmExtern(DO_SELECT_METHOD_NAME)
                .map(extern -> {
                    // call WASI function
                    final Long argumentId = getArgumentId(invokers, url, invocation);
                    ARGUMENTS.put(argumentId, new Argument<>(invokers, url, invocation));
                    // WASI cannot easily pass Java objects like JNI, here we pass Long
                    // then we can get the argument by Long
                    final Integer index = WasmFunctions.func(wasmLoader.getStore(), extern.func(), WasmValType.I64, WasmValType.I32)
                            .call(argumentId);
                    ARGUMENTS.remove(argumentId);
                    
                    // For demonstration purposes, the doSelect method has been overwritten
                    final String result = RESULTS.get(argumentId);
                    assertEquals("rust result", result);
                    return invokers.get(index);
                })
                .orElseThrow(() -> new DubboWasmException(
                        DO_SELECT_METHOD_NAME + " function not found in " + wasmLoader.getWasmName()));
    }
}
```
