package com.example.chatstorage.service.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableValidator {

    private PageableValidator() {
    }

    public static Pageable validate(Pageable pageable, int maxSize, Sort defaultSort) {
        int size = pageable.getPageSize();
        if (size < 1 || size > maxSize) {
            throw new IllegalArgumentException("size must be between 1 and " + maxSize);
        }

        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
