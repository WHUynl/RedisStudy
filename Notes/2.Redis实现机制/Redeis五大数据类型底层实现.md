# 五大数据类型的底层实现

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