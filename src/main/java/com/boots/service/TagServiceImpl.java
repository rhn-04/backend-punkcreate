package com.boots.service;

import com.boots.DTO.TagDTO;
import com.boots.entity.Tag;
import com.boots.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Autowired
    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public void createOrFindTags(List<TagDTO> tagDTOList) {
        List<Tag> tags = new ArrayList<>();
        for (TagDTO tagDTO : tagDTOList) {
            String tagName = tagDTO.getName().toLowerCase();

            Tag existingTag = tagRepository.findByName(tagName);
            if (existingTag == null) {
                Tag newTag = new Tag();
                newTag.setName(tagName);
                tags.add(newTag);
            } else {
                tags.add(existingTag);
            }
        }
        tagRepository.saveAll(tags);
    }

}

