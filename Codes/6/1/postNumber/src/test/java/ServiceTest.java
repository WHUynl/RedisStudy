import org.junit.Test;

public class ServiceTest {
    PostService postService = new PostService();

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
}
