package com.example.itodo.sync.dto;

import java.util.UUID;

public record TodoTagLinkResponse(
        UUID todoId,
        UUID tagId
) {
}
