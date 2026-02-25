package com.hotelio.booking_service.dto;

import java.time.Instant;

public class BookingEvent {
    private String bookingId;
    private String userId;
    private String hotelId;
    private String promoCode;
    private Double price;
    private Instant createdAt;

    public BookingEvent() {
    }

    public BookingEvent(String bookingId, String userId, String hotelId, String promoCode, Double price,
            Instant createdAt) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.hotelId = hotelId;
        this.promoCode = promoCode;
        this.price = price;
        this.createdAt = createdAt;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
