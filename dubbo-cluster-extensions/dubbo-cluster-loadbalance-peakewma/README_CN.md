# 简介
PeakEwmaLoadBalance 旨在在遇到慢速端点时快速收敛。

它对延迟峰值反应很快，只是谨慎地恢复。峰值 EWMA 会考虑历史，因此相对于提供的“decayTime”，缓慢的行为会受到惩罚。

如果有多个调用者且成本相同，则随机调用，不关心权重。

灵感源自：

https://github.com/twitter/finagle/blob/1bc837c4feafc0096e43c0e98516a8e1c50c4421

/finagle-core/src/main/scala/com/twitter/finagle/loadbalancer/PeakEwma.scala
