package com.example.itodo.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long total,
        long page,
        long size
) {
}
