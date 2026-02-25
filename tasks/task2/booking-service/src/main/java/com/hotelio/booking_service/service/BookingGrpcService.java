package com.hotelio.booking_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotelio.booking_service.dto.BookingEvent;
import com.hotelio.booking_service.entity.Booking;
import com.hotelio.booking_service.repository.BookingRepository;
import com.hotelio.proto.booking.BookingListRequest;
import com.hotelio.proto.booking.BookingListResponse;
import com.hotelio.proto.booking.BookingRequest;
import com.hotelio.proto.booking.BookingResponse;
import com.hotelio.proto.booking.BookingServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class BookingGrpcService extends BookingServiceGrpc.BookingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(BookingGrpcService.class);
    private final BookingRepository bookingRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BookingGrpcService(BookingRepository bookingRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.bookingRepository = bookingRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void createBooking(BookingRequest request, StreamObserver<BookingResponse> responseObserver) {
        log.info("Creating booking via gRPC: userId={}, hotelId={}, promoCode={}",
                request.getUserId(), request.getHotelId(), request.getPromoCode());

        double basePrice = 100.0;
        double discount = 0.0;
        double finalPrice = basePrice - discount;

        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setHotelId(request.getHotelId());
        booking.setPromoCode(request.getPromoCode());
        booking.setDiscountPercent(discount);
        booking.setPrice(finalPrice);

        booking = bookingRepository.save(booking);

        BookingResponse response = BookingResponse.newBuilder()
                .setId(String.valueOf(booking.getId()))
                .setUserId(booking.getUserId())
                .setHotelId(booking.getHotelId())
                .setPromoCode(booking.getPromoCode() != null ? booking.getPromoCode() : "")
                .setDiscountPercent(booking.getDiscountPercent())
                .setPrice(booking.getPrice())
                .setCreatedAt(booking.getCreatedAt().toString())
                .build();

        try {
            BookingEvent event = new BookingEvent(
                    String.valueOf(booking.getId()), booking.getUserId(),
                    booking.getHotelId(), booking.getPromoCode(),
                    booking.getPrice(), booking.getCreatedAt());
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("booking_created", eventJson);
            log.info("Published booking_created event to Kafka: {}", eventJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize BookingEvent", e);
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listBookings(BookingListRequest request, StreamObserver<BookingListResponse> responseObserver) {
        List<Booking> bookings = request.getUserId() != null && !request.getUserId().isEmpty()
                ? bookingRepository.findByUserId(request.getUserId())
                : bookingRepository.findAll();

        List<BookingResponse> responses = bookings.stream().map(b -> BookingResponse.newBuilder()
                .setId(String.valueOf(b.getId()))
                .setUserId(b.getUserId())
                .setHotelId(b.getHotelId())
                .setPromoCode(b.getPromoCode() != null ? b.getPromoCode() : "")
                .setDiscountPercent(b.getDiscountPercent() != null ? b.getDiscountPercent() : 0.0)
                .setPrice(b.getPrice())
                .setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "")
                .build()).collect(Collectors.toList());

        BookingListResponse listResponse = BookingListResponse.newBuilder()
                .addAllBookings(responses)
                .build();

        responseObserver.onNext(listResponse);
        responseObserver.onCompleted();
    }
}
