import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import util.JedisFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    /**
     * 添加热门帖子
     *
     * @param postId
     * @param score
     */
    public void addPostList(int postId, double score) {
        String key = "post:list";
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zadd(key, score, String.valueOf(postId));
            //redis的key的集合数量超过10个就删除最小的那一个
            if (jedis.zcard(key) > 10) {
                jedis.zpopmin(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回热门帖子列表
     *
     * @return
     */
    public List<Integer> getPostList() {
        String key = "post:list";
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> set = jedis.zrevrange(key, 0, -1);
            List<Integer> list = new ArrayList<>();
            for (String id : set) {
                list.add(Integer.valueOf(id));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
