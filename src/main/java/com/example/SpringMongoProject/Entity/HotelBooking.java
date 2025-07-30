package com.example.SpringMongoProject.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "hotel_bookings")
public class HotelBooking {
    @Id
    private String id;
    @DBRef
    private User user;
    @DBRef
    private Hotel hotel;
    @DBRef
    private HotelRoom room;
    private Date checkIn;
    private Date checkOut;
    private Integer guests;
    private Double totalPrice;
    private String status = "pending";
}