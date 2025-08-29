package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Booking;
import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Entity.Notification;
import com.example.SpringMongoProject.Entity.Payment;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.BookingRepository;
import com.example.SpringMongoProject.Repo.DestinationRepository;
import com.example.SpringMongoProject.Repo.NotificationRepository;
import com.example.SpringMongoProject.Repo.PaymentRepository;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    @Autowired
    private NotificationRepository notificationRepository;

    public List<Booking> findAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        bookings.sort(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        populateToursForBookings(bookings);
        return bookings;
    }

    public List<Booking> findBookingsByUserId(String userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        bookings.sort(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        populateToursForBookings(bookings);
        return bookings;
    }

    public Booking approveBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
        booking.setStatus("approved");
        Booking savedBooking = bookingRepository.save(booking);

        Notification notification = new Notification();
        notification.setMessage("Yêu cầu đặt tour '" + savedBooking.getTour().getTitle() + "' của bạn đã được duyệt. Hãy tiến hành thanh toán.");
        notification.setBookingId(savedBooking.getId());
        notification.setRecipientId(savedBooking.getUser().getId());
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        return savedBooking;
    }

    public Booking rejectBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
        booking.setStatus("rejected");
        return bookingRepository.save(booking);
    }

    public Booking createBooking(Booking bookingData) {
        User user = userRepository.findById(bookingData.getUser().getId()).orElseThrow(() -> new RuntimeException("User not found"));
        Tour tour = tourRepository.findById(bookingData.getTour().getId()).orElseThrow(() -> new RuntimeException("Tour not found"));
        bookingData.setUser(user);
        bookingData.setTour(tour);
        bookingData.setStatus("pending_approval");
        bookingData.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(bookingData);

        Notification notification = new Notification();
        notification.setMessage(user.getName() + " vừa gửi yêu cầu đặt tour " + tour.getTitle());
        notification.setBookingId(savedBooking.getId());
        notification.setRecipientId("admin");
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        return savedBooking;
    }

    // ✅ SỬA LỖI: Thay đổi kiểu dữ liệu của `amount` từ `double` thành `Long` để khớp với Controller
    public boolean processPayment(String bookingId, Long amount, String method) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!"approved".equals(booking.getStatus())) {
            throw new IllegalStateException("Booking này chưa được duyệt hoặc đã được xử lý.");
        }
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
    }

    public void setPaymentOrderCode(String bookingId, long orderCode) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
        booking.setPaymentOrderCode(orderCode);
        bookingRepository.save(booking);
    }
    // ✅ ĐÂY LÀ PHƯƠNG THỨC CÒN THIẾU
    public Booking prepareBookingForPayment(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        long orderCode = new Date().getTime();
        booking.setPaymentOrderCode(orderCode);

        return bookingRepository.save(booking);
    }

    public Booking confirmBookingByOrderCode(long orderCode) {
        Booking booking = bookingRepository.findByPaymentOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Webhook received, but no booking was found with orderCode: " + orderCode + ". This might be a test webhook from PayOS dashboard."));

        if (!"approved".equals(booking.getStatus())) {
            System.err.println("Attempted to confirm a booking that is not in 'approved' state. OrderCode: " + orderCode + ", Current Status: " + booking.getStatus());
            return booking;
        }

        booking.setStatus("confirmed");
        Booking savedBooking = bookingRepository.save(booking);

        Notification notification = new Notification();
        notification.setMessage("Thanh toán cho tour '" + savedBooking.getTour().getTitle() + "' đã thành công!");
        notification.setBookingId(savedBooking.getId());
        notification.setRecipientId(savedBooking.getUser().getId());
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        return savedBooking;
    }

    public Booking findBookingById(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));
        populateToursForBookings(List.of(booking));
        return booking;
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

        Map<String, Destination> destMap = destinations.stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity(), (existing, replacement) -> existing));

        tours.forEach(tour -> {
            if (tour.getDestinationId() != null) {
                tour.setDestination(destMap.get(tour.getDestinationId()));
            }
        });
    }
}