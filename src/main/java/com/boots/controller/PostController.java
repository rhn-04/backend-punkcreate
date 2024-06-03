package com.boots.controller;

import com.boots.DTO.*;
import com.boots.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class PostController {
    static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> createPost(@RequestParam("userId") Long userId,
                                        @RequestParam("title") String title,
                                        @RequestParam("description") String description,
                                        @RequestParam("tags") String[] tags,
                                        @RequestParam("isNSFW") boolean isNSFW,
                                        @RequestParam("images") MultipartFile[] images) {
        try {
            PostDTO postDTO = new PostDTO();
            postDTO.setUserId(userId);
            postDTO.setTitle(title);
            postDTO.setDescription(description);
            postDTO.setTags(List.of(tags));
            postDTO.setNSFW(isNSFW);
            postDTO.setImages(images);

            postService.createPost(postDTO);
            logger.info("Post created successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error creating post: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("post/{id}")
    public ResponseEntity<FullPostDTO> getFullPostById(@PathVariable Long id) {
        try {
            FullPostDTO fullPostDTO = postService.getFullPostDTOById(id);
            return ResponseEntity.ok(fullPostDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/")
    public List<FullPostDTO> getAllPosts() {
        return postService.getAllPosts();
    }

    @PostMapping("/post/edit/{postId}")
    public ResponseEntity<?> editPost(@PathVariable Long postId,
                                      @RequestParam Long userId,
                                      @RequestParam("isNSFW") boolean isNSFW,
                                      @ModelAttribute EditPostDTO editPostDTO) {
        try {
            editPostDTO.setNSFW(isNSFW);
            postService.editPost(editPostDTO);
            logger.info("Post edited successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error editing post: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId, @RequestBody LikeDTO likeDTO) {
        try {
            postService.likePost(likeDTO);
            return ResponseEntity.ok("Post liked successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error liking post");
        }
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<?> unlikePost(@PathVariable Long postId, @RequestBody LikeDTO likeDTO) {
        try {
            postService.unlikePost(likeDTO);
            return ResponseEntity.ok("Post unliked successfully");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error unliking post");
        }
    }

    @GetMapping("/post/{id}/likes")
    public ResponseEntity<List<UserPreviewDTO>> getPostLikes(@PathVariable Long id) {
        try {
            List<UserPreviewDTO> users = postService.getPostLikes(id);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching likes for post: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/isPostLikedByUser")
    public boolean isPostLikedByUser(@RequestBody LikeDTO likeRequest) {
        return postService.isPostLikedByUser(likeRequest);
    }



    @GetMapping("/search")
    public ResponseEntity<List<FullPostDTO>> searchPosts(
            @RequestParam(required = false, name = "title") String title,
            @RequestParam(required = false, name = "tag") String tag,
            @RequestParam(required = false, name = "sortBy") String sortBy) {
        System.out.println("Received search request with title: " + title + ", tag: " + tag + ", sortBy: " + sortBy);
        List<FullPostDTO> posts = postService.searchPosts(title, tag, sortBy);
        return ResponseEntity.ok(posts);
    }

    @DeleteMapping("/post/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, @RequestParam Long userId) {
        try {
            postService.deletePost(postId, userId);
            return ResponseEntity.ok("Post deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting post: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting post");
        }
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    @ResponseStatus
    public void handleOptions() {}
}