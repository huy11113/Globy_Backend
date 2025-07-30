package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Repo.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TourService {

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Tìm kiếm, lọc, và phân trang cho Tour.
     * Đáp ứng tất cả yêu cầu từ hook useTours.js và useDestinationDetail.js.
     */
    public Page<Tour> findTours(String searchTerm, String sortBy, int page, int limit, Double maxPrice, Boolean featured, String destinationId) {
        // Mặc định phân trang, sắp xếp theo ngày tạo mới nhất
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Query query = new Query().with(pageable);

        // Thêm điều kiện tìm kiếm (search)
        if (searchTerm != null && !searchTerm.isEmpty()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(searchTerm, "i"), // 'i' for case-insensitive
                    Criteria.where("city").regex(searchTerm, "i")
            );
            query.addCriteria(searchCriteria);
        }

        // Thêm điều kiện lọc theo giá
        if (maxPrice != null) {
            query.addCriteria(Criteria.where("price").lte(maxPrice));
        }

        // Thêm điều kiện lọc tour nổi bật
        if (featured != null) {
            query.addCriteria(Criteria.where("featured").is(featured));
        }

        // Thêm điều kiện lọc theo destination ID
        if (destinationId != null && !destinationId.isEmpty()) {
            query.addCriteria(Criteria.where("destination._id").is(destinationId));
        }

        // Thực thi câu lệnh
        List<Tour> tours = mongoTemplate.find(query, Tour.class);

        // Trả về đối tượng Page chứa dữ liệu và thông tin phân trang
        return PageableExecutionUtils.getPage(
                tours,
                pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Tour.class)
        );
    }

    /**
     * Tìm chi tiết một tour theo ID.
     */
    public Tour findTourById(String id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tour với ID: " + id));
    }
}