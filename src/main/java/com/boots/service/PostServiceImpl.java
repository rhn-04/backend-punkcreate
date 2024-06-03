package com.boots.service;

import com.boots.DTO.*;
import com.boots.entity.*;
import com.boots.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, PostImageRepository postImageRepository, TagRepository tagRepository, UserRepository userRepository, CommentRepository commentRepository, FileStorageService fileStorageService) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public void createPost(PostDTO postDTO) {
        Long userId = postDTO.getUserId();
        logger.info("Start creating post for user with id {}", userId);

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            logger.info("User found: {}", optionalUser.get());

            User user = optionalUser.get();
            Post post = new Post();
            post.setUser(user);
            post.setTitle(postDTO.getTitle());
            post.setDescription(postDTO.getDescription());
            post.setNSFW(postDTO.isNSFW());
            post.setPostDate(new Date());

            post = postRepository.save(post);

            List<PostImage> postImages = new ArrayList<>();
            for (MultipartFile image : postDTO.getImages()) {
                String storedImageUrl = fileStorageService.storeFile(image);
                PostImage postImage = new PostImage();
                postImage.setPost(post);
                postImage.setImageUrl(storedImageUrl);
                postImages.add(postImage);
            }
            postImageRepository.saveAll(postImages);

            for (String tagName : postDTO.getTags()) {
                Tag tag = tagRepository.findByName(tagName);
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(tagName);
                    tagRepository.save(tag);
                }
                post.getTags().add(tag);
            }

            postRepository.save(post);
            logger.info("Post created successfully");
        } else {
            logger.error("User with id {} not found", userId);
            throw new IllegalArgumentException("Пользователь с id " + userId + " не найден");
        }
    }

    @Override
    public FullPostDTO getFullPostDTOById(Long id) {
        Optional<Post> optionalPost = postRepository.findById(id);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            return convertToFullPostDTO(post);
        } else {
            throw new IllegalArgumentException("Post not found with id: " + id);
        }
    }

    @Override
    public List<FullPostDTO> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByPostDateDesc();
        return posts.stream()
                .map(this::convertToFullPostDTO)
                .collect(Collectors.toList());
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
        List<CommentResponseDTO> list = new ArrayList<>();
        for (Comment comment : commentRepository.findByPostId(post.getId())) {
            CommentResponseDTO commentResponseDTO = convertToCommentResponseDTO(comment);
            list.add(commentResponseDTO);
        }
        fullPostDTO.setComments(list);
        return fullPostDTO;
    }

    @Override
    @Transactional
    public void editPost(EditPostDTO editPostDTO) {
        Long postId = editPostDTO.getPostId();
        logger.info("Start editing post with id {}", postId);

        Optional<Post> optionalPost = postRepository.findById(postId);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();
            User user = post.getUser();
            if (user.getId().equals(editPostDTO.getUserId())) {
                logger.info("User is the author of the post");
                List<String> newImageUrls = new ArrayList<>();
                if (Objects.nonNull(editPostDTO.getImages()) && editPostDTO.getImages().length > 0) {
                    for (MultipartFile image : editPostDTO.getImages()) {
                        String storedImageUrl = fileStorageService.storeFile(image);
                        newImageUrls.add(storedImageUrl);
                    }
                }

                List<PostImage> oldImages = postImageRepository.findByPostId(postId);
                for (PostImage oldImage : oldImages) {
                    fileStorageService.deleteFileByUrl(oldImage.getImageUrl());
                    postImageRepository.delete(oldImage);
                }

                List<PostImage> postImages = new ArrayList<>();
                for (String imageUrl : newImageUrls) {
                    PostImage postImage = new PostImage();
                    postImage.setPost(post);
                    postImage.setImageUrl(imageUrl);
                    postImages.add(postImage);
                }
                postImageRepository.saveAll(postImages);

                logger.info("Value of isNSFW before setting: {}", editPostDTO.isNSFW());
                post.setNSFW(editPostDTO.isNSFW());
                logger.info("Value of isNSFW after setting: {}", post.isNSFW());

                post.setTitle(editPostDTO.getTitle());
                post.setDescription(editPostDTO.getDescription());

                List<Tag> tags = new ArrayList<>();
                for (String tagName : editPostDTO.getTags()) {
                    Tag tag = tagRepository.findByName(tagName);
                    if (tag == null) {
                        tag = new Tag();
                        tag.setName(tagName);
                        tagRepository.save(tag);
                    }
                    tags.add(tag);
                }
                post.setTags(tags);

                postRepository.save(post);
                logger.info("Post edited successfully");
            } else {
                logger.error("User is not the author of the post");
                throw new IllegalArgumentException("User is not the author of this post");
            }
        } else {
            logger.error("Post not found with id {}", postId);
            throw new IllegalArgumentException("Post not found with id " + postId);
        }
    }

    @Override
    public void likePost(LikeDTO likeDTO) {
        postRepository.likePost(likeDTO.getPostId(), likeDTO.getUserId());
    }

    @Override
    public void unlikePost(LikeDTO likeDTO) {
        postRepository.unlikePost(likeDTO.getPostId(), likeDTO.getUserId());
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean isPostLikedByUser(LikeDTO likeDTO) {
        String queryStr = "SELECT COUNT(p) > 0 FROM Post p JOIN p.likes u WHERE p.id = :postId AND u.id = :userId";
        TypedQuery<Boolean> query = entityManager.createQuery(queryStr, Boolean.class);
        query.setParameter("postId", likeDTO.getPostId());
        query.setParameter("userId", likeDTO.getUserId());
        return query.getSingleResult();
    }

    @Override
    public List<UserPreviewDTO> getPostLikes(Long postId) {
        List<User> users = postRepository.findUsersWhoLikedPost(postId);
        return users.stream()
                .map(user -> new UserPreviewDTO(user.getId(), user.getUsername(), user.getAvatar()))
                .collect(Collectors.toList());
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
    public List<FullPostDTO> searchPosts(String title, String tag, String sortBy) {
        List<Post> posts;
        if (sortBy != null && sortBy.equals("likes")) {
            List<Object[]> postLikeCounts = postRepository.findPostsWithLikeCount(title, tag);
            posts = postLikeCounts.stream().map(object -> (Post) object[0]).collect(Collectors.toList());
            System.out.println("Sorting by likes");
        } else {
            posts = getPosts(title, tag, Sort.by(Sort.Direction.DESC, "postDate"));
            System.out.println("Sorting by date");
        }
        return posts.stream().map(this::convertToFullPostDTO).collect(Collectors.toList());
    }


    private List<Post> getPosts(String title, String tag, Sort sort) {
        List<Post> posts;
        if (title != null && tag != null) {
            posts = postRepository.findByTitleContainingIgnoreCaseAndTagsNameContainingIgnoreCase(title, tag, sort);
            System.out.println("Searching by title and tag: " + title + ", " + tag);
        } else if (title != null) {
            posts = postRepository.findByTitleContainingIgnoreCase(title, sort);
            System.out.println("Searching by title: " + title);
        } else if (tag != null) {
            posts = postRepository.findByTagsNameContainingIgnoreCase(tag, sort);
            System.out.println("Searching by tag: " + tag);
        } else {
            posts = List.of();
        }
        return posts;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();

            if (!post.getUser().getId().equals(userId)) {
                logger.error("User with id {} is not the author of the post with id {}", userId, postId);
                throw new IllegalArgumentException("User is not the author of this post");
            }

            List<PostImage> postImages = postImageRepository.findByPostId(postId);
            for (PostImage postImage : postImages) {
                fileStorageService.deleteFileByUrl(postImage.getImageUrl());
                postImageRepository.delete(postImage);
            }

            List<Comment> comments = commentRepository.findByPostId(postId);
            commentRepository.deleteAll(comments);

            post.getLikes().clear();
            postRepository.save(post);

            post.getTags().clear();
            postRepository.save(post);

            postRepository.delete(post);

            logger.info("Post with id {} deleted successfully", postId);
        } else {
            logger.error("Post not found with id {}", postId);
            throw new IllegalArgumentException("Post not found with id " + postId);
        }
    }
}