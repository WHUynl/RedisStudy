import redis.clients.jedis.*;
import java.io.Closeable;
import java.io.IOException;

public class PubSubProducer implements Closeable {
    private Jedis jedis;
    public PubSubProducer(Jedis jedis){
        this.jedis = jedis;
    }

    //向channel发布各种数据
    public Long publish(String channel , String message){
        return jedis.publish(channel,message);
    }

    @Override
    public void close() throws IOException{
        if(jedis != null){
            jedis.close();
        }
    }

}
