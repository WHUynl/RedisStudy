import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import util.JedisFactory;

public class PostService {
    private JedisPool jedisPool = JedisFactory.getInstance().getJedisPool();

    /**
     * 增加帖子的阅读数量
     *
     * @param postId
     */
    public void increaseReadCount(int postId) {
        String key = "post:read:count:" + postId;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.incr(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 增加帖子的点赞数量
     *
     * @param postId
     */
    public void increaseLikeCount(int postId) {
        String key = "post:like:count:" + postId;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.incr(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 减少帖子的点赞数量
     *
     * @param postId
     */
    public void decreaseLikeCount(int postId) {
        String key = "post:like:count:" + postId;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.decr(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
