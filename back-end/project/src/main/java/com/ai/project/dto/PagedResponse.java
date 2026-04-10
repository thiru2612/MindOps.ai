package com.ai.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic paginated response wrapper used by all list endpoints.
 *
 * @param <T> the type of items in the content list
 */
@Getter
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int     currentPage;
    private int     totalPages;
    private long    totalElements;
    private int     pageSize;
    private boolean isLast;
}