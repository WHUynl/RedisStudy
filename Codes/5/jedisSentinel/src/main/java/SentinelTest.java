import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import util.JedisFactory;
public class SentinelTest {
    private JedisSentinelPool jedisSentinelPool = JedisFactory.getInstance().getJedisSentinelPool();

    public void testJedisSentinelPool() {
        try (Jedis jedis = jedisSentinelPool.getResource()) {
            jedis.set("test:sentinel", "SUCCESS");
            System.out.println(jedis.get("test:sentinel"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
