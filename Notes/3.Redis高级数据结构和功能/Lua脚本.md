# Lua脚本

### Redis的Lua脚本

Redis从2.6开始引入Lua脚本，利用Lua脚本可以很方便地对Redis服务器的功能进行扩展： 

1.Redis服务器**内置了Lua解释器**，可以直接执行Lua脚本； 

2.Lua脚本**可以直接调用Redis命令**(redis.call()来进行调用)，并使用**Lua语言及其内置的函数库处理命令的结果**； 

3.Redis服务器在执行Lua脚本的过程中，**不会执行其他客户端发送的命令或脚本，执行过程是原子的**。

### 使用Lua脚本

EVAL script numkeys key [key ...] arg [arg ...] 

1. script参数用于**传递脚本本身**； 
2. numkeys参数用于指定脚本需要处理的键的数量； 
3. 参数key可以是任意多个，用来指定被脚本处理的键，在脚本中通过**KEYS数组(注意必须声明为KEYS[i])**来访问这些参数key； 
4. 参数arg可以是任意多个，用来指定传递给脚本的附加参数，在脚本中通过**ARGV**数组来访问这些参数arg。

SCRIPT LOAD script 

EVALSHA sha1 numkeys key [key ...] arg [arg ...] 

 	1. SCRIPT LOAD命令可以将指定的脚本缓存在服务器上，并返回脚本对应的**SHA1校验和**； 
	1. EVALSHA命令用来执行已被缓存的脚本，它后面的sha1参数是脚本对应的SHA1校验和。

### 管理Lua脚本

1. SCRIPT LOAD script 将指定的脚本缓存到Redis服务器上； 
2. SCRIPT EXISTS sha1 [sha1 ...] 检查校验和对应的脚本是否存在于Redis服务器中； 
3. SCRIPT FLUSH 移除所有已经缓存的脚本； 
4. SCRIPT KILL 强制停止正在运行的脚本。



**redis.conf的配置项说明**

lua-time-limit配置项 

1. 该配置项定义了Lua脚本不受限制运行的时长，其默认值为5000；
2.  **当脚本的运行时间超过该值时，向服务器发送请求的客户端将得到一个 错误的回复，提示用户可以使用SCRIPT KILL或SHUTDOWN NOSAVE 命令来终止脚本或者直接关闭服务器；** 

SCRIPT KILL命令执行后 

1. 如果正在运行的Lua脚本尚未执行过任何写命令，则服务器终止该脚本， 回到正常状态，继续处理客户端的请求； 
1. 如果正在运行的Lua脚本已经执行过写命令，服务器**不会直接终止脚本**，并回到正常状态，这种情况下，用户**只能通过SHUTDOWN NOSAVE命令， 在不持久化的情况下关闭服务器**。