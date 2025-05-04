package com.project.mydrive.core.repository;

import com.project.mydrive.core.domain.SharedItem;
import org.springframework.data.repository.CrudRepository;

public interface SharedItemsRepository extends CrudRepository<SharedItem, Long> {
}
