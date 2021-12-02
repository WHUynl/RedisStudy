# 数据类型的底层实现

底层实现如下所示

![](\pic\1.png)

使用object  encoding命令可以得到其底层编码实现

```bash
type hello
#得到string
object encoding hello
#得到embstr
```

##                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               1.简单动态字符串(SDS)

### 简介

1. Simple Dynamic String，是Redis内部自定义的一种数据类型；
2.  在Redis数据库内部，**包含字符串的键值对在底层都是由SDS实现的**； 
3. **SDS还被用于缓冲区的实现**，如AOF缓冲区、客户端中的输入缓冲区。



### C语言原生字符串的问题

1.获取长度的复杂度高：C字符串不记录自身长度，程序必须遍历整个字符串统计其长度，复杂度为O(N)。 

2.内存重分配十分频繁：几乎每次修改C字符串，程序就要对保存这个字符串的数组重新分配一次内存空间。 

3.不能保证二进制安全：因为C字符串以空字符结尾，所以不适合保存二进制数据（内部可能携带空字符）

### Redis 3.2之前的解决方案

```c
/* sds.h */
struct sdshdr { 
	unsigned int len; // 已使用的字节数量（不包含'\0'）
	unsigned int free; // 未使用的字节数量（也不包含'\0'）
	char buf[]; // 保存字符串的数组
};
/*
解决好处：
1. 降低了获取字符串长度的复杂度：
SDS在len属性中记录了字符串的实际长度，所以获取长度的复杂度仅仅为O(1)；
2. 减少了修改带来的内存分配次数：
通过空间预分配和惰性空间释放策略，优化了修改字符串时所需的内存分配次数；
3. 保证了二进制数据存储的安全性：
SDS不会对buf中的数据做任何限制，因为它采用len属性来判定字符串是否结束；
SDS依然以空字符结尾，这样其内部可以很方便的重用一部分C字符串库中的函数。
*/

/*
预分配和惰性释放
预分配：
用于优化增长操作，即不仅为其分配存放字符串所需的空间，还会为其分配额外的未使用空间；
若修改后SDS的长度小于1MB，则分配的未使用空间与len相同，否则分配的未使用空间为1MB；

惰性释放：
用于优化缩短操作，当缩短SDS时程序不立刻重新分配内存，而是使用free属性记录这些字节；
*/
```

#### 不足之处：

**len、free统一占据4字节**，对于较短的字符串来说，浪费了存储空间。

### Redis 3.2之后的解决方案：

优化方向： 

通过字符串长度，将其分为5种类型，分别为1字节、2字节、4字节、8字节、小于1字节。

```c
/* sds.h */
/*
注意数字代表占据的位数，即sdshdr8表示8位=1字节，故同理含有16,32,64,且C语言中。
typedef unsigned char           uint8_t;  
typedef unsigned short int      uint16_t;  
typedef unsigned int            uint32_t;    
typedef unsigned long int       uint64_t; 
————————————————
版权声明：本文为CSDN博主「海阔天空sky1992」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/mary19920410/article/details/71518130

5代表的正是小于1字节的类型。
*/
struct __attribute__ ((__packed__)) sdshdr5 { 
	unsigned char flags; // 低3位存储类型，高5位存储长度
	char buf[]; // 存放实际的内容
};
struct __attribute__ ((__packed__)) sdshdr8 { 
	uint8_t len; // 使用的字节数量
	uint8_t alloc; // 全部的字节数量
	unsigned char flags; // 低3位存储类型，高5位预留
	char buf[]; // 存放实际的内容
};

/*
注意：
1.因为Redis中许多数据结构都是基于SDS实现的，所以flags就是存放实现的数据结构的类型。
2.使用packed修饰后，结构体大小将要按照1字节对齐，从而进一步节省内存。
*/

```

## 2.整数集合(intset)

### 简介

1. 整数集合中的元素按照值的大小由小到大地排列； 
2. 它可以保存int16_t、int32_t、int64_t类型的整数值； 
3. 在存储数据时，整数集合可以保证其内部不出现重复的数据。

**注意：**

当一个set只包含整数元素，并且这个set的元素数量不多时（这个数量我们可以设置），Redis就会使用整数集合作为set的底层实现！

### 整数集合的解决方案：

```c
/* intset.h */
typedef struct intset { 
	uint32_t encoding; // 编码类型
	uint32_t length; // 元素数量
	int8_t contents[]; // 元素数组
} intset;

/*
contents虽然声明为int8_t类型，但它实际上并不保存任何int8_t类型的值，
contents数组的实际类型取决于encoding属性的值：
1. encoding = INTSET_ENC_INT16 -> contents存储int16_t类型的值；
2. encoding = INTSET_ENC_INT32 -> contents存储int32_t类型的值；
3. encoding = INTSET_ENC_INT64 -> contents存储int64_t类型的值；
INTSET_ENC_INT16、INTSET_ENC_INT32、INTSET_ENC_INT64定义在intset.c中。
*/
```

