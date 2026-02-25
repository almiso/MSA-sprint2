package com.hotelio.booking_history_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotelio.booking_history_service.dto.BookingEvent;
import com.hotelio.booking_history_service.entity.BookingHistory;
import com.hotelio.booking_history_service.repository.BookingHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class BookingHistoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingHistoryConsumer.class);
    private final BookingHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public BookingHistoryConsumer(BookingHistoryRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "booking_created", groupId = "history-group")
    public void consume(String message) {
        log.info("Received Kafka message: {}", message);
        try {
            BookingEvent event = objectMapper.readValue(message, BookingEvent.class);
            BookingHistory history = new BookingHistory();
            history.setOriginalBookingId(event.getBookingId());
            history.setUserId(event.getUserId());
            history.setHotelId(event.getHotelId());
            history.setPromoCode(event.getPromoCode());
            history.setPrice(event.getPrice());
            history.setCreatedAt(event.getCreatedAt());

            repository.save(history);
            log.info("Saved booking history for bookingId={}", event.getBookingId());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message: {}", message, e);
        }
    }
}
