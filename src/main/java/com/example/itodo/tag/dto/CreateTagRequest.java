package com.example.itodo.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 32) String color
) {
}
