package com.boots.controller;

import com.boots.DTO.TagDTO;
import com.boots.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping("/create-or-find")
    public ResponseEntity<?> createOrFindTags(@RequestBody List<TagDTO> tagDTOList) {
        tagService.createOrFindTags(tagDTOList);
        return ResponseEntity.ok().build();
    }
}
