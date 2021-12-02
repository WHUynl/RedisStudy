# Bitmap

### 简介

1. Bitmap本身不是一种数据类型，它**实际上就是字符串，但是它可以对字符串进行按位的操作**；  
2. Redis为Bitmap单独提供了一套命令，所以使用Bitmap与使用普通字符串的方式是不同的； 
3. 可以把**Bitmap看作是一个以位为单位的数组**，数组的每个单元只能存储0和1，数组的下标叫做偏移量

### Bitmap的扩展

1. 当用户执行命令尝试对一个bitmap进行设置的时候，**如果该bitmap不存在，或者当前bitmap的大小无法满足 用户想要执行的设置操作，则Redis会对被设置的bitmap进行扩展**，使得bitmap可以满足用户的设置需求； 
2. Redis对bitmap的扩展操作是以**字节**为单位进行的，所以扩展之后的位图包含的二进制位数量可能会比用户需求的稍微多一些，并且在扩展bitmap的同时，Redis还会将所有未被设置的二进制位的值初始化为0。

![](pic\1.png)

### Bitmap的使用场景

记录用户一年的签到数据： 示例：

user:9527:2020 -> 00101101 10010001 ...... 

说明：从第1天开始，以天数为索引记录，0表示未签、1表示已签，记录一年的数据只需368位（46字节）即可。

### Bitmap常用命令

使用上述的mybitmap

```bash
#设置添加
127.0.0.1:6379> setbit mybitmap 20 1
(integer) 0
127.0.0.1:6379> setbit mybitmap 5 1
(integer) 0
127.0.0.1:6379> setbit mybitmap 10 1
(integer) 0
#获得位值
127.0.0.1:6379> getbit mybitmap 10
(integer) 1
127.0.0.1:6379>
#获得[start,end]内位值为1的个数，注意start和end是代表的是字节
127.0.0.1:6379> bitcount mybitmap 1 2
(integer) 2
#获得在区间内第一个1出现的位置
127.0.0.1:6379> bitpos mybitmap 1 -3 -2
(integer) 5
#按位运算,此处展示或运算,并将结果存到deskey
#bitmap2 -> 10000100 10000000
#bitmap3 -> 00001000
127.0.0.1:6379> bitop or deskey bitmap3 bitmap2 mybitmap
(integer) 
```

