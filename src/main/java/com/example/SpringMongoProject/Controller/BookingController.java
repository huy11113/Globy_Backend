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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * API để người dùng gửi yêu cầu đặt tour.
     * Trạng thái ban đầu của booking sẽ là 'pending_approval'.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody CreateBookingRequest request) {
        try {
            Booking newBookingData = new Booking();

            // Tạo các đối tượng User và Tour tạm thời chỉ chứa ID để liên kết
            User user = new User();
            user.setId(request.getUserId());
            Tour tour = new Tour();
            tour.setId(request.getTourId());

            newBookingData.setUser(user);
            newBookingData.setTour(tour);
            newBookingData.setStartDate(request.getStartDate());
            newBookingData.setPeople(request.getPeople());
            newBookingData.setTotalPrice(request.getTotalPrice());

            // ✅ ĐÃ CẬP NHẬT: Nhận và lưu ghi chú từ request
            newBookingData.setNotes(request.getNotes());

            Booking createdBooking = bookingService.createBooking(newBookingData);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Yêu cầu đặt tour của bạn đã được gửi thành công!",
                    "data", createdBooking
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Không thể tạo yêu cầu đặt tour.",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * API để người dùng lấy danh sách tất cả các chuyến đi đã đặt của họ.
     * Sẽ được dùng cho trang "Chuyến đi của tôi".
     */
    @GetMapping("/my-trips/{userId}")
    public ResponseEntity<Map<String, Object>> getMyTrips(@PathVariable String userId) {
        try {
            List<Booking> bookings = bookingService.findBookingsByUserId(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", bookings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Không thể lấy danh sách chuyến đi."
            ));
        }
    }

    /**
     * API để xử lý thanh toán cho một booking đã được admin duyệt ('approved').
     * Sau khi thành công, trạng thái sẽ chuyển thành 'confirmed'.
     */
    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> handlePayment(@RequestBody PaymentRequest request) {
        try {
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
        } catch (IllegalStateException e) {
            // Bắt lỗi khi thanh toán cho booking chưa được duyệt
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Đã xảy ra lỗi trong quá trình thanh toán."));
        }
    }
    // Thêm endpoint này vào trong class BookingController

    /**
     * API để lấy thông tin chi tiết của một booking.
     * Dùng cho trang thanh toán.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> getBookingDetails(@PathVariable String bookingId) {
        try {
            Booking booking = bookingService.findBookingById(bookingId);
            return ResponseEntity.ok(Map.of("success", true, "data", booking));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}