/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#[link(wasm_import_module = "dubbo")]
extern "C" {
    fn get_args(arg_id: i64, addr: i64, len: i32) -> i32;

    fn put_result(arg_id: i64, addr: i64, len: i32) -> i32;

    fn setRemoteAddressHost(arg_id: i64, addr: i64, len: i32) -> i32;

    fn setLocalAddressHost(arg_id: i64, addr: i64, len: i32) -> i32;
}

pub unsafe extern "C" fn impls(arg_id: i64) {
    let mut buf = [0u8; 32];
    let buf_ptr = buf.as_mut_ptr() as i64;
    // get arg from java
    let len = get_args(arg_id, buf_ptr, buf.len() as i32);
    let java_arg = std::str::from_utf8(&buf[..len as usize]).unwrap();
    eprintln!("rust side-> recv:{}", java_arg);
    // pass rust result to java
    let rust_result = "rust result".as_bytes();
    let result_ptr = rust_result.as_ptr() as i64;
    _ = put_result(arg_id, result_ptr, rust_result.len() as i32);
}

/// 0
#[no_mangle]
pub unsafe extern "C" fn invoke(arg_id: i64) {
    eprintln!("rust side-> invoke");
    impls(arg_id);
}

/// 1
#[no_mangle]
pub unsafe extern "C" fn doSelect(arg_id: i64) -> i32 {
    eprintln!("rust side-> doSelect");
    impls(arg_id);
    1
}

/// 2
#[no_mangle]
pub unsafe extern "C" fn route(arg_id: i64) {
    eprintln!("rust side-> route");
    impls(arg_id);
}

/// 3
#[no_mangle]
pub unsafe extern "C" fn afterUnExport() {
    eprintln!("rust side-> afterUnExport");
    impls(3);
}

