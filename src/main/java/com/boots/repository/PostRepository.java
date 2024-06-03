package com.boots.repository;

import com.boots.entity.Post;
import com.boots.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT pi.imageUrl FROM PostImage pi WHERE pi.post.id = :postId")
    List<String> findImageUrlsByPostId(Long postId);

    List<Post> findAllByOrderByPostDateDesc();

    @Modifying
    @Query(value = "INSERT INTO likes (post_id, user_id) " +
            "SELECT :postId, :userId " +
            "WHERE NOT EXISTS (SELECT 1 FROM likes WHERE post_id = :postId AND user_id = :userId)",
            nativeQuery = true)
    void likePost(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM likes WHERE post_id = :postId AND user_id = :userId", nativeQuery = true)
    void unlikePost(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("SELECT u FROM Post p JOIN p.likes u WHERE p.id = :postId")
    List<User> findUsersWhoLikedPost(Long postId);

    List<Post> findByUser(User user);
    List<Post> findByIdIn(List<Long> postIds);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.tags WHERE p.id IN :postIds")
    List<Post> findPostsWithTagsByIds(@Param("postIds") List<Long> postIds);
    List<Post> findByTitleContainingIgnoreCaseAndTagsNameContainingIgnoreCase(String title, String tag, Sort sort);
    List<Post> findByTitleContainingIgnoreCase(String title, Sort sort);
    List<Post> findByTagsNameContainingIgnoreCase(String tag, Sort sort);

    @Query("SELECT p, COUNT(l) as likeCount " +
            "FROM Post p LEFT JOIN p.likes l " +
            "WHERE (:title IS NULL OR p.title LIKE %:title%) " +
            "AND (:tag IS NULL OR :tag MEMBER OF p.tags) " +
            "GROUP BY p.id " +
            "ORDER BY likeCount DESC")
    List<Object[]> findPostsWithLikeCount(@Param("title") String title, @Param("tag") String tag);

}
