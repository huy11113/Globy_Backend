package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Booking;
import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Entity.Payment;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.BookingRepository;
import com.example.SpringMongoProject.Repo.DestinationRepository;
import com.example.SpringMongoProject.Repo.PaymentRepository;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map; // THÊM IMPORT NÀY
import java.util.function.Function; // THÊM IMPORT NÀY
import java.util.stream.Collectors; // THÊM IMPORT NÀY

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
    @Autowired
    private DestinationRepository destinationRepository;

    public List<Booking> findAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        populateToursForBookings(bookings);
        return bookings;
    }

    public List<Booking> findBookingsByUserId(String userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        populateToursForBookings(bookings);
        return bookings;
    }

    public Booking approveBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
        booking.setStatus("approved");
        return bookingRepository.save(booking);
    }

    public Booking rejectBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
        booking.setStatus("rejected");
        return bookingRepository.save(booking);
    }

    public Booking createBooking(Booking bookingData) {
        User user = userRepository.findById(bookingData.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Tour tour = tourRepository.findById(bookingData.getTour().getId())
                .orElseThrow(() -> new RuntimeException("Tour not found"));
        bookingData.setUser(user);
        bookingData.setTour(tour);
        bookingData.setStatus("pending_approval");
        return bookingRepository.save(bookingData);
    }

    public boolean processPayment(String bookingId, double amount, String method) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"approved".equals(booking.getStatus())) {
            throw new IllegalStateException("Booking này chưa được duyệt hoặc đã được xử lý.");
        }
        boolean isPaymentSuccessful = true;
        if (isPaymentSuccessful) {
            booking.setStatus("confirmed");
            bookingRepository.save(booking);
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

    private void populateToursForBookings(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) return;

        List<Tour> tours = bookings.stream().map(Booking::getTour).collect(Collectors.toList());
        List<String> destIds = tours.stream()
                .map(Tour::getDestinationId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (destIds.isEmpty()) return;

        List<Destination> destinations = destinationRepository.findAllById(destIds);

        // SỬA LỖI TẠI ĐÂY: Sử dụng Function.identity() và thêm merge function để tránh lỗi
        Map<String, Destination> destMap = destinations.stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity(), (existing, replacement) -> existing));

        tours.forEach(tour -> {
            if (tour.getDestinationId() != null) {
                tour.setDestination(destMap.get(tour.getDestinationId()));
            }
        });
    }
    // Thêm phương thức này vào trong class BookingService

    public Booking findBookingById(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
        // Làm đầy dữ liệu tour và destination
        populateToursForBookings(List.of(booking));
        return booking;
    }
}