# Redis复制的常见问题

### 1.读写分离

对于读占比较高的场景，可以把一部分读流量分摊到从节点，来减轻主节点的压力；

当使用从节点响应读请求时，业务端可能会遇到如下问题： 

1. 复制数据延迟 

   • 由于异步复制的特性，**数据延迟是无法避免的**，延迟取决于网络带宽和命令阻塞的情况； 

   • 可以编写监控程序来监听主从节点的复制偏移量，当延迟较大时触发报警以通知客户端； 

   • 对于无法容忍延迟的业务，更加建议**采用集群方案做水平扩展**，而不是采用上述高成本的方案；

2.  读到过期数据 

   • 主节点每次处理读取命令时，都会检查键是否超时，若超时则执行del命令，再将del命令发送给从节点； 、

   • **主节点内部定时任务会循环采样一定数量的键，当发现采样的键过期时执行del命令，然后同步给从节点**； 

   • 若此时数据大量超时，主节点采样速度跟不上过期速度，且主节点没有读取过期键的操作，则从节点无法收 到del命令，这时在从节点上可以读取到已经超时的数据； 

   • Redis在3.2版本解决了这个问题，**从节点读取数据之前会检查键的过期时间，从而决定是否要返回数据**。

### 2.主从配置不一致

◼ 有些配置主从之间可以不一致 例如，

​	主节点关闭AOF持久化，从节点开启AOF持久化；

 ◼ 内存相关配置主从之间须一致 例如，

​	配置的maxmemory从节点小于主节点，若复制的数据量超过从节点的maxmemory时，它会根据 maxmemory-policy进行内存溢出控制，此时从节点数据已经丢失，但主节点复制流程依然正常进行， 复制偏移量也正常，而修复这类问题也只能手动进行全量复制。

### 3.规避全量复制

◼ 第一次建立复制 

从节点不包含任何主节点数据，必须进行全量复制才能完成数据同步，这种情况下全量复制无法避免； 对数据量较大且流量较高的主节点添加从节点时，**建议在低峰时进行操作**； 

◼ 节点运行ID不匹配 

若主节点因故障重启，则它的运行ID会改变，从节点发现主节点运行ID不匹配时，会触发全量复制； 这种情况应该从架构上规避，**比如提供故障转移功能，可以采用支持自动故障转移的哨兵/集群模式**； 

◼ 复制积压缓冲区不足 

当主从节点网络中断后，从节点再次连上主节点时会请求部分复制，若请求的偏移量不在主节点的积 压缓冲区内，则主节点无法为从节点提供数据，此时部分复制会退化为全量复制； **应根据网络中断时长，写命令数据量分析出合理的积压缓冲区大小，设置合理的积压缓冲区空间**