package Model;

import java.util.concurrent.ConcurrentHashMap;

public class UserLikesDislikes {
  private ConcurrentHashMap<String, LikesDislikes> userLikesDislikesMap;

  public UserLikesDislikes() {
    userLikesDislikesMap = new ConcurrentHashMap<>();
  }

  public void addUserLike(String userId) {
    userLikesDislikesMap.computeIfAbsent(userId, k -> new LikesDislikes()).addLike();
  }

  public void addUserDislike(String userId) {
    userLikesDislikesMap.computeIfAbsent(userId, k -> new LikesDislikes()).addDislike();
  }

  public int getUserLikes(String userId) {
    LikesDislikes ld = userLikesDislikesMap.get(userId);
    return ld == null ? 0 : ld.getLikes();
  }

  public int getUserDislikes(String userId) {
    LikesDislikes ld = userLikesDislikesMap.get(userId);
    return ld == null ? 0 : ld.getDislikes();
  }

  private class LikesDislikes {
    private int likes;
    private int dislikes;

    public LikesDislikes() {
      likes = 0;
      dislikes = 0;
    }

    public synchronized void addLike() {
      likes++;
    }

    public synchronized void addDislike() {
      dislikes++;
    }

    public synchronized int getLikes() {
      return likes;
    }

    public synchronized int getDislikes() {
      return dislikes;
    }
  }
}
