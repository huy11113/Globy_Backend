package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Booking;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Service.BookingService;
import com.example.SpringMongoProject.dto.CreateBookingRequest;
import com.example.SpringMongoProject.dto.PaymentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // API tạo booking mới
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody CreateBookingRequest request) {
        try {
            Booking newBookingData = new Booking();

            // Tạo các đối tượng User và Tour tạm thời chỉ chứa ID
            User user = new User();
            user.setId(request.getUserId());
            Tour tour = new Tour();
            tour.setId(request.getTourId());

            newBookingData.setUser(user);
            newBookingData.setTour(tour);
            newBookingData.setStartDate(request.getStartDate());
            newBookingData.setPeople(request.getPeople());
            newBookingData.setTotalPrice(request.getTotalPrice());

            Booking createdBooking = bookingService.createBooking(newBookingData);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Booking đã được tạo, vui lòng tiến hành thanh toán.",
                    "data", createdBooking
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Không thể tạo booking.",
                    "error", e.getMessage()
            ));
        }
    }

    // API xử lý thanh toán
    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> handlePayment(@RequestBody PaymentRequest request) {
        boolean success = bookingService.processPayment(request.getBookingId(), request.getAmount(), request.getMethod());
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thanh toán thành công! Chuyến đi của bạn đã được xác nhận."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Thanh toán thất bại, vui lòng thử lại."
            ));
        }
    }
}