### 整数集合插入比当前元素长or短的解决方案

#### 升级

当添加的新元素，其类型比现有元素类型都长时，集合需要先升级再添加： 

1. 根据新元素的类型，扩展集合底层数组的空间，并为新元素分配空间； 
2. 将现有元素都转成与新元素相同的类型，并将其存储到正确的位置上； 
3. 将新元素添加到数组之内；

#### 降级

整数集合不支持降级，一旦对数组进行了升级，编码就会一直保持升级后的状态！

#### 总结

**升级的优点：**

让一个整数数组同时支持int16_t、int32_t、int64_t，最简单的做法是使用 int64_t，但这样显然是浪费内存空间的，而**升级操作可以尽量的节约内存的使用**。 

**升级的缺点：**

 每次向集合中添加数据都可能会引起升级，而**每次升级都需要对底层数组中所有的元素进行类型转换，所以向集合中添加新元素的时间复杂度为O(n)。**

## 3.字典(dict)

### 简介

1. 字典（dict）又称为散列表，是一种用来**存储键值对的数据结构**； 
2. C语言没有内置这种数据结构，所以Redis构建了自己的字典实现。

**注意：**

字典在Redis中应用十分广泛：

1. **Redis数据库的底层就是采用字典实现的**； 
2.  字典也是**集合、哈希类型**的底层实现之一； 
3. Redis的哨兵模式，是以字典存储所有主从节点的。

### 字典实现

```c
/*
Redis字典的实现主要涉及三个结构体：字典、哈希表、哈希表节点。其中，每个哈希表节
点保存一个键值对，每个哈希表由多个哈希表节点构成，而字典则是对哈希表的进一步封装。

dict.h
*/
typedef struct dict { 
    dictType *type; // 字典类型，内含若干特定的操作函数；
    void *privdata; // 该字典持有的私有数据；
    dictht ht[2]; // 哈希表数组，固定长度为2,在REHASH的时候才会使用到ht[1]，一般正常存储全在ht[0]；
    long rehashidx; // rehash标识，存储rehash的偏移量，默认-1；
    unsigned long iterators; // 记录绑定在此字典上的，正在运行的迭代器数量；
} dict;

typedef struct dictht { 
    // 节点数组
    dictEntry **table;
    // 数组大小
    unsigned long size;
    // 掩码（size-1），用于哈希的恢复计算
    unsigned long sizemask;
    // 已有节点数量
    unsigned long used;
} dictht;

typedef struct dictEntry { 
    void *key; // 键
    union { 
    void *val;
    uint64_t u64; 
    int64_t s64; 
    double d; 
    } v; // 值
    struct dictEntry *next; // 下一节点
} dictEntry;
```

### 字典实现的图例

![](\pic\9.png)

### 哈希算法

向字典中添加新的键值对时，程序需要**先根据键计算出哈希值**，再根据哈希值计算出索引值， **最后将此键值对封装在哈希表节点中，放到节点数组的指定索引上**，关键步骤参考如下代码：

```c
// 使用哈希函数，计算键的哈希值
hash = dict->type->hashFunction(key);
// 使用哈希值和掩码，计算索引值
// 等价于哈希值与哈希表容量取余，但位运算效率更高
index = hash & dict->ht[x].sizemask;
```

**键冲突问题：**

1. 当多个键被分配到了节点数组的同一个索引上时，则这些键发生了冲突； 
2. Redis采用链表来解决键冲突，**即使用next指针将这些节点链接起来，形成单向链表**； 
3. Redis的哈希表节点**没有设计表尾指针**，每次添加时都是**将新节点插入到表头的位置**。

### REHASH

#### REHASH的基本概念

当哈希表保存的键值对数量过多或过少时，需要对哈希表的大小进行**扩展或收缩操作**， 在Redis中，扩展和收缩哈希表是通过REHASH实现的，执行REHASH的大致步骤如下： 

1. 为字典的ht[1]哈希表分配内存空间，如果执行的是扩展操作，**则ht[1]的大小为第1个大于等于ht[0].used*2**的2的n次方； 如果执行的是收缩操作，则**ht[1]的大小为第1个大于等于ht[0].used**的2的n次方； 
2.  将存储在ht[0]中的数据迁移到ht[1]上，重新计算键的哈希值和索引值，然后将键值对放置到ht[1]哈希表的指定位置上； 
3.  **将字典的ht[1]哈希表晋升为默认哈希表 迁移完成后，清空ht[0]，再交换ht[0]和ht[1]的值，为下一次REHASH做准备。**

#### REHASH的触发条件

当满足以下任何一个条件时，程序会自动开始对哈希表执行扩展操作： 

