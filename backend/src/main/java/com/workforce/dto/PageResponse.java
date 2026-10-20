package com.workforce.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Stable JSON shape for paginated results. */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements(),
                page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
