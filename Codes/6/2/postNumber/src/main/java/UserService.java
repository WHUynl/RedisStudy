import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import util.JedisFactory;
import java.util.*;
import util.CommonUtil;

public class UserService {
    private JedisPool jedisPool = JedisFactory.getInstance().getJedisPool();
    private Map<String, Integer> serviceMap = new HashMap<>();

    public UserService() {
        serviceMap.put("request", 20); // 普通请求
        serviceMap.put("password", 5); // 输入密码
    }
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


    /**
     * 添加标签
     *
     * @param userId
     * @param tags
     */
    public void addTags(int userId, String... tags) {
        if (tags == null || tags.length == 0) {
            throw new IllegalArgumentException("参数为空!");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "tags:" + userId;
            //使用pipeline流水线来完成标签列表的大量增加，加速实现
            Pipeline pipeline = jedis.pipelined();
            for (String tag : tags) {
                pipeline.sadd(key, tag);
            }
            pipeline.syncAndReturnAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取标签
     *
     * @param userId
     * @return
     */
    public Set<String> getTags(int userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "tags:" + userId;
            return jedis.smembers(key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    //使用list来存放待办事项
    /**
     * 添加待办事项
     *
     * @param userId
     * @param item
     */
    public void addTodoItem(int userId, String item) {
        if (item == null || item.length() == 0) {
            throw new IllegalArgumentException("参数为空!");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "todo:list:" + userId;
            jedis.rpush(key, item);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 移除待办事项
     *
     * @param userId
     * @param item
     */
    public void delTodoItem(int userId, String item) {
        if (item == null || item.length() == 0) {
            throw new IllegalArgumentException("参数为空!");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "todo:list:" + userId;
            jedis.lrem(key, 1, item);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取待办事项
     *
     * @param userId
     * @return
     */
    public List<String> getTodoList(int userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "todo:list:" + userId;
            return jedis.lrange(key, 0, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     *
     *
     * Redis解决的是分布式服务器不方便共享时，则可以均存放到Redis里面
     *
     **/
    /**
     * 创建令牌
     *
     * @param userId
     * @return
     */
    public String createToken(int userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String token = CommonUtil.generateUUID();
            String key = "token:" + token;
            jedis.setex(key, 10, String.valueOf(userId));
            return token;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 验证令牌
     *
     * @param token
     * @return
     */
    public Integer validateToken(String token) {
        if (token == null || token.length() == 0) {
            throw new IllegalArgumentException("参数为空!");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "token:" + token;
            String userId = jedis.get(key);
            if (userId != null) {
                return Integer.valueOf(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 是否可用
     *
     * @param service
     * @param userId
     * @return
     */
    public boolean isAvailable(String service, int userId) {
        if (service == null || service.length() == 0) {
            throw new IllegalArgumentException("参数为空!");
        }

        boolean result = false;
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "times:" + service + ":" + userId;
            String times = jedis.get(key);
            if (times == null) {
                result = true;
                jedis.setex(key, 15, String.valueOf(serviceMap.get(service)));
            } else {
                result = Integer.valueOf(times) > 0;
            }
            jedis.decr(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
