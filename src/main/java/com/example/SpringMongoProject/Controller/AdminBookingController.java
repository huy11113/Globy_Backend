package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Booking;
import com.example.SpringMongoProject.Service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bookings")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}) // Cho phép cả trang user và admin
public class AdminBookingController {

    @Autowired
    private BookingService bookingService;

    // API để lấy tất cả các yêu cầu đặt tour
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBookings() {
        List<Booking> bookings = bookingService.findAllBookings();
        return ResponseEntity.ok(Map.of("success", true, "data", bookings));
    }

    // API để duyệt một yêu cầu
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveBooking(@PathVariable String id) {
        try {
            Booking updatedBooking = bookingService.approveBooking(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã duyệt yêu cầu thành công.",
                    "data", updatedBooking
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // API để từ chối một yêu cầu
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectBooking(@PathVariable String id) {
        try {
            Booking updatedBooking = bookingService.rejectBooking(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã từ chối yêu cầu.",
                    "data", updatedBooking
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}