/// 4
#[no_mangle]
pub unsafe extern "C" fn doInvoke(arg_id: i64) {
    eprintln!("rust side-> doInvoke");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn destroy() {
    eprintln!("rust side-> destroy");
    impls(4);
}

#[no_mangle]
pub unsafe extern "C" fn destroyAll() {
    eprintln!("rust side-> destroyAll");
    impls(4);
}

/// 5
#[no_mangle]
pub unsafe extern "C" fn refer(arg_id: i64) {
    eprintln!("rust side-> refer");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn export(arg_id: i64) {
    eprintln!("rust side-> export");
    impls(arg_id);
}

/// 6 & 7
#[no_mangle]
pub unsafe extern "C" fn doRegister(arg_id: i64) {
    eprintln!("rust side-> doRegister");
    impls(arg_id);
}

/// 6 & 7
#[no_mangle]
pub unsafe extern "C" fn doUnregister(arg_id: i64) {
    eprintln!("rust side-> doUnregister");
    impls(arg_id);
}

/// 6
#[no_mangle]
pub unsafe extern "C" fn doSubscribe(arg_id: i64) {
    eprintln!("rust side-> doSubscribe");
    impls(arg_id);
}

/// 6
#[no_mangle]
pub unsafe extern "C" fn doUnsubscribe(arg_id: i64) {
    eprintln!("rust side-> doUnsubscribe");
    impls(arg_id);
}

/// 6
#[no_mangle]
pub unsafe extern "C" fn isAvailable() -> i32 {
    eprintln!("rust side-> isAvailable");
    impls(6);
    1
}

/// 7
#[no_mangle]
pub unsafe extern "C" fn doDestroy() {
    eprintln!("rust side-> doDestroy");
    impls(7);
}

#[no_mangle]
pub unsafe extern "C" fn getServices() -> i32 {
    eprintln!("rust side-> getServices");
    impls(7);
    1
}

#[no_mangle]
pub unsafe extern "C" fn getInstances(arg_id: i64) -> i32 {
    eprintln!("rust side-> getInstances");
    impls(arg_id);
    1
}

#[no_mangle]
pub unsafe extern "C" fn addServiceInstancesChangedListener(arg_id: i64) {
    eprintln!("rust side-> addServiceInstancesChangedListener");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn removeServiceInstancesChangedListener(arg_id: i64) {
    eprintln!("rust side-> removeServiceInstancesChangedListener");
    impls(arg_id);
}

/// 8

#[no_mangle]
pub unsafe extern "C" fn getInternalProperty(arg_id: i64) {
    eprintln!("rust side-> getInternalProperty");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn doPublishConfig(arg_id: i64) -> i32 {
    eprintln!("rust side-> doPublishConfig");
    impls(arg_id);
    1
}

#[no_mangle]
pub unsafe extern "C" fn doGetConfig(arg_id: i64) {
    eprintln!("rust side-> doPublishConfig");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn doRemoveConfig(arg_id: i64) -> i32 {
    eprintln!("rust side-> doRemoveConfig");
    impls(arg_id);
    1
}

#[no_mangle]
pub unsafe extern "C" fn doGetConfigKeys(arg_id: i64) -> i32 {
    eprintln!("rust side-> doGetConfigKeys");
    impls(arg_id);
    1
}

#[no_mangle]
pub unsafe extern "C" fn doGetConfigKeyItem(arg_id: i64, index:i32) {
    eprintln!("rust side-> doGetConfigKeyItem {index}");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn doAddListener(arg_id: i64) {
    eprintln!("rust side-> doAddListener");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn doRemoveListener(arg_id: i64) {
    eprintln!("rust side-> doRemoveListener");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn doClose(arg_id: i64) {
    eprintln!("rust side-> doClose");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn publishConfigCas(arg_id: i64) -> i32 {
    eprintln!("rust side-> publishConfigCas");
    impls(arg_id);
    1
}

/// 9

#[no_mangle]
pub unsafe extern "C" fn send(arg_id: i64) {
    eprintln!("rust side-> send");
    impls(arg_id);
}

#[no_mangle]
pub unsafe extern "C" fn getRemoteAddressHost()->i64 {
    eprintln!("rust side-> getRemoteAddressHost");
    impls(9);
    let rust_result = "localhost".as_bytes();
    let result_ptr = rust_result.as_ptr() as i64;
    _ = setRemoteAddressHost(9, result_ptr, rust_result.len() as i32);
    9
}

#[no_mangle]
pub unsafe extern "C" fn getRemoteAddressPort()->i32 {
    eprintln!("rust side-> getRemoteAddressPort");
    impls(9);
    9999
}

#[no_mangle]
pub unsafe extern "C" fn isConnected()->i32 {
    eprintln!("rust side-> isConnected");
    impls(9);
    1
}

#[no_mangle]
pub unsafe extern "C" fn hasAttribute(arg_id: i64) -> i32 {
    eprintln!("rust side-> hasAttribute");
    impls(arg_id);
    1
}

#[no_mangle]
pub unsafe extern "C" fn getAttribute(arg_id: i64) -> i32 {
    eprintln!("rust side-> getAttribute");
    impls(arg_id);
    1
}

#[no_mangle]
pub unsafe extern "C" fn setAttribute(arg_id: i64) -> i32 {
    eprintln!("rust side-> setAttribute");
    impls(arg_id);
    1
}

#[no_mangle]
pub unsafe extern "C" fn removeAttribute(arg_id: i64) -> i32 {
    eprintln!("rust side-> removeAttribute");
    impls(arg_id);
    1
}

#[no_mangle]
pub unsafe extern "C" fn getLocalAddressHost()->i64 {
    eprintln!("rust side-> getLocalAddressHost");
    impls(9);
    let rust_result = "localhost".as_bytes();
    let result_ptr = rust_result.as_ptr() as i64;
    _ = setLocalAddressHost(9, result_ptr, rust_result.len() as i32);
    9
}

#[no_mangle]
pub unsafe extern "C" fn getLocalAddressPort()->i32 {
    eprintln!("rust side-> getLocalAddressPort");
    impls(9);
    9999
}

#[no_mangle]
pub unsafe extern "C" fn closeChannel() {
    eprintln!("rust side-> closeChannel");
    impls(9);
}
