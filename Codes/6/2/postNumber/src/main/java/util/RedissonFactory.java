package util;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonFactory {

    private static RedissonFactory redissonFactory;

    private RedissonClient clusterClient;

    private RedissonFactory() {
        super();
    }

    public static RedissonFactory getInstance() {
        if (redissonFactory == null) {
            redissonFactory = new RedissonFactory();
        }
        return redissonFactory;
    }

    public RedissonClient getClusterClient() {
        if (clusterClient == null) {
            Config config = new Config();
            config.useClusterServers()
                    .setCheckSlotsCoverage(false)
                    .setPassword("123456")
                    .addNodeAddress("redis://124.71.228.7:9001")
                    .addNodeAddress("redis://124.71.228.7:9002")
                    .addNodeAddress("redis://124.71.228.7:9003")
                    .addNodeAddress("redis://124.71.228.7:9004")
                    .addNodeAddress("redis://124.71.228.7:9005")
                    .addNodeAddress("redis://124.71.228.7:9006");

            clusterClient = Redisson.create(config);
        }
        return clusterClient;
    }

}
