package com.boots.service;

import com.boots.DTO.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface PostService {
     void createPost(PostDTO postDTO);

    FullPostDTO getFullPostDTOById(Long id);

    List<FullPostDTO> getAllPosts();

    @Transactional
    void editPost(EditPostDTO editPostDTO);

    @Transactional
    void likePost(LikeDTO likeDTO);

    @Transactional
    void unlikePost(LikeDTO likeDTO);

    public boolean isPostLikedByUser(LikeDTO likeRequest);

    List<UserPreviewDTO> getPostLikes(Long postId);

    List<CommentResponseDTO> getCommentsByPostId(Long postId);


    List<FullPostDTO> searchPosts(String title, String tag, String sortBy);

    @Transactional
    void deletePost(Long postId, Long userId);
}

