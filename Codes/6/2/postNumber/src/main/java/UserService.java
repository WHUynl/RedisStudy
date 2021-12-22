import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import util.JedisFactory;
import java.util.HashSet;
import java.util.Set;
public class UserService {
    private JedisPool jedisPool = JedisFactory.getInstance().getJedisPool();

    /**
     * 关注
     *
     * @param userId
     * @param targetId
     */
    public void follow(int userId, int targetId) {
        String key1 = "following:" + userId;
        String key2 = "follower:" + targetId;

        /**
         1.使用事务保证A的关注以及B的关注者应该是同时成功执行的
         2.使用set而非list来存放关注、追随列表的原因：1.保证不重复 2.可以更轻松的查询共同关注
        **/
        Jedis jedis = jedisPool.getResource();
        Transaction tx = null;
        try {
            //保证两者一起被更新，加一个乐观锁
            jedis.watch(key1);
            tx = jedis.multi();
            tx.sadd(key1, String.valueOf(targetId));
            tx.sadd(key2, String.valueOf(userId));
            tx.exec();
        } catch (Exception e) {
            e.printStackTrace();
            if (tx != null) {
                //失败则丢弃事务
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

    /**
     * 获取粉丝数
     *
     * @param userId
     * @return
     */
    public long getFollowerCount(int userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "follower:" + userId;
            return jedis.scard(key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * 获取关注数
     *
     * @param userId
     * @return
     */
    public long getFollowingCount(int userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "following:" + userId;
            return jedis.scard(key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * 获取共同关注列表
     *
     * @param userId1
     * @param userId2
     * @return
     */
    public Set<Integer> getSameFollowing(int userId1, int userId2) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key1 = "following:" + userId1;
            String key2 = "following:" + userId2;
            Set<String> set = jedis.sinter(key1, key2);
            Set<Integer> result = new HashSet<>();
            for (String member : set) {
                result.add(Integer.valueOf(member));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
