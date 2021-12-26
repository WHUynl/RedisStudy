import util.RedissonFactory;
import org.junit.Test;
import org.redisson.api.*;

import java.util.Collection;

public class RedissonTest {

    RedissonClient client = RedissonFactory.getInstance().getClusterClient();

    /**
     * Redisson的基本用法
     */
    @Test
    public void testRedisson() {
        // string
        RBucket<String> bucket = client.getBucket("test:string");
        bucket.set("hello");
        System.out.println(bucket.get());

        RAtomicLong atomicLong = client.getAtomicLong("test:long");
        atomicLong.set(100L);
        atomicLong.incrementAndGet();
        System.out.println(atomicLong.get());

        RAtomicDouble atomicDouble = client.getAtomicDouble("test:double");
        atomicDouble.set(200.00);
        atomicDouble.incrementAndGet();
        System.out.println(atomicDouble.get());

        // hash
        RMap<String, String> map = client.getMap("test:hash");
        map.put("name", "Tom");
        map.put("age", "23");
        System.out.println(map.get("name") + ", " + map.get("age"));

        // list
        RList<String> list = client.getList("test:list");
        list.add("xxx");
        list.add("yyy");
        list.add("zzz");
        System.out.println(list.get(0));
        System.out.println(list.get(1));
        System.out.println(list.get(2));

        // set
        RSet<String> set = client.getSet("test:set");
        set.add("aaa");
        set.add("bbb");
        set.add("ccc");
        System.out.println(set.removeRandom());
        System.out.println(set.removeRandom());
        System.out.println(set.removeRandom());

        // zset
        RScoredSortedSet<String> zset = client.getScoredSortedSet("test:zset");
        zset.add(80, "Lucy");
        zset.add(90, "Tony");
        zset.add(70, "John");
        Collection<String> collection = zset.valueRange(0, -1);
        System.out.println(collection);
    }

}
