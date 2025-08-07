package com.example.SpringMongoProject.Repo;

import com.example.SpringMongoProject.Entity.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByUserId(String userId);

    // ✅ Đảm bảo phương thức này tồn tại
    Optional<Booking> findByPaymentOrderCode(Long paymentOrderCode);

    // Phương thức cũ, không cần thiết cho logic mới nữa
    Optional<Booking> findFirstByStatusOrderByCreatedAtDesc(String status);
}