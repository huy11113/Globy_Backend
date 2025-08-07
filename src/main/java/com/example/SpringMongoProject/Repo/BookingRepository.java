package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByUserId(String userId);
    Optional<Booking> findByPaymentOrderCode(Long paymentOrderCode);

    // --- THÊM PHƯƠNG THỨC NÀY ---
    Optional<Booking> findFirstByStatusOrderByCreatedAtDesc(String status);
}