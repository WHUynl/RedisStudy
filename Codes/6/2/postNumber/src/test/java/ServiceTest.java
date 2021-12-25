import org.junit.Test;
import util.CommonUtil;


import java.util.*;


public class ServiceTest {
    PostService postService = new PostService();
    UserService userService = new UserService();
    DataService dataService = new DataService();

    @Test
    public void testIncreasePostReadCount() {
        postService.increaseReadCount(101);
        postService.increaseReadCount(101);
        postService.increaseReadCount(101);
    }

    @Test
    public void testIncreasePostLikeCount() {
        postService.increaseLikeCount(101);
        postService.increaseLikeCount(101);
        postService.increaseLikeCount(101);
    }

    @Test
    public void testDecreasePostLikeCount() {
        postService.decreaseLikeCount(101);
    }

    @Test
    public void testAddPostList() {
        for (int i = 0; i < 30; i++) {
            postService.addPostList(i + 1, (i + 1) * 10);
        }
    }

    @Test
    public void testGetPostList() {
        List<Integer> list = postService.getPostList();
        System.out.println(list);
    }

    @Test
    public void testFollow() {
        userService.follow(1, 8);
        userService.follow(2, 8);
        userService.follow(3, 8);
        userService.follow(1, 9);
        userService.follow(2, 9);
        userService.follow(9, 1);
        userService.follow(9, 2);
    }

    @Test
    public void testGetFollowCount() {
        System.out.println(userService.getFollowingCount(1));
        System.out.println(userService.getFollowerCount(1));
        System.out.println(userService.getSameFollowing(1, 2));
        System.out.println(userService.getSameFollowing(3, 2));
    }

    @Test
    public void testTags() {
        // 添加标签
        userService.addTags(1, "音乐");
        userService.addTags(1, "运动");
        userService.addTags(1, new String[]{"游戏", "交友", "鬼畜"});
        // 查询标签
        System.out.println(userService.getTags(1));
    }

    @Test
    public void testTodoList() {
        // 添加待办事项
        userService.addTodoItem(1, "看书");
        userService.addTodoItem(1, "购物");
        userService.addTodoItem(1, "定外卖");
        userService.addTodoItem(1, "打游戏");
        // 删除待办事项
        userService.delTodoItem(1, "打游戏");
        // 查询待办事项
        System.out.println(userService.getTodoList(1));
    }



    @Test
    public void testToken() throws InterruptedException {
        String token = userService.createToken(1);
        System.out.println(userService.validateToken(token));

        Thread.sleep(1000);
        System.out.println(userService.validateToken(token));

        System.out.println(userService.validateToken(CommonUtil.generateUUID()));
    }

    @Test
    public void testServiceAvailable() throws InterruptedException {
        System.out.println(userService.isAvailable("password", 1));
        System.out.println(userService.isAvailable("password", 1));
        System.out.println(userService.isAvailable("password", 1));
        System.out.println(userService.isAvailable("password", 1));
        System.out.println(userService.isAvailable("password", 1));
        System.out.println(userService.isAvailable("password", 1));

        Thread.sleep(1 * 1000);
        System.out.println(userService.isAvailable("password", 1));
    }

    @Test
    public void testRecordUV() {
        dataService.recordUV("139.9.119.1");
        dataService.recordUV("139.9.119.2");
        dataService.recordUV("139.9.119.3");
        dataService.recordUV("139.9.119.1");
        dataService.recordUV("139.9.119.2");
        dataService.recordUV("139.9.119.3");
        dataService.recordUV("139.9.119.1");
        dataService.recordUV("139.9.119.2");
        dataService.recordUV("139.9.119.3");
    }

    @Test
    public void testCalculateUV() {
        Date start = new Date("2021/12/1");
        Date end = new Date("2021/12/26");
        long uv = dataService.calculateUV(start, end);
        System.out.println(uv);
    }


    @Test
    public void testRecordOnline() {
        dataService.recordOnline(1);
        dataService.recordOnline(2);
        dataService.recordOnline(3);
        dataService.recordOnline(1);
        dataService.recordOnline(2);
        dataService.recordOnline(3);
        dataService.recordOnline(1);
        dataService.recordOnline(2);
        dataService.recordOnline(3);
    }

    @Test
    public void testCalculateOnline() {
        int[] flags = dataService.calculateOnline(1);
        for (int i = 0; i < flags.length; i++) {
            System.out.print(flags[i] + " ");
            if ((i + 1) % 20 == 0) System.out.println();
        }
    }
}
