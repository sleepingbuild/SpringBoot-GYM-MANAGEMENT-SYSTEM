package com.gym.management.repository;

import com.gym.management.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent 1. Truy vấn quản trị tài khoản (Super Admin). Tách khỏi UserRepository để không đụng vào
 * repository đang được nhiều module dùng. Tìm kiếm dùng ESCAPE '!' như UserDirectoryRepository.
 */
public interface UserAdminRepository extends Repository<User, UUID> {

    @Query(value = """
            select u from User u
            where exists (select 1 from u.roles r where r.name in :roleNames)
              and (:status = '' or u.status = :status)
              and (:q = ''
                   or lower(u.fullName) like lower(concat('%', :q, '%')) escape '!'
                   or lower(u.email) like lower(concat('%', :q, '%')) escape '!'
                   or u.phone like concat('%', :q, '%') escape '!')
            """,
            countQuery = """
            select count(u) from User u
            where exists (select 1 from u.roles r where r.name in :roleNames)
              and (:status = '' or u.status = :status)
              and (:q = ''
                   or lower(u.fullName) like lower(concat('%', :q, '%')) escape '!'
                   or lower(u.email) like lower(concat('%', :q, '%')) escape '!'
                   or u.phone like concat('%', :q, '%') escape '!')
            """)
    Page<User> search(@Param("roleNames") Collection<String> roleNames,
                      @Param("status") String status,
                      @Param("q") String q,
                      Pageable pageable);

    Optional<User> findById(UUID id);

    User save(User user);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);
}
