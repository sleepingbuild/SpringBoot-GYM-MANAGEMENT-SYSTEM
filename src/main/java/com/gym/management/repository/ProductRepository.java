package com.gym.management.repository;

import com.gym.management.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * Khoá bi quan 1 dòng sản phẩm khi tạo đơn POS — tránh bán vượt tồn kho khi 2 đơn
     * đến gần như đồng thời (xem PosOrderServiceImpl#createOrder). Dùng cùng 1
     * @Transactional với thao tác save đơn để khoá được giữ tới hết transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") UUID id);
}
