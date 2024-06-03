package com.boots.service;

import com.boots.DTO.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    UserDetails loadUserByUsername(String username);

    void logoutUser(String userToken);

    void registerUser(RegisterDTO registerDTO);

    ResponseEntity<?> createAuthToken(JwtRequest authRequest);
}
