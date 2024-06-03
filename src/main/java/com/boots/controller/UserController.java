package com.boots.controller;

import com.boots.DTO.*;
import com.boots.entity.User;
import com.boots.repository.UserRepository;
import com.boots.service.UserProfileServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.boots.controller.PostController.logger;

@RestController
@CrossOrigin
@RequestMapping("/users")
public class UserController {

    private final UserProfileServiceImpl userProfileService;
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserProfileServiceImpl userProfileService, UserRepository userRepository) {
        this.userProfileService = userProfileService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{username}")
    public AuthorDTO getAuthorProfile(@PathVariable String username) {
        return userProfileService.getAuthorProfile(username);
    }

    @PostMapping("/follow")
    public ResponseEntity<?> followUser(@RequestBody SubscriptionDTO subscriptionDTO) {
        logger.info("Request to follow user with id {} by user with id {}", subscriptionDTO.getTargetUserId(), subscriptionDTO.getUserId());
        userProfileService.followUser(subscriptionDTO);
        return ResponseEntity.ok("Successfully followed the user");
    }

    @PostMapping("/unfollow")
    public ResponseEntity<?> unfollowUser(@RequestBody SubscriptionDTO subscriptionDTO) {
        logger.info("Request to unfollow user with id {} by user with id {}", subscriptionDTO.getTargetUserId(), subscriptionDTO.getUserId());
        userProfileService.unfollowUser(subscriptionDTO);
        return ResponseEntity.ok("Successfully unfollowed the user");
    }

    @GetMapping("/isSubscribed")
    public ResponseEntity<Boolean> isUserSubscribedToUser(@RequestParam Long userId, @RequestParam Long targetUserId) {
        SubscriptionDTO subscriptionDTO = new SubscriptionDTO();
        subscriptionDTO.setUserId(userId);
        subscriptionDTO.setTargetUserId(targetUserId);
        boolean isSubscribed = userProfileService.isUserSubscribedToUser(subscriptionDTO);
        return ResponseEntity.ok(isSubscribed);
    }

    @GetMapping("/{userId}/likedPosts")
    public List<FullPostDTO> getLikedPosts(@PathVariable Long userId) {
        List<Long> likedPostIds = userRepository.findLikedPostIdsByUserId(userId);
        return userProfileService.getFullPostsByIds(likedPostIds);
    }

    @PostMapping("/{username}/profile/update")
    public ResponseEntity<?> updateUserProfile(@PathVariable String username,
                                               @ModelAttribute @Validated UpdateProfileDTO updateProfileDTO,
                                               @RequestParam(name = "avatar", required = false) MultipartFile avatarFile,
                                               BindingResult bindingResult) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                logger.error("Authentication object is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            logger.info("Authentication object: {}", authentication);

            if (avatarFile != null && !avatarFile.isEmpty()) {
                updateProfileDTO.setAvatarFileName(avatarFile.getOriginalFilename());
                updateProfileDTO.setAvatarFile(avatarFile);
            }

            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body("Validation error: " + bindingResult.getAllErrors());
            }

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

            userProfileService.updateUserProfile(user, updateProfileDTO);

            return ResponseEntity.ok("Profile updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during profile update: " + e.getMessage());
        }
    }
    @GetMapping("/{userId}/followers")
    public List<SubscriberDTO> getFollowers(@PathVariable Long userId) {
        return userProfileService.getFollowersDTO(userId);
    }

    @GetMapping("/{userId}/following")
    public List<SubscriberDTO> getFollowing(@PathVariable Long userId) {
        return userProfileService.getFollowingDTO(userId);
    }

    @GetMapping("/{userId}/feed")
    public List<FullPostDTO> getFeed(@PathVariable Long userId, @RequestHeader("Authorization") String token) {
        return userProfileService.getFeed(userId);
    }
}