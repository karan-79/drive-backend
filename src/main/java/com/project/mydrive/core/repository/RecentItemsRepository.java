package com.project.mydrive.core.repository;

import com.project.mydrive.core.domain.RecentItems;
import org.springframework.data.repository.CrudRepository;

public interface RecentItemsRepository extends CrudRepository<RecentItems, Long> {
}
