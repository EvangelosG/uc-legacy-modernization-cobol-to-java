package com.carddemo.domain.repository;

import com.carddemo.domain.entity.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, String> {
}
