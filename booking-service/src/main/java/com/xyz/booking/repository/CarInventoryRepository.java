package com.xyz.booking.repository;

import com.xyz.booking.entity.CarInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CarInventoryRepository extends JpaRepository<CarInventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CarInventory c WHERE c.carSegment = :segment")
    @Transactional
    CarInventory lockInventoryRow(@Param("segment") String segment);
}
