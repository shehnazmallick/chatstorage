package com.example.chatstorage.service.support;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public abstract class AbstractPageableService {

    protected Pageable validatePageable(Pageable pageable, int maxSize, Sort defaultSort) {
        return PageableValidator.validate(pageable, maxSize, defaultSort);
    }
}
