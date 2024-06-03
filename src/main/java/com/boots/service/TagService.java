package com.boots.service;

import com.boots.DTO.TagDTO;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface TagService {
    void createOrFindTags(List<TagDTO> tagDTOList);
}
