import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import util.JedisFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DataService {

    private JedisPool jedisPool = JedisFactory.getInstance().getJedisPool();

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    /**
     * 将指定的IP计入UV
     * 使用HyperLogLog来存放数据，减少内存量
     * @param ip
     */
    public void recordUV(String ip) {
        if (ip == null || ip.length() == 0) {
            throw new IllegalArgumentException("参数为空!");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "uv:" + df.format(new Date());
            jedis.pfadd(key, ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 统计指定日期范围内的UV
     *
     * @param start
     * @param end
     * @return
     */
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数为空!");
        }

        // 整理该日期范围内的key
        List<String> keys = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = "uv:" + df.format(calendar.getTime());
            keys.add(key);
            calendar.add(Calendar.DATE, 1);
        }

        // 合并统计结果
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.pfcount(keys.toArray(new String[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    /**
     * 记录用户的在线状态
     *
     * @param userId
     */
    public void recordOnline(int userId) {
        Calendar calendar = Calendar.getInstance();
        int y = calendar.get(Calendar.YEAR);
        int n = calendar.get(Calendar.DAY_OF_YEAR);

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "online:" + y + ":" + userId;
            jedis.setbit(key, n - 1, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 统计用户的在线状态
     *
     * @param userId
     * @return
     */
    public int[] calculateOnline(int userId) {
        Calendar calendar = Calendar.getInstance();
        int y = calendar.get(Calendar.YEAR);
        int n = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "online:" + y + ":" + userId;
            Pipeline pipeline = jedis.pipelined();
            for (int i = 0; i < n; i++) {
                pipeline.getbit(key, i);
            }
            List<Object> list = pipeline.syncAndReturnAll();
            int[] flags = new int[n];
            for (int i = 0; i < n; i++) {
                flags[i] = Boolean.parseBoolean(list.get(i).toString()) ? 1 : 0;
            }
            return flags;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
