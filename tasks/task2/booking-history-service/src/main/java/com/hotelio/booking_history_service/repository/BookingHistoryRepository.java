package com.hotelio.booking_history_service.repository;

import com.hotelio.booking_history_service.entity.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingHistoryRepository extends JpaRepository<BookingHistory, Long> {
}
