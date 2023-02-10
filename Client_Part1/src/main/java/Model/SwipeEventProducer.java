package Model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.RandomStringUtils;
  public class SwipeEventProducer implements Runnable {
    private static final int MIN_SWIPER_ID = 1;
    private static final int MAX_SWIPER_ID = 5000;
    private static final int MIN_SWIPEE_ID = 1;
    private static final int MAX_SWIPEE_ID = 1000000;
    private static final int MIN_COMMENT_LENGTH = 1;
    private static final int MAX_COMMENT_LENGTH = 256;

    private BlockingQueue<SwipeEvent> swipeEvents;
    private int totalPosts;

    public  SwipeEventProducer(BlockingQueue<SwipeEvent> swipeEvents, int totalPosts) {
      this.swipeEvents = swipeEvents;
      this.totalPosts = totalPosts;
    }

    @Override
    public void run() {
      for (int i = 0; i < totalPosts; i++) {
        String swipe = genRandomSwipe();
        String swiperId = String.valueOf(
            ThreadLocalRandom.current().nextInt(MIN_SWIPER_ID, MAX_SWIPER_ID + 1));
        String swipeeId = String.valueOf(ThreadLocalRandom.current().nextInt(MIN_SWIPEE_ID, MAX_SWIPEE_ID + 1));
        String comment = genRandomComment();
        SwipeEvent swipeEvent = new SwipeEvent(swipe, swiperId, swipeeId, comment);
        swipeEvents.offer(swipeEvent);
      }
    }

    private String genRandomSwipe() {
      return ThreadLocalRandom.current().nextBoolean() ? "left" : "right";
    }

    private String genRandomComment() {
      int commentLength = ThreadLocalRandom.current().nextInt(MIN_COMMENT_LENGTH, MAX_COMMENT_LENGTH + 1);
      return RandomStringUtils.randomAlphanumeric(commentLength);
    }
  }
