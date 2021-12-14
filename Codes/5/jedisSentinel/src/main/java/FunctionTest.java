import redis.clients.jedis.*;
import redis.clients.jedis.params.*;
import util.JedisFactory;

import java.util.Arrays;
import java.util.List;

public class FunctionTest {
    private JedisPool jedisPool = JedisFactory.getInstance().getJedisPool();

    //bitmap测试
    public void testBitmap(){
        try (Jedis jedis = jedisPool.getResource()){
            String key = "test:bm:01";
            String key2 = "test:bm:02";
            String key3 = "test:bm:03";

            //设置bitmap
            jedis.setbit(key,1,true);
            jedis.setbit(key,4,true);
            jedis.setbit(key,10,true);

            jedis.setbit(key2,1,true);
            jedis.setbit(key2,5,true);
            jedis.setbit(key2,8,true);

            jedis.setbit(key3,2,true);
            jedis.setbit(key3,4,true);
            jedis.setbit(key3,5,true);

            //查看bitmap的位
            System.out.println(jedis.getbit(key,0));//false
            System.out.println(jedis.getbit(key,10));//true
            System.out.println(jedis.getbit(key,12));//false

            //输出bitmap的1的个数
            System.out.println(jedis.bitcount(key));//3

            //bitmap或运算
            String key4 = "test:bm:04";
            jedis.bitop(BitOP.OR,key4,key,key2,key3);

            System.out.println(jedis.bitcount(key4));//6
        }catch (Exception e){

        }
    }

    //HyperLogLog测试
    void testHyperLogLog(){
        try (Jedis jedis = jedisPool.getResource()){
            String key1 = "test:hll:01";
            String key2 = "test:hll:02";
            //添加数据，201出现误差
            for(int i =0;i<200;i++){
                jedis.pfadd(key1,String.valueOf(i));
            }

            for(int i =100;i<300;i++){
                jedis.pfadd(key2,String.valueOf(i));
            }

            //获得数据
            System.out.println(jedis.pfcount(key1));
            System.out.println(jedis.pfcount(key2));

            //返回两个hll合并后的结果
            System.out.println(jedis.pfcount(key1,key2));
        }catch (Exception e){

        }
    }

    //GEO测试
    void testGEO(){
        try (Jedis jedis = jedisPool.getResource()){
            // 记录
            String key = "test:geo:china";
            jedis.geoadd(key, 116.418067, 39.91582, "北京");
            jedis.geoadd(key, 117.236746, 39.024619, "天津");
            jedis.geoadd(key, 118.184207, 39.639001, "唐山");
            jedis.geoadd(key, 119.600799, 39.944149, "秦皇岛");
            jedis.geoadd(key, 114.90029, 40.7815, "张家口");
            jedis.geoadd(key, 115.470606, 38.88815, "保定");
            jedis.geoadd(key, 114.513947, 38.056512, "石家庄");
            // 坐标
            System.out.println(jedis.geopos(key, "北京", "天津", "保定"));
            // 距离
            System.out.println(jedis.geodist(key, "北京", "天津"));
            System.out.println(jedis.geodist(key, "北京", "天津", GeoUnit.KM));
            // 半径
            GeoRadiusParam param = GeoRadiusParam.geoRadiusParam().withCoord().withDist();
            List<GeoRadiusResponse> list = jedis.georadiusByMember(
                    key, "北京", 200.0, GeoUnit.KM, param);
            for (GeoRadiusResponse response : list) {
                System.out.println(response.getMemberByString() + ", "
                        + response.getCoordinate() + ", " + response.getDistance());
            }
        }catch (Exception e){

        }
    }

    //PipeLine测试
    public void testPipeline() {
        try (Jedis jedis = jedisPool.getResource()) {
            //创建pipeline对象
            Pipeline pipeline = jedis.pipelined();
//            pipeline.set("count", "100");
//            pipeline.incr("count");
//            List<Object> list = pipeline.syncAndReturnAll();
//            for (Object obj : list) {
//                System.out.println(obj.toString());
//            }

            //创建string值 100
            pipeline.set("count", "100");
            //只接收这条命令的返回值
            Response<Long> rCount = pipeline.incr("count");
            //执行pipeline组合的命令
            pipeline.sync();
            System.out.println(rCount.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //测试事务
    public void testTransaction() {
        Jedis jedis = jedisPool.getResource();
        Transaction tx = null;
        try {
            String key = "test:long";
            //使用watch命令，一旦key的value被更改，则会返回一个报错，再来清空事务
            jedis.watch(key);

            //开启事务
            tx = jedis.multi();
            tx.set(key, "1");
            tx.incr(key);

            //执行事务
            tx.exec();
        } catch (Exception e) {
            e.printStackTrace();
            //事务不为空，则清空事务
            if (tx != null) {
                tx.discard();
            }
        } finally {
            if (tx != null) {
                tx.close();
            }
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    //测试Lua脚本
    public void testLuaScript() {
        try (Jedis jedis = jedisPool.getResource()) {
            // eval
            List<String> keys = Arrays.asList(new String[]{"hello"});
            List<String> args = Arrays.asList(new String[]{"Lua"});
            //注意此时不需要单独传入keys的个数
            jedis.eval("redis.call('set',KEYS[1],ARGV[1])", keys, args);
            System.out.println(jedis.get("hello"));
            // evalsha
            //获得脚本得到SHA码
            String sha1 = jedis.scriptLoad("for i=1,512 do redis.call('sadd',KEYS[1],i) end");
            jedis.evalsha(sha1, 1, "test:lua:set");
            System.out.println(jedis.smembers("test:lua:set"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //测试排序功能
    public void testSort() {
        try (Jedis jedis = jedisPool.getResource()) {
            // set1
            jedis.sadd("set1", "3.0", "2.0", "5.0", "1.0", "4.0");
            System.out.println("set1: " + jedis.smembers("set1"));

            // set2
            jedis.sadd("set2", "aaa", "bbb", "ccc", "ddd", "eee");
            System.out.println("set2: " + jedis.smembers("set2"));

            // users
            jedis.sadd("users", "aaa", "bbb", "ccc", "ddd", "eee");
            System.out.println("users: " + jedis.smembers("users"));

            // score
            jedis.set("score:aaa", "3.0");
            jedis.set("score:bbb", "2.0");
            jedis.set("score:ccc", "5.0");
            jedis.set("score:ddd", "1.0");
            jedis.set("score:eee", "4.0");
            System.out.println("score keys: " + jedis.keys("score:*"));

            // user info
            jedis.hset("user:aaa", "age", "23");
            jedis.hset("user:bbb", "age", "21");
            jedis.hset("user:ccc", "age", "25");
            jedis.hset("user:ddd", "age", "22");
            jedis.hset("user:eee", "age", "24");

            // 数字排序
            System.out.println("sorted set1: " + jedis.sort("set1"));

            // 字符串排序(new SortingParams().alpha().desc(),还表明了倒序，且结果也保存在new的内存中),
            System.out.println("sorted set2: " + jedis.sort("set2",
                    new SortingParams().alpha().desc().limit(0, 3)));

            // 返回外部的值（以将users的排序方式展示值）
            System.out.println("sorted users: " + jedis.sort("users",
                    new SortingParams().alpha().get("#").get("score:*").get("user:*->age")));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //测试自动过期
    public void testExpire() {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "test:expire";
//            jedis.set(key, "xxx");
//            jedis.expire(key, 10);
//            jedis.expireAt(key, System.currentTimeMillis() / 1000 + 10);
            jedis.setex(key, 10, "xxx");
            Thread.sleep(3000);
            System.out.println(jedis.ttl(key));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
