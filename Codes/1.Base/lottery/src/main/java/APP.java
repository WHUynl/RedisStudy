import redis.clients.jedis.JedisPool;

import java.util.Set;

public class APP {
    public static void main(String[] args) {
        Lottery lottery = new Lottery();
        lottery.clear();
        lottery.addUser("ab","ss","sa","aa","cwdcc","dcw","sdc");

        Set<String> Lucks = lottery.getLotteryUsers(2);

        for(String luck:Lucks){
            System.out.println(luck);
        }
    }
}
