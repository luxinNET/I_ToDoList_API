package com.example.itodo.tag;

import com.example.itodo.tag.dto.TagResponse;
import com.example.itodo.tag.entity.Tag;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagDtoMapper {

    TagResponse toTagResponse(Tag tag);

    List<TagResponse> toTagResponses(List<Tag> tags);
}
