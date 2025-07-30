package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Booking;
import com.example.SpringMongoProject.Entity.Payment;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.BookingRepository;
import com.example.SpringMongoProject.Repo.PaymentRepository;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TourRepository tourRepository;

    public Booking createBooking(Booking bookingData) {
        // Lấy thông tin user và tour từ ID để đảm bảo dữ liệu hợp lệ
        User user = userRepository.findById(bookingData.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Tour tour = tourRepository.findById(bookingData.getTour().getId())
                .orElseThrow(() -> new RuntimeException("Tour not found"));

        bookingData.setUser(user);
        bookingData.setTour(tour);
        bookingData.setStatus("pending"); // Luôn bắt đầu với trạng thái chờ

        return bookingRepository.save(bookingData);
    }

    public boolean processPayment(String bookingId, double amount, String method) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Mô phỏng quá trình thanh toán
        boolean isPaymentSuccessful = true; // Giả định thanh toán luôn thành công

        if (isPaymentSuccessful) {
            // Cập nhật trạng thái booking
            booking.setStatus("confirmed");
            bookingRepository.save(booking);

            // Tạo bản ghi thanh toán
            Payment payment = new Payment();
            payment.setUserId(booking.getUser().getId());
            payment.setAmount(amount);
            payment.setMethod(method);
            payment.setStatus("paid");
            payment.setPaidAt(new Date());
            payment.setBookingId(booking.getId());
            payment.setBookingModel("Booking");
            paymentRepository.save(payment);

            return true;
        } else {
            booking.setStatus("cancelled");
            bookingRepository.save(booking);
            return false;
        }
    }
}