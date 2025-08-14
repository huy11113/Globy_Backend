// File: src/main/java/com/example/SpringMongoProject/Service/TourVectorCache.java
package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Repo.TourRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class TourVectorCache {

    // Lớp nội bộ để lưu trữ thông tin tinh gọn trong RAM (ID và vector)
    private static class TourVector {
        String id;
        List<Double> vector;

        TourVector(String id, List<Double> vector) {
            this.id = id;
            this.vector = vector;
        }
    }

    @Autowired
    private TourRepository tourRepository;

    // "Bộ não" chính, là một danh sách các vector tour được lưu trong RAM
    private List<TourVector> tourVectorCache = List.of();

    /**
     * Phương thức này sẽ tự động chạy một lần duy nhất khi ứng dụng khởi động.
     * Nhiệm vụ của nó là tải dữ liệu vector từ MongoDB vào bộ nhớ cache.
     */
    @PostConstruct
    public void init() {
        System.out.println("Bắt đầu tải dữ liệu vector vào bộ nhớ cache...");
        List<Tour> allTours = tourRepository.findAll();
        this.tourVectorCache = allTours.stream()
                // Chỉ lấy những tour nào đã có vector embedding
                .filter(tour -> tour.getTourEmbedding() != null && !tour.getTourEmbedding().isEmpty())
                // Chuyển đổi từ Tour đầy đủ sang đối tượng TourVector gọn nhẹ
                .map(tour -> new TourVector(tour.getId(), tour.getTourEmbedding()))
                .collect(Collectors.toList());
        System.out.println("✅ Đã tải thành công " + this.tourVectorCache.size() + " vector vào bộ nhớ.");
    }

    /**
     * Tìm kiếm N tour tương đồng nhất trong bộ nhớ cache.
     * @param queryVector Vector của câu hỏi người dùng.
     * @param topN Số lượng kết quả cần trả về.
     * @return Danh sách ID của các tour tương đồng nhất.
     */
    public List<String> findSimilarTourIds(List<Double> queryVector, int topN) {
        if (queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }

        // Dùng stream để xử lý hiệu năng cao
        return tourVectorCache.stream()
                // 1. Tính toán độ tương đồng của câu hỏi với TẤT CẢ các tour trong cache
                .map(tourVector -> {
                    double similarity = cosineSimilarity(queryVector, tourVector.vector);
                    return new SimpleEntry<>(tourVector.id, similarity);
                })
                // 2. Sắp xếp kết quả, đưa những tour có độ tương đồng cao nhất lên đầu
                .sorted(Comparator.comparing(SimpleEntry<String, Double>::getValue).reversed())
                // 3. Chỉ lấy số lượng kết quả theo yêu cầu (ví dụ: 20)
                .limit(topN)
                // 4. Chỉ lấy ra ID của tour
                .map(SimpleEntry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Hàm toán học để tính toán độ tương đồng cosine giữa hai vector.
     * @return Một số từ -1 đến 1 (càng gần 1 càng tương đồng).
     */
    private double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
        if (vecA.size() != vecB.size() || vecA.isEmpty()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.size(); i++) {
            dotProduct += vecA.get(i) * vecB.get(i);
            normA += Math.pow(vecA.get(i), 2);
            normB += Math.pow(vecB.get(i), 2);
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}