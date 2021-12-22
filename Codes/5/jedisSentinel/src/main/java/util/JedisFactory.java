package util;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Set;

public class JedisFactory {
    private static JedisFactory jedisFactory;

    private JedisPool jedisPool;

    private JedisSentinelPool jedisSentinelPool;

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

    public JedisSentinelPool getJedisSentinelPool() {
        if (jedisSentinelPool == null) {
            // 连接池配置
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            // 哨兵集合
            Set<String> sentinels = new HashSet<>();
            sentinels.add("124.71.228.7:8801");
            sentinels.add("124.71.228.7:8802");
            sentinels.add("124.71.228.7:8803");
            // 创建连接池
            jedisSentinelPool = new JedisSentinelPool("mymaster", sentinels, config, "123456");
        }
        return jedisSentinelPool;
    }

    public JedisCluster jedisCluster;

    public JedisCluster getJedisCluster(){
        if(jedisCluster == null){
            //连接池的配置
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            //节点集合
            Set<HostAndPort> nodes = new HashSet<>();
            nodes.add(new HostAndPort("124.71.228.7",9001));
            nodes.add(new HostAndPort("124.71.228.7",9002));
            nodes.add(new HostAndPort("124.71.228.7",9003));
            nodes.add(new HostAndPort("124.71.228.7",9004));
            nodes.add(new HostAndPort("124.71.228.7",9005));
            nodes.add(new HostAndPort("124.71.228.7",9006));
            jedisCluster = new JedisCluster(nodes,1000,1000,10,"123456",config);
        }
        return jedisCluster;
    }
}
