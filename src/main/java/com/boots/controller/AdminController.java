package com.boots.controller;

import com.boots.DTO.SubscriberDTO;
import com.boots.entity.User;
import com.boots.entity.Role;
import com.boots.repository.UserRepository;
import com.boots.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/users")
    public List<SubscriberDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(user -> {
            SubscriberDTO dto = new SubscriberDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setAvatar(user.getAvatar());
            dto.setFollowersCount(user.getFollowing().size());
            dto.setRole(user.getRole().getName());
            return dto;
        }).collect(Collectors.toList());
    }

    @PostMapping("/users/{id}/role")
    public void changeUserRole(@PathVariable Long id, @RequestBody String roleName) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);
        userRepository.save(user);
    }
}
