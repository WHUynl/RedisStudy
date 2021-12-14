import redis.clients.jedis.*;
import redis.clients.jedis.JedisPubSub;
import util.JedisFactory;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.StreamEntryID;
import com.alibaba.fastjson.JSON;
import java.util.List;
import java.util.Map;

public class APP {
    public static void main(String[] args) throws InterruptedException {
        SentinelTest sentinelTest = new SentinelTest();
        sentinelTest.testJedisSentinelPool();
    }
}
