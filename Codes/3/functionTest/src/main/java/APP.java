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
        FunctionTest functionTest = new FunctionTest();
        /*
        functionTest.testBitmap();
        functionTest.testHyperLogLog();
        functionTest.testGEO();
        functionTest.testPipeline();
        functionTest.testTransaction();
        functionTest.testLuaScript();
        functionTest.testSort();
        */

        /*
        JedisPool jedisPool = JedisFactory.getInstance().getJedisPool();

        //订阅
        PubSubConsumer pubSubConsumer = new PubSubConsumer(jedisPool.getResource());
        JedisPubSub handel1 = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                System.out.println("consumer: "+Thread.currentThread().getName());
                System.out.println("channel: " + channel);
                System.out.println("message: "+message);
                System.out.println("##############################");
            }
        };

        pubSubConsumer.subscribe(handel1,"news:music");

        PubSubConsumer pubSubConsumer2 = new PubSubConsumer(jedisPool.getResource());
        JedisPubSub handle2 = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                System.out.println("consumer2: "+Thread.currentThread().getName());
                System.out.println("channel: " + channel);
                System.out.println("message: "+message);
                System.out.println("##############################");
            }
        };

        pubSubConsumer2.subscribe(handle2,"news:sport");

        PubSubConsumer pubSubConsumer3 = new PubSubConsumer(jedisPool.getResource());
        JedisPubSub handle3 = new JedisPubSub() {
            @Override
            public void onPMessage(String pattern,String channel, String message) {
                System.out.println("consumer3: "+Thread.currentThread().getName());
                System.out.println("pattern: " + pattern);
                System.out.println("channel: " + channel);
                System.out.println("message: "+message);
                System.out.println("##############################");
            }
        };

        pubSubConsumer3.subscribeByPattern(handle3,"news:*");

        //发布
        PubSubProducer pubSubProducer = new PubSubProducer(jedisPool.getResource());
        pubSubProducer.publish("news:music","Hello Music");
        pubSubProducer.publish("news:sport","Hello Sport");
        pubSubProducer.publish("news:it","Hello IT");


        pubSubConsumer.unsubscribe(handel1,"news:music");
        pubSubConsumer2.unsubscribe(handle2,"news:sport");
        pubSubConsumer3.unsubscribeByPattern(handle3,"news:*");

        //再发布将不再接收
        pubSubProducer.publish("news:sport","Hello Sport");

         */
        JedisPool jedisPool = JedisFactory.getInstance().getJedisPool();
        // 独立消费者
//        StreamConsumer sc0 = new StreamConsumer(jedisPool.getResource());
//        sc0.consume("s1", new StreamConsumerHandler() {
//            @Override
//            public void handle(List<StreamEntry> list) {
//                for (StreamEntry entry : list) {
//                    System.out.println(entry.getID());
//                    System.out.println(entry.getFields());
//                    System.out.println("==============================");
//                }
//            }
//        });


        // 创建消费组
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.xgroupCreate("s1", "g1", new StreamEntryID(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 消费规则
        StreamConsumerHandler handler = new StreamConsumerHandler() {
            @Override
            public void handle(List<StreamEntry> list) {
                for (StreamEntry entry : list) {
                    System.out.println("consumer: " + Thread.currentThread().getName());
                    System.out.println(entry.getID());
                    System.out.println(entry.getFields());
                    System.out.println("==============================");
                }
            }
        };

        // 消费组消费
        StreamConsumer sc1 = new StreamConsumer(jedisPool.getResource());
        sc1.consumeGroup("s1", "g1", "c1", handler);
        StreamConsumer sc2 = new StreamConsumer(jedisPool.getResource());
        sc2.consumeGroup("s1", "g1", "c2", handler);
        StreamConsumer sc3 = new StreamConsumer(jedisPool.getResource());
        sc3.consumeGroup("s1", "g1", "c3", handler);

        // 生产
        StreamProducer sp = new StreamProducer(jedisPool.getResource());
        for (int i = 0, n = 0; i < 3; i++) {
            sp.xadd("s1", StreamEntryID.NEW_ENTRY, JSON.parseObject("{'num':'" + ++n + "'}", Map.class));
            sp.xadd("s1", StreamEntryID.NEW_ENTRY, JSON.parseObject("{'num':'" + ++n + "'}", Map.class));
            sp.xadd("s1", StreamEntryID.NEW_ENTRY, JSON.parseObject("{'num':'" + ++n + "'}", Map.class));
            sp.xadd("s1", StreamEntryID.NEW_ENTRY, JSON.parseObject("{'num':'" + ++n + "'}", Map.class));
            sp.xadd("s1", StreamEntryID.NEW_ENTRY, JSON.parseObject("{'num':'" + ++n + "'}", Map.class));
            Thread.sleep(3000);
        }
    }
}
