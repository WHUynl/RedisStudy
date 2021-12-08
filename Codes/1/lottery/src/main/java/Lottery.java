import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import util.JedisFactory;

import java.util.Set;

//使用的集合来存储，原因：1.保证用户不重复，2.随机弹出
public class Lottery {
    private JedisPool jedisPool = JedisFactory.getInstance().getJedisPool();
    private String key = "user:lottery";

    public Lottery() {

    }

    //添加用户
    void addUser(String... users){
        if(users==null||users.length==0){
            throw new RuntimeException("至少传入一个用户！/n");
        }
        try(Jedis lotteryJedis = jedisPool.getResource()){
            lotteryJedis.sadd(key,users);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //清空奖池,直接删除KEY即可
    void clear(){
        try(Jedis lotteryJedis = jedisPool.getResource()){
            lotteryJedis.del(key);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //抽取用户
    Set<String> getLotteryUsers(int n){
        if(n<=0){
            throw new RuntimeException("至少传入一个用户！/n");
        }
        try(Jedis lotteryJedis = jedisPool.getResource()){
            long count = lotteryJedis.scard(key);
            if(count < n){
                throw new RuntimeException("无法抽出这么多用户！/n");
            }
            return lotteryJedis.spop(key,n);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
