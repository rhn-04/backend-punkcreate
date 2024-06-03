package com.boots.service;

import com.boots.DTO.CommentDTO;
import com.boots.DTO.CommentResponseDTO;
import com.boots.entity.Comment;
import com.boots.entity.Post;
import com.boots.entity.User;
import com.boots.repository.CommentRepository;
import com.boots.repository.PostRepository;
import com.boots.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService{
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Override
    public Comment addComment(CommentDTO commentDTO) {
        User user = userRepository.findById(commentDTO.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(commentDTO.getPostId()).orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPost(post);
        comment.setCommentText(commentDTO.getCommentText());
        comment.setCommentDate(new Date());

        return commentRepository.save(comment);
    }

    @Override
    public List<CommentResponseDTO> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream()
                .map(this::convertToCommentResponseDTO)
                .collect(Collectors.toList());
    }

    private CommentResponseDTO convertToCommentResponseDTO(Comment comment) {
        CommentResponseDTO commentResponseDTO = new CommentResponseDTO();
        commentResponseDTO.setCommentId(comment.getId());
        commentResponseDTO.setUserId(comment.getUser().getId());
        commentResponseDTO.setUsername(comment.getUser().getUsername());
        commentResponseDTO.setUserAvatar(comment.getUser().getAvatar());
        commentResponseDTO.setCommentText(comment.getCommentText());
        commentResponseDTO.setCommentDate(comment.getCommentDate());
        commentResponseDTO.setPostId(comment.getPost().getId());
        return commentResponseDTO;
    }

    @Override
    public boolean isUserAllowedToDeleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return false;
        }
        Post post = comment.getPost();
        return comment.getUser().getId().equals(userId) || post.getUser().getId().equals(userId);
    }

    @Override
    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    @Override
    public boolean isUserAllowedToEditComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        return comment != null && comment.getUser().getId().equals(userId);
    }

    @Override
    public void editComment(Long commentId, CommentDTO commentDTO) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            comment.setCommentText(commentDTO.getCommentText());
            commentRepository.save(comment);
        }
    }
}
