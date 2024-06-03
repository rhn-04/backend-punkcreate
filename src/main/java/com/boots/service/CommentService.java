package com.boots.service;

import com.boots.DTO.CommentDTO;
import com.boots.DTO.CommentResponseDTO;
import com.boots.entity.Comment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CommentService {
    Comment addComment(CommentDTO commentDTO);

    List<CommentResponseDTO> getCommentsByPostId(Long postId);

    boolean isUserAllowedToDeleteComment(Long commentId, Long userId);

    void deleteComment(Long commentId);

    boolean isUserAllowedToEditComment(Long commentId, Long userId);

    void editComment(Long commentId, CommentDTO commentDTO);
}
