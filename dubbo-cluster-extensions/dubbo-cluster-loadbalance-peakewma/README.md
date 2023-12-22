# Introduction
PeakEwmaLoadBalance is designed to converge quickly when encountering slow endpoints.

It is quick to react to latency spikes recovering only cautiously.Peak EWMA takes history into account,so that slow behavior is penalized relative to the supplied `decayTime`.

if there are multiple invokers and the same cost,then randomly called,which doesn't care about weight.

Inspiration drawn from:

https://github.com/twitter/finagle/blob/1bc837c4feafc0096e43c0e98516a8e1c50c4421

/finagle-core/src/main/scala/com/twitter/finagle/loadbalancer/PeakEwma.scala