1. 服务器目前**没有执行**bgsave或bgrewriteof命令，并且哈希表的负载因子大于等于1； 
2. 服务器目前**正在执行**bgsave或bgrewriteof命令，并且哈希表的负载因子大于等于5；

 其中，负载因子可以通过如下公式计算： 

```c
load_factor = ht[0].used / ht[0].size; 
```

另外，当**哈希表的负载因子小于0.1时，程序会自动开始对哈希表执行收缩操作。**

#### REHASH的详细步骤

为了避免REHASH对服务器性能造成影响，REHASH操作不是一次性地完成的， 而是**分多次、渐进式地完成的**。渐进式REHASH的详细过程如下： 

1. 为ht[1]分配空间，让字典同时持有ht[0]和ht[1]两个哈希表；  
2. 在字典中的索引计数器rehashidx设置为0，表示REHASH操作正式开始；  
3. 在REHASH期间，**每次对字典执行添加、删除、修改、查找操作时，程序除了执行指 定的操作外，还会顺带将ht[0]中位于rehashidx上的所有键值对迁移到ht[1]中， 再将rehashidx的值加1；** 
4. 随着字典不断被访问，最终在某个时刻，ht[0]上的所有键值对都被迁移到ht[1]上， **此时程序将rehashidx属性值设置为-1，标识REHASH操作完成**

#### REHASH期间的访问

REHSH期间，字典同时持有两个哈希表，此时的访问将按照如下原则处理： 

1. **新添加的键值对，一律被保存到ht[1]中；** 
2. 删除、修改、查找等其他操作，会在两个哈希表上进行，即**程序先尝试去ht[0]中 访问要操作的数据，若不存在则到ht[1]中访问，再对访问到的数据做相应的处理。**

## 4.链表(linkedlist)

### 简介

1. 链表（linkedlist）是一种有序的数据结构，且增删效率较高； 
2. C语言没有内置这种数据结构，所以Redis构建了自己的链表实现。

**注意：**

链表在Redis中应用十分广泛： 

1. 链表是列表的底层实现方案之一； 
2. 发布与订阅、慢查询、监视器等功能也用到了链表； 
3. Redis服务器采用链表保存多个客户端的状态信息； 
4. Redis客户端输出缓冲区是在链表的基础上实现的。

### 链表的解决方案：

链表的实现主要涉及2个结构体：list、listNode，他们均位于adlist.h内。

```c
//listNode的实现
typedef struct listNode { 
	// 前置节点
	struct listNode *prev; 
	// 后置节点
	struct listNode *next; 
	// 节点的值
	void *value;
} listNode;

//list的实现
typedef struct list { 
	listNode *head; // 表头节点
	listNode *tail; // 表尾节点
	void *(*dup)(void *ptr); // 复制节点
	void (*free)(void *ptr); // 释放节点
	int (*match)(void *ptr, void *key); //比较节点的值
	unsigned long len; // 节点数量
} list;

/*
链表的特点，从上述代码中可以看出：
1.双端：
链表节点带有prev指针和next指针，获取某个节点的前置和后置节点的复杂度为O(1)；
2.无环：
表头节点的prev指针和表尾节点的next指针都指向NULL，对链表的访问以NULL为终点；
3.多态：
链表节点采用void*指针来保存节点的值，所以链表可以用于存储各种不同类型的值；
4.有表头及表尾指针：
通过list结构的head指针和tail指针，程序获取链表的表头和表尾节点的复杂度为O(1)；
5.有链表长度计数器
程序使用list结构的len属性对链表节点计数，所以获取链表中节点数量的复杂度为O(1)。
*/
```

整个linkedlist的示意图如下所示：

![](\pic\2.png)

## 5.压缩链表(ziplist)

### 简介：

1. 压缩列表（ziplist），是Redis为了节约内存而设计的一种**线性数据结构**，它是由一系列**具有特殊编码的连续内存块**构成的； 
2. 一个压缩列表可以包含任意多个节点，每个节点**可以保存一个字节数组或一个整数值**。

**注意：**

Redis的列表、哈希、有序集合都直接或间接地使用了压缩列表！

### 压缩链表的结构示意图以及组成说明

![](\pic\3.png)

### 列表节点的实现

![](\pic\4.png)

​	**3种情况存储字节数组，6种情况存储中整数**

![](\pic\5.png)

### 压缩链表的连锁更新

new的添加，导致e1的pel节点从1字节变成5字节，从而导致e1的重分配，随后又因为e1>=254字节然后导致e2的重分配，以此类推下去

同理删除也可导致相同的问题。

![](C:\Users\HP\Desktop\ynl\study materials\Redis\Notes\2.Redis实现机制\pic\6.png)

**注意：**

1. 连锁更新出现的概率很低： 压缩列表中需要恰好有多个连续的，长度介于250~253字节的节点； 
2. 控制节点数量可消除影响： 如果节点数量不多，即便出现连锁更新，对性能也不会造成任何影响

