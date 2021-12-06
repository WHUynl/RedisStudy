# Pipeline

### RTT(Round Trip Time)

![](pic\5.png)

### 简介

![](pic\6.png)

### Pipeline与批量命令对比

1. 批量命令是**原子的**，Pipeline是**非原子的**； 
2. 批量命令是**一个命令对应多个key**，Pipeline则**支持多个命令**； 
3. 批量命令是**Redis服务端支持的**，Pipeline则需要**服务端和客户端共同支持**。

**注意**

1. 每次Pipeline组装的命令不宜过多，**一方面增加客户端等待时间，另一方面会造成一定的网络阻塞**； 
2. 建议将一次包含大量命令的Pipeline，拆分成多次较小的Pipeline来实现。

