# dubbo WASM api

At present, Dubbo's SPI extensions can only be written in Java language, the dubbo-wasm module aims to overcome this limitation.

WASM(WebAssembly) bytecode is designed to be encoded in a size- and load-time-efficient binary format. WASM aims to leverage the common hardware features available on various platforms to execute in browsers at machine code speed.

WASI(WebAssembly System Interface) provide a portable interface for applications that run within a constrained sandbox environment, which allows WASM to run in non browser environments such as Linux. It's portable and secure.

We use [wasmtime-java](https://github.com/kawamuray/wasmtime-java) to run WASI in Java, as long as the language can be compiled into WASM bytecode, it can be used to write SPI extensions for dubbo.

## Module structure

* dubbo-wasm-api: Responsible for defining basic exceptions and providing runtime for executing WASM bytecode (Dubbo API)
* dubbo-wasm-cluster-api: Provide the WASM version of the dubbo-cluster module SPIs
* dubbo-wasm-common-api: Provide the WASM version of the dubbo-common module SPIs
* dubbo-wasm-metrics-api(todo): Provide the WASM version of the dubbo-metrics module SPIs
* dubbo-wasm-registry-api: Provide the WASM version of the dubbo-registry-api module SPIs
* dubbo-wasm-remoting-api(todo): Provide the WASM version of the dubbo-remoting-api module SPIs
* dubbo-wasm-rpc-api: Provide the WASM version of the dubbo-rpc-api module SPIs

## Note

Due to the strict requirements of WASM on parameter types and for simplicity reasons, types other than `java.lang.Long`/`java.lang.Integerr` are not used as parameters or return value.

## How to use

Below is an example of implementing `org.apache.dubbo.rpc.cluster.LoadBalance` SPI in rust.

### 1.Create subproject in another language

```shell
cargo new --lib your_subproject_name
```

### 2.Add methods to lib.rs

```rust
// Adding `#[no_mangle]` to prevent the Rust compiler from modifying method names is mandatory
#[no_mangle]
pub unsafe extern "C" fn doSelect(_arg_id: i64) -> i32 {
    1
}
```

### 3.Add `[lib]` to `Cargo.toml` and change `crate-type` to `["cdylib"]`. Ultimately, your `Cargo.toml` should look like

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

### 4.Generate the wasm file

```shell
cargo build --target wasm32-wasi --release
```

You will see `{your_subproject_name}/target/wasm32-wasi/release/{your_subproject_name}.wasm`.

### 5.Add dependency to `pom.xml`

```xml
<dependency>
    <groupId>org.apache.dubbo.extensions</groupId>
    <artifactId>dubbo-wasm-cluster-api</artifactId>
    <version>${revision}</version>
</dependency>
```

### 6.Add x.y.z.RustLoadBalance.java

```java
package x.y.z;

public class RustLoadBalance extends AbstractWasmLoadBalance {
    public static final String NAME = "rust";
}
```

### 7.Add resources/META-INF/dubbo/org.apache.dubbo.rpc.cluster.LoadBalance

```text
rust=x.y.z.RustLoadBalance
```

### 8.Rename the WASM file

Due to the class `x.y.z.RustLoadBalance.java`，the final wasm file name should be `x.y.z.RustLoadBalance.wasm`, finally, put the wasm file in the `resources` folder of your module.

## Communicate with Java

Below is an example of using rust.

### Pass args to another language

#### 1.Export method to another language

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

#### 2.Import method in another language

```rust
#[link(wasm_import_module = "dubbo")]
extern "C" {
    fn get_args(arg_id: i64, addr: i64, len: i32) -> i32;
}
```

#### 3.Get args in another language

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

### Pass result to Java

Below is an example of using rust.

#### 1.Export method to another language

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

#### 2.Import method in another language

```rust
#[link(wasm_import_module = "dubbo")]
extern "C" {
    fn put_result(arg_id: i64, addr: i64, len: i32) -> i32;
}
```

#### 3.Pass result in another language

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

#### 4.Get result in Java

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
