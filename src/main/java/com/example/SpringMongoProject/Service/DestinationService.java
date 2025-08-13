package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Repo.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DestinationService {

    @Autowired
    private DestinationRepository destinationRepository;

    /**
     * Lấy danh sách địa điểm, có thể giới hạn số lượng.
     * @param limit Số lượng địa điểm tối đa cần lấy.
     * @return Danh sách các địa điểm.
     */
    public List<Destination> findDestinations(Integer limit) {
        List<Destination> allDestinations = destinationRepository.findAll();

        // Nếu có limit và limit > 0, giới hạn số lượng kết quả
        if (limit != null && limit > 0) {
            return allDestinations.stream().limit(limit).collect(Collectors.toList());
        }

        return allDestinations;
    }

    /**
     * Tìm một địa điểm cụ thể theo ID.
     * @param id ID của địa điểm.
     * @return Đối tượng Destination.
     * @throws RuntimeException nếu không tìm thấy.
     */
    public Destination findDestinationById(String id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa điểm với ID: " + id));
    }

    /**
     * ✅ HÀM MỚI: Tạo một địa điểm mới.
     * (Đây là hàm còn thiếu gây ra lỗi "cannot find symbol")
     */
    public Destination createDestination(Destination destinationData) {
        return destinationRepository.save(destinationData);
    }

    /**
     * ✅ HÀM MỚI: Cập nhật thông tin một địa điểm.
     */
    public Destination updateDestination(String id, Destination destinationDetails) {
        Destination destination = findDestinationById(id); // Tái sử dụng hàm có sẵn để kiểm tra tồn tại
        destination.setName(destinationDetails.getName());
        destination.setDescription(destinationDetails.getDescription());
        destination.setImage(destinationDetails.getImage());
        destination.setContinent(destinationDetails.getContinent());
        return destinationRepository.save(destination);
    }

    /**
     * ✅ HÀM MỚI: Xóa một địa điểm.
     */
    public void deleteDestination(String id) {
        if (!destinationRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy địa điểm với ID: " + id);
        }
        destinationRepository.deleteById(id);
    }
}