## 6.快速链表(quicklist)

### 简介

1. 快速列表（quicklist）是Redis 3.2新引入的数据结构，**该结构是链表和压缩列表的结合**； 
2. 快速列表是采用双向链表将若干压缩列表连接到一起而组成的数据结构，即它是一个双向链表， 链表中的每个节点是一个压缩列表，这种设计能够在时间效率和空间效率之间实现较好的折中。

**注意：**

在3.2之前，列表类型是采用压缩列表及双向链表实现的； 

从3.2开始，**列表类型采用快速列表作为底层的唯一实现。**

### 快速链表的实现

```c
//quicklist.h: quicklist, quicklistNode
typedef struct quicklist {
	// 头节点
	quicklistNode *head; 
	// 尾节点
	quicklistNode *tail; 
	// 压缩列表的元素总数,即所有压缩链表的元素的总和
	unsigned long count; 
	// 快速列表的节点个数
	unsigned long len; 
	// 压缩列表的最大填充量
	int fill:QL_FILL_BITS; 
	// 不参与压缩的节点个数
	unsigned int compress:QL_COMP_BITS; 
	// 书签数量，可以标记特别快速链表的节点，存到下方的书签数组里
	unsigned int bookmark_count:QL_BM_BITS; 
	// 书签数组
	quicklistBookmark bookmarks[];
} quicklist;

typedef struct quicklistNode {
    // 前一个节点
    struct quicklistNode *prev;
    // 后一个节点
    struct quicklistNode *next;
    // ziplist
    unsigned char *zl;
    // ziplist的字节数量
    unsigned int sz;
    // ziplist的元素个数
    unsigned int count:16;
    // 编码方式(RAW==1 or LZF==2)
    unsigned int encoding:2;
    // 容器类型(NONE==1 or ZIPLIST==2)
    unsigned int container:2;
    // 该节点是否被压缩过
    unsigned int recompress:1;
    // 用于测试期间的验证
    unsigned int attempted_compress : 1;
    // 预留字段
    unsigned int extra : 10;
} quicklistNode;
```

### 快速链表的实现图例

![](\pic\7.png)

### 数据压缩的机制

![](\pic\8.png)

## 7.跳跃表（skiplist）

### 简介

1. 有序集合的底层，可以采用数组、链表、平衡树等结构来实现： 数组不便于元素的插入和删除，链表的查询效率低，平衡树/红黑树效率高但实现复杂；  
2. Redis采用跳跃表（skiplist）作为有序集合的一种实现方案： **跳跃表的查找复杂度为平均O(logN)，最坏O(N)，效率堪比红黑树，却远比红黑树实现简单**；

**注意：**

在Redis中，跳跃表的另一个应用是作为集群节点的内部数据结构！

### 跳跃表的实现

```c
/*
 3.0版本及以前，它们被定义在redis.h中，3.0版本之后，它们被定义在server.h中。
*/
typedef struct zskiplistNode {
    // 节点数据,注意是简单动态字符串
    sds ele;
    // 节点分值，用于比大小
    double score;
    // 后退指针
    struct zskiplistNode *backward;
    // 层级数组（各节点不一样）
    struct zskiplistLevel {
    // 前进指针
    struct zskiplistNode *forward;
    // 跨度（节点间的距离，用于计算排名）
    unsigned long span;
    } level[];
} zskiplistNode;

typedef struct zskiplist {
    // 表头指针、表尾指针
    struct zskiplistNode *header, *tail;
    // 跳跃表的长度（除表头之外的节点总数），表头只是作为头结点存储信息。
    unsigned long length;
    // 跳跃表的高度（除表头之外的最高层数）
    int level;
} zskiplist;
```

### 跳跃表的实现图例

![](\pic\10.png)

**注意：（上述应该是[1,31]不是[1,32]）**

1. 优先从高层开始查找； 
2. 若**next节点值大于目标值，或next指针指向NULL**，则从当前节点下降一层继续向后查找；

### 跳跃表小结

1. 跳跃表由多层构成，它的**每一层都是一个有序链表，数据依次递增**； 
2. 跳跃表有一个**头节点，它是一个32层的结构，内部不存储实际数据**； 
3. **跳跃表包含有头尾指针**，分别指向跳跃表的第一个和最后一个节点； 
4. 除头节点外，层数最多的节点的层高为跳跃表的高度； 
5. 除头节点外，**一个元素在上层有序链表中出现，则它一定能够会在下层有序链表中出现**； 
6. 跳跃表每层的最后一个节点指向NULL； 
7. 最底层的有序链表包含所有的节点，最底层的节点个数为跳跃表的长度； 
8. 每个节点包含一个后退指针，头节点和第一个节点指向NULL，其他节点指向最底层的前一节点。