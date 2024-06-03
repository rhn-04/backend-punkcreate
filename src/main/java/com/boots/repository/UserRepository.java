package com.boots.repository;

import com.boots.entity.Post;
import com.boots.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u JOIN u.following f WHERE f.id = :userId")
    List<User> findFollowersById(@Param("userId") Long userId);

    @Query("SELECT p.id FROM User u JOIN u.likedPosts p WHERE u.id = :userId")
    List<Long> findLikedPostIdsByUserId(Long userId);

    @Query("SELECT p FROM Post p WHERE p.id IN :postIds")
    List<Post> findPostsByIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT f FROM User u JOIN u.following f WHERE u.id = :userId")
    List<User> findFollowingById(Long userId);

    @Query("SELECT p FROM Post p WHERE p.user.id IN (SELECT f.id FROM User u JOIN u.following f WHERE u.id = :userId) ORDER BY p.postDate DESC")
    List<Post> findFeedByUserId(Long userId);

    @Query("SELECT u FROM User u JOIN u.following f WHERE f.id = :userId")
    List<User> findFollowersByUserId(@Param("userId") Long userId);
    @Query("SELECT COUNT(f) FROM User u JOIN u.following f WHERE f.id = :userId")
    int countFollowersByUserId(@Param("userId") Long userId);
}
