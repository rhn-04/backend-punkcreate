package com.boots.controller;

import com.boots.DTO.JwtRequest;
import com.boots.DTO.JwtResponse;
import com.boots.DTO.RegisterDTO;
import com.boots.entity.User;
import com.boots.exceptions.AppError;
import com.boots.repository.UserRepository;
import com.boots.service.AuthService;
import com.boots.service.FileStorageService;
import com.boots.service.UserProfileServiceImpl;
import com.boots.utils.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

import static com.boots.controller.PostController.logger;

@RestController
@CrossOrigin
public class AuthController {

    private final UserRepository userRepository;
    private final UserProfileServiceImpl userProfileService;
    private final JwtTokenUtils jwtTokenUtils;
    private final AuthService authService;
    private final FileStorageService fileStorageService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(UserRepository userRepository, UserProfileServiceImpl userProfileService, JwtTokenUtils jwtTokenUtils, AuthService authService, FileStorageService fileStorageService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userProfileService = userProfileService;
        this.jwtTokenUtils = jwtTokenUtils;
        this.authService = authService;
        this.fileStorageService = fileStorageService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            String username = jwtTokenUtils.getUsername(token.substring(7));
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "Invalid token"), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<?> createAuthToken(@RequestBody JwtRequest authRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(), "Неправильный логин или пароль"), HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + authRequest.getUsername()));
        String token = jwtTokenUtils.generateToken(user);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@ModelAttribute @Validated RegisterDTO registerDTO,
                                          @RequestParam(name = "avatar", required = false) MultipartFile avatarFile,
                                          BindingResult bindingResult) {
        try {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                registerDTO.setAvatarFileName(avatarFile.getOriginalFilename());
                registerDTO.setAvatarFile(avatarFile);
            }

            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body("Validation error: " + bindingResult.getAllErrors());
            }

            authService.registerUser(registerDTO);

            User user = userRepository.findByUsername(registerDTO.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + registerDTO.getUsername()));

            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName()))
            );

            String token = jwtTokenUtils.generateToken(user);

            return ResponseEntity.ok(new JwtResponse(token));
        } catch (Exception e) {
            logger.error("Error during user registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during user registration: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        String userToken = token.substring(7);
        authService.logoutUser(userToken);
        return ResponseEntity.ok().build();
    }
}
