package com.carddemo.domain.repository;

import com.carddemo.domain.entity.DisclosureGroup;
import com.carddemo.domain.entity.DisclosureGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, DisclosureGroupId> {
}
