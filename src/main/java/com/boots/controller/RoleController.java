package com.boots.controller;

import com.boots.repository.RoleRepository;
import com.boots.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public List<String> getAllRoles() {
        return roleRepository.findAll().stream().map(Role::getName).collect(Collectors.toList());
    }
}