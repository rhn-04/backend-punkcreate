package com.boots.service;

import com.boots.DTO.JwtRequest;
import com.boots.DTO.JwtResponse;
import com.boots.DTO.RegisterDTO;
import com.boots.entity.User;
import com.boots.exceptions.AppError;
import com.boots.repository.RoleRepository;
import com.boots.repository.UserRepository;
import com.boots.utils.JwtTokenUtils;
import com.boots.utils.TokenBlacklist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserProfileServiceImpl userProfileService;
    private final FileStorageService fileStorageService;
    private final JwtTokenUtils jwtTokenUtils;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, UserProfileServiceImpl userProfileService, FileStorageService fileStorageService, JwtTokenUtils jwtTokenUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userProfileService = userProfileService;
        this.fileStorageService = fileStorageService;
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName()))
        );
    }


    @Override
    public void logoutUser(String userToken) {
        SecurityContextHolder.clearContext();
        TokenBlacklist.invalidateToken(userToken);
    }


    @Override
    public void registerUser(RegisterDTO registerDTO) {
        User user = new User();
        String username = registerDTO.getUsername().toLowerCase();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        String password = registerDTO.getPassword();
        String confirmPassword = registerDTO.getConfirmPassword();
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }
        if (password.length() < 8 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain both letters and digits");
        }
        String encodedPassword = passwordEncoder.encode(password);
        user.setUsername(username);
        user.setAvatar(registerDTO.getAvatarFileName());
        user.setDescription(registerDTO.getDescription());
        user.setRole(roleRepository.findByName("AUTHOR").orElseThrow(() -> new RuntimeException("Default role not found")));
        user.setPassword(encodedPassword);
        user.setBirthDate(registerDTO.getBirthDate());
        userRepository.save(user);

        if (registerDTO.getAvatarFile() != null) {
            String storedFileName = fileStorageService.storeFile(registerDTO.getAvatarFile());
            user.setAvatar(storedFileName); 
            userRepository.save(user);
        }
    }

    @Override
    public ResponseEntity<?> createAuthToken(JwtRequest authRequest) {
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


}
