package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Booking;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Entity.User;
import com.example.SpringMongoProject.Repo.BookingRepository;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.Repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TourRepository tourRepository;

    public Map<String, Object> getDashboardData() {
        List<Booking> allBookings = bookingRepository.findAll();
        List<User> allUsers = userRepository.findAll();
        List<Tour> allTours = tourRepository.findAll();

        Map<String, Object> data = new HashMap<>();

        // 1. Thống kê tổng quan
        long totalRevenue = allBookings.stream()
                .filter(b -> "confirmed".equals(b.getStatus()) && b.getTotalPrice() != null)
                .mapToLong(Booking::getTotalPrice)
                .sum();

        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        long newBookingsThisMonth = allBookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().isAfter(oneMonthAgo))
                .count();

        long newUsersThisMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().isAfter(oneMonthAgo))
                .count();

        data.put("totalRevenue", totalRevenue);
        data.put("totalBookings", allBookings.size());
        data.put("totalUsers", allUsers.size());
        data.put("totalTours", allTours.size());
        data.put("newBookingsThisMonth", newBookingsThisMonth);
        data.put("newUsersThisMonth", newUsersThisMonth);

        // 2. Dữ liệu biểu đồ doanh thu 6 tháng
        Map<String, Long> revenueByMonth = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        for (int i = 0; i < 6; i++) {
            LocalDate date = LocalDate.now().minusMonths(i);
            revenueByMonth.put("T" + date.format(formatter), 0L);
        }

        allBookings.stream()
                .filter(b -> "confirmed".equals(b.getStatus()) && b.getCreatedAt() != null)
                .forEach(b -> {
                    String monthKey = "T" + b.getCreatedAt().toLocalDate().format(formatter);
                    if (revenueByMonth.containsKey(monthKey)) {
                        revenueByMonth.merge(monthKey, b.getTotalPrice(), Long::sum);
                    }
                });
        data.put("revenueByMonth", revenueByMonth);

        // 3. Dữ liệu biểu đồ trạng thái booking
        Map<String, Long> bookingStatusCounts = allBookings.stream()
                .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));
        data.put("bookingStatusCounts", bookingStatusCounts);

        return data;
    }
}