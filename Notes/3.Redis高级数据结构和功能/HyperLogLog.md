# HyperLogLog

### 简介

1. HyperLogLog并不是一种新的数据类型，实际上它是字符串类型； 
2. HyperLogLog是一个**专门为了计算集合的基数而创建的概率算法**，其优点在于它十分的节约内存空间； 
3. HyperLogLog**只需12KB的内存空间，就可以对2的64次方个元素进行计数**，其标准误差仅为0.81%，结果是相当可信的。

###  HyperLogLog的使用场景

![](pic\2.png)

### HyperLogLog常用命令

```bash
#添加数据至hill1，注意返回的值仅代表添加的值是否含有新的值
127.0.0.1:6379> pfadd hill1 1 2 3 4 5
(integer) 1
127.0.0.1:6379> pfadd hill1 4 5 6
(integer) 1
#计算HLL里面的基数个数
127.0.0.1:6379> pfcount hill1
(integer) 6

#合并两个HLL，使用pfmerge将结果进行存储，使用pfcount也可，但是其底层由pfmerge实现
127.0.0.1:6379> pfadd hill2 5 6 7 8 10
(integer) 1
127.0.0.1:6379> pfmerge hill3 hill1 hill2
127.0.0.1:6379> pfcount hill3
(integer) 9

127.0.0.1:6379> pfcount hill1 hill2
(integer) 9

#使用脚本可以发现当插入数量过大时，还是会出现误差
127.0.0.1:6379> eval "for i = 1,20000000 do redis.call('pfadd',KEYS[1],i) end" 1 "hll5"
(nil)
(16.40s)
127.0.0.1:6379> pfcount hll5
(integer) 20135950
```

