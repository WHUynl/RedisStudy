package util;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;

public class JedisFactory {
    private static JedisFactory jedisFactory;

    private JedisPool jedisPool;

    public static JedisFactory getInstance(){
        if(jedisFactory==null){
            jedisFactory = new JedisFactory();
        }
        return jedisFactory;
    }

    public JedisPool getJedisPool() {
        if (jedisPool == null) {
            // 连接池配置
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            // 创建连接池
            jedisPool = new JedisPool(config, "124.71.228.7", 6379, 3000, "123456");
        }
        return jedisPool;
    }
}
