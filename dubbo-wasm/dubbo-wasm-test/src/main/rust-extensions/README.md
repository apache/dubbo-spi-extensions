# How to build the wasm file

1. install rustup

2. install rust

3. generate the wasm file

```shell
cd {dubbo-spi-extensions}/dubbo-wasm/dubbo-wasm-test/src/main/rust-extensions
cargo build --target wasm32-wasi --release
```

then you will see the wasm file
in `{dubbo-spi-extensions}/dubbo-wasm/dubbo-wasm-test/src/main/rust-extensions/target/wasm32-wasi/release/rust_filter.wasm`

4. rename the wasm file if you need
