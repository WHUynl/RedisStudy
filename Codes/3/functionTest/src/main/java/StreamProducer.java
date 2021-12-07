import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public class StreamProducer implements Closeable {

    private Jedis jedis;

    public StreamProducer(Jedis jedis) {
        this.jedis = jedis;
    }

    public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> data) {
        return jedis.xadd(key, id, data);
    }

    @Override
    public void close() throws IOException {
        if (jedis != null) {
            jedis.close();
        }
    }

}
