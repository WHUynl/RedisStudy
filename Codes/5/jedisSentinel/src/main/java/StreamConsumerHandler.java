import redis.clients.jedis.StreamEntry;

import java.util.List;

public interface StreamConsumerHandler {

    void handle(List<StreamEntry> list);

}
