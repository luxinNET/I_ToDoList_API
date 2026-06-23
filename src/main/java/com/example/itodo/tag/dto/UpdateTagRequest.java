package com.example.itodo.tag.dto;

import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
        @Size(max = 64) String name,
        @Size(max = 32) String color
) {
}
