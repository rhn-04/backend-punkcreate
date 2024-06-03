package com.boots.controller;

import com.boots.DTO.CommentDTO;
import com.boots.DTO.CommentResponseDTO;
import com.boots.entity.Comment;
import com.boots.service.CommentService;
import com.boots.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;
    @Autowired
    private PostService postService;

    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody CommentDTO commentDTO) {
        try {
            Comment comment = commentService.addComment(commentDTO);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при добавлении комментария: " + e.getMessage());
        }
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> getCommentsByPostId(@PathVariable Long postId) {
        try {
            List<CommentResponseDTO> comments = postService.getCommentsByPostId(postId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при получении комментариев: " + e.getMessage());
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        try {
            if (!commentService.isUserAllowedToDeleteComment(commentId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Вы не можете удалить этот комментарий.");
            }
            commentService.deleteComment(commentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удалении комментария: " + e.getMessage());
        }
    }

    @PostMapping("/{commentId}")
    public ResponseEntity<?> editComment(@PathVariable Long commentId, @RequestParam Long userId, @RequestBody CommentDTO commentDTO) {
        try {
            if (!commentService.isUserAllowedToEditComment(commentId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Вы не можете редактировать этот комментарий.");
            }
            commentService.editComment(commentId, commentDTO);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при редактировании комментария: " + e.getMessage());
        }
    }
}

