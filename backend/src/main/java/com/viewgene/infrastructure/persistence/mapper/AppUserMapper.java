package com.viewgene.infrastructure.persistence.mapper;

import com.viewgene.domain.model.AppUser;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

public interface AppUserMapper {
    Optional<AppUser> findById(@Param("id") long id);

    Optional<AppUser> findByUsername(@Param("username") String username);
}
