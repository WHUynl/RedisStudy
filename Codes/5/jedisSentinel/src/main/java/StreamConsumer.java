import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntry;
import redis.clients.jedis.StreamEntryID;

import java.util.AbstractMap.SimpleEntry;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class StreamConsumer {

    private Jedis jedis;

    public StreamConsumer(Jedis jedis) {
        this.jedis = jedis;
    }

    public void consume(final String key, final StreamConsumerHandler handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                StreamEntryID streamEntryID = new StreamEntryID();
                while (true) {
                    List<Entry<String, List<StreamEntry>>> list =
                            jedis.xread(1, 5000, new SimpleEntry(key, streamEntryID));
                    if (list != null && list.size() > 0) {
                        List<StreamEntry> streamEntries = list.get(0).getValue();
                        handler.handle(streamEntries);
                        streamEntryID = streamEntries.get(streamEntries.size() - 1).getID();
                    }
                }
            }
        }).start();
    }

    public void consumeGroup(final String key, final String groupname,
                             final String consumer, final StreamConsumerHandler handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    List<Entry<String, List<StreamEntry>>> list =
                            jedis.xreadGroup(groupname, consumer, 1, 5000,
                                    false, new SimpleEntry(key, StreamEntryID.UNRECEIVED_ENTRY));
                    if (list != null && list.size() > 0) {
                        List<StreamEntry> streamEntries = list.get(0).getValue();
                        handler.handle(streamEntries);
                        for (StreamEntry entry : streamEntries) {
                            jedis.xack(key, groupname, entry.getID());
                        }
                    }
                }
            }
        }).start();
    }

}
