# dubbo-rpc-hessian

## Security

Warning: by default, anyone who can provide data to the Hessian deserializer
can cause it to run arbitrary code.

For that reason, if you enable the dubbo-rpc-hessian component, you must make
sure your deployment is only reachable by trusted parties, and/or configure
a serialization whitelist. Unfortunately we don't currently have any
documentation on how to configure a serialization whitelist.

For more general information on how to deal with deserialization security,
see [this page](https://dubbo.apache.org/en/docs/notices/security/#some-suggestions-to-deal-with-the-security-vulnerability-of-deserialization)
