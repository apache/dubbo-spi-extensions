# How to build the wasm file

1. install rustup

2. install rust

3. generate the wasm file

```shell
cd {dubbo-spi-extensions}/dubbo-wasm/dubbo-wasm-cluster-api/src/test/rust-load-balance
cargo build --target wasm32-wasi --release
```

then you will see the wasm file
in `{dubbo-spi-extensions}/dubbo-wasm/dubbo-wasm-cluster-api/src/test/rust-load-balance/target/wasm32-wasi/release/rust_load_balance.wasm`

4. rename the wasm file

rename the file to `org.apache.dubbo.wasm.cluster.AbstractWasmLoadBalanceTest$RustLoadBalance.wasm`
