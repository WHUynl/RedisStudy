import redis.clients.jedis.*;
import java.io.Closeable;
import java.io.IOException;

public class PubSubConsumer implements Closeable{
    private Jedis jedis;

    public PubSubConsumer(Jedis jedis){
        this.jedis = jedis;
    }

    //订阅频道
    //注意JedisPubSub只是一个jedis封装的抽象类，有调用者去实现具体细节
    public void subscribe(JedisPubSub handel,String... channels){
        if(handel == null||channels == null){
            return ;
        }
        //注意一旦订阅会导致该线程阻塞，所以我们需要新创建一个线程来进行订阅
        new Thread(new Runnable() {
            @Override
            public void run() {
                jedis.subscribe(handel,channels);
            }
        }).start();
    }

    public void unsubscribe(JedisPubSub handel,String... channels){
        handel.unsubscribe(channels);
    }


    //模式订阅频道
    //注意JedisPubSub只是一个jedis封装的抽象类，有调用者去实现具体细节
    public void subscribeByPattern(JedisPubSub handel,String... patterns){
        if(handel == null||patterns == null){
            return ;
        }
        //注意一旦订阅会导致该线程阻塞，所以我们需要新创建一个线程来进行订阅
        new Thread(new Runnable() {
            @Override
            public void run() {
                jedis.psubscribe(handel,patterns);
            }
        }).start();
    }

    public void unsubscribeByPattern(JedisPubSub handel,String... patterns){
        handel.punsubscribe(patterns);
    }


    @Override
    public void close() throws IOException{
        if(jedis != null){
            jedis.close();
        }
    }
}
