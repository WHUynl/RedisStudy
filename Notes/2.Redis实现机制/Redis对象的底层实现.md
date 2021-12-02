# Redis对象的底层实现

### **简介**

1. Redis数据库中的键值对由对象来表示，其中**键是一个对象，值是另一个对象**； 
2. Redis设计了**redisObject结构来表示一个对象**，该结构的源代码如下：

```c
// redis.h, server.h
typedef struct redisObject {
    unsigned type:4; // 对象类型
    unsigned encoding:4; // 对象编码
    unsigned lru:LRU_BITS; // 访问时间
    int refcount; // 引用计数
    void *ptr; // 指向底层数据结构
} robj;

```

### **对象的类型**

![](pic\11.png)

### 对象的编码

![](pic\12.png)

###  类型与编码

![](pic\13.png)

### 对象的访问时间

1. lru属性用于记录对象最后一次被程序访问的时间，可用于实现缓存淘汰策略；  
2. **OBJECT IDLETIME**命令可以打印出某个键的空闲时间，该时间是由lru计算而来；

```bash
127.0.0.1:6379> object idletime yy
(integer) 2600
```

### 对象的引用计数

refcount属性用于记录对象的引用次数： 

1. 在创建一个新对象时，引用计数的值会被初始化为1； 
2.  当对象被一个新程序使用时，它的引用计数值会加1； 
3. 当对象不再被某程序使用时，他的引用计数值会减1； 
4. 当对象的引用计数值变为0时，它所占用的内存空间将会被释放； 对象的引用计数，可用于实现对象的内存回收，以及对象共享的功能。

**注意**

Redis会在初始化服务器时，创建一万个字符串对象，这些对象包含了 从0到9999的所有整数值。**当服务器需要用到值为0到9999的字符串对 象时，就会使用这些共享对象，而不是创建新的对象！**

```c
127.0.0.1:6379> set yy 20000
OK
127.0.0.1:6379> object refcount yy
(integer) 1
127.0.0.1:6379> set yy 200
OK
127.0.0.1:6379> object refcount yy
(integer) 2147483647
127.0.0.1:6379> set xx 200
OK
127.0.0.1:6379> object refcount xx
(integer) 2147483647
127.0.0.1:6379> set xx 20000
OK
127.0.0.1:6379> object refcount xx
(integer) 1
```

注意，具体实现和理论是有差距的