package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PotentialMatches {
  private ConcurrentHashMap<String, List<String>> swipeMap;
  private ReadWriteLock lock;

  public PotentialMatches() {
    swipeMap = new ConcurrentHashMap<>();
    lock = new ReentrantReadWriteLock();
  }

  public void swipeRight(String swipingUser, String swipedUser) {
    lock.writeLock().lock();
    try {
      List<String> swipedUsers = swipeMap.get(swipingUser);
      if (swipedUsers == null) {
        swipedUsers = new ArrayList<>();
        swipeMap.put(swipingUser, swipedUsers);
      }
      if (!swipedUsers.contains(swipedUser)) {
        swipedUsers.add(swipedUser);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public List<String> getPotentialMatches(String userId) {
    lock.readLock().lock();
    try {
      List<String> swipedUsers = swipeMap.get(userId);
      if (swipedUsers == null) {
        return new ArrayList<>();
      } else {
        int endIndex = Math.min(swipedUsers.size(), 100);
        return swipedUsers.subList(0, endIndex);
      }
    } finally {
      lock.readLock().unlock();
    }
  }
}
