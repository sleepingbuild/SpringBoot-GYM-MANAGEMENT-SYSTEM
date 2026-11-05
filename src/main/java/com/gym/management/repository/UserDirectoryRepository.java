package com.gym.management.repository;

import com.gym.management.dto.response.TrainerResponse;
import com.gym.management.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Agent 1 - SHARED. Truy vấn danh mục chỉ đọc trên bảng users (tìm hội viên, danh sách PT).
 * Tách khỏi UserRepository để không đụng vào repository đang được nhiều module dùng.
 *
 * Tìm kiếm dùng ESCAPE '!': DirectoryServiceImpl thoát các ký tự đặc biệt của LIKE (% _ [ !) trước khi truyền vào.
 */
public interface UserDirectoryRepository extends Repository<User, UUID> {

    @Query(value = """
            select u from User u
            where u.status = 'ACTIVE'
              and exists (select 1 from u.roles r where r.name = :role)
              and (:q = ''
                   or lower(u.fullName) like lower(concat('%', :q, '%')) escape '!'
                   or lower(u.email) like lower(concat('%', :q, '%')) escape '!'
                   or u.phone like concat('%', :q, '%') escape '!')
            """,
            countQuery = """
            select count(u) from User u
            where u.status = 'ACTIVE'
              and exists (select 1 from u.roles r where r.name = :role)
              and (:q = ''
                   or lower(u.fullName) like lower(concat('%', :q, '%')) escape '!'
                   or lower(u.email) like lower(concat('%', :q, '%')) escape '!'
                   or u.phone like concat('%', :q, '%') escape '!')
            """)
    Page<User> searchActiveByRole(@Param("role") String role, @Param("q") String q, Pageable pageable);

    @Query("""
            select new com.gym.management.dto.response.TrainerResponse(u.id, u.fullName, p.avatarUrl)
            from User u left join UserProfile p on p.user = u
            where u.status = 'ACTIVE'
              and exists (select 1 from u.roles r where r.name = :role)
            order by u.fullName asc
            """)
    List<TrainerResponse> findActiveByRole(@Param("role") String role);
}
