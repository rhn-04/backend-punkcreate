package com.boots.service;

import com.boots.DTO.*;
import com.boots.entity.Comment;
import com.boots.entity.Post;
import com.boots.entity.Tag;
import com.boots.entity.User;
import com.boots.repository.CommentRepository;
import com.boots.repository.PostRepository;
import com.boots.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserProfileServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public UserProfileServiceImpl(UserRepository userRepository, PostRepository postRepository, CommentRepository commentRepository, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getName())
                .build();
    }

    public AuthorDTO getAuthorProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AuthorDTO authorDTO = new AuthorDTO();
        authorDTO.setId(user.getId());
        authorDTO.setUsername(user.getUsername());
        authorDTO.setAvatar(user.getAvatar());
        authorDTO.setDescription(user.getDescription());
        authorDTO.setBirthDate(user.getBirthDate());

        List<User> followers = userRepository.findFollowersById(user.getId());
        authorDTO.setFollowersCount(followers.size());
        authorDTO.setFollowingCount(user.getFollowing().size());

        List<Post> userPosts = postRepository.findByUser(user);
        authorDTO.setPostsCount(userPosts.size());

        List<FullPostDTO> postDTOs = userPosts.stream()
                .map(this::convertToFullPostDTO)
                .sorted((p1, p2) -> p2.getPostDate().compareTo(p1.getPostDate()))
                .collect(Collectors.toList());

        authorDTO.setPosts(postDTOs);

        return authorDTO;
    }

    private FullPostDTO convertToFullPostDTO(Post post) {
        FullPostDTO fullPostDTO = new FullPostDTO();
        fullPostDTO.setPostId(post.getId());
        fullPostDTO.setUserId(post.getUser().getId());
        fullPostDTO.setUsername(post.getUser().getUsername());
        fullPostDTO.setTitle(post.getTitle());
        fullPostDTO.setDescription(post.getDescription());
        fullPostDTO.setTags(post.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
        fullPostDTO.setNSFW(post.isNSFW());
        fullPostDTO.setPostDate(post.getPostDate());
        List<String> imageUrls = postRepository.findImageUrlsByPostId(post.getId());
        fullPostDTO.setImages(imageUrls);
        int likesCount = post.getLikes().size();
        fullPostDTO.setLikes(likesCount);
        List<CommentResponseDTO> comments = commentRepository.findByPostId(post.getId()).stream()
                .map(this::convertToCommentResponseDTO)
                .collect(Collectors.toList());
        fullPostDTO.setComments(comments);
        return fullPostDTO;
    }

    private CommentResponseDTO convertToCommentResponseDTO(Comment comment) {
        CommentResponseDTO commentResponseDTO = new CommentResponseDTO();
        commentResponseDTO.setCommentId(comment.getId());
        commentResponseDTO.setPostId(comment.getPost().getId());
        commentResponseDTO.setUsername(comment.getUser().getUsername());
        commentResponseDTO.setCommentText(comment.getCommentText());
        commentResponseDTO.setCommentDate(comment.getCommentDate());
        return commentResponseDTO;
    }

    @PersistenceContext
    private EntityManager entityManager;


    public void followUser(SubscriptionDTO subscriptionDTO) {
        Long userId = subscriptionDTO.getUserId();
        Long targetUserId = subscriptionDTO.getTargetUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с id " + userId + " не найден"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с id " + targetUserId + " не найден"));

        user.getFollowing().add(targetUser);
        userRepository.save(user);
    }

    public void unfollowUser(SubscriptionDTO subscriptionDTO) {
        Long userId = subscriptionDTO.getUserId();
        Long targetUserId = subscriptionDTO.getTargetUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с id " + userId + " не найден"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с id " + targetUserId + " не найден"));

        user.getFollowing().remove(targetUser);
        userRepository.save(user);
    }

    public boolean isUserSubscribedToUser(SubscriptionDTO subscriptionDTO) {
        Long userId = subscriptionDTO.getUserId();
        Long targetUserId = subscriptionDTO.getTargetUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с id " + userId + " не найден"));

        return user.getFollowing().stream().anyMatch(following -> following.getId().equals(targetUserId));
    }

    public List<FullPostDTO> getFullPostsByIds(List<Long> postIds) {
        List<Post> posts = postRepository.findByIdIn(postIds);
        return posts.stream().map(this::convertToFullPostDTO).collect(Collectors.toList());
    }

    public void updateUserProfile(User user, UpdateProfileDTO updateProfileDTO) {
        user.setUsername(updateProfileDTO.getUsername());
        user.setDescription(updateProfileDTO.getDescription());
        user.setBirthDate(updateProfileDTO.getBirthDate());

        if (updateProfileDTO.getAvatarFile() != null) {
            if (user.getAvatar() != null) {
                fileStorageService.deleteFileByUrl(user.getAvatar());
            }
            String storedFileName = fileStorageService.storeFile(updateProfileDTO.getAvatarFile());
            user.setAvatar(storedFileName);
        }

        userRepository.save(user);
    }

    public List<SubscriberDTO> getFollowersDTO(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<User> followers = userRepository.findFollowersByUserId(userId);
        return followers.stream()
                .map(this::convertToSubscriberDTO)
                .collect(Collectors.toList());
    }

    public List<SubscriberDTO> getFollowingDTO(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<User> following = user.getFollowing();
        return following.stream()
                .map(this::convertToSubscriberDTO)
                .collect(Collectors.toList());
    }

    private SubscriberDTO convertToSubscriberDTO(User user) {
        SubscriberDTO subscriberDTO = new SubscriberDTO();
        subscriberDTO.setId(user.getId());
        subscriberDTO.setUsername(user.getUsername());
        subscriberDTO.setAvatar(user.getAvatar());
        subscriberDTO.setFollowersCount(userRepository.countFollowersByUserId(user.getId()));
        return subscriberDTO;
    }

    public List<FullPostDTO> getFeed(Long userId) {
        List<Post> posts = userRepository.findFeedByUserId(userId);
        return posts.stream().map(this::convertToFullPostDTO).collect(Collectors.toList());
    }
}
