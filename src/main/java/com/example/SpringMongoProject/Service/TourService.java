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

    public Page<Tour> findTours(String searchTerm, String sortBy, int page, int limit, Double maxPrice, Boolean featured, String destinationId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // Mặc định
        if (sortBy != null && !sortBy.isEmpty()) {
            if (sortBy.startsWith("-")) {
                sort = Sort.by(Sort.Direction.DESC, sortBy.substring(1));
            } else {
                sort = Sort.by(Sort.Direction.ASC, sortBy);
            }
        }
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        Query query = new Query().with(pageable);
        // Thêm điều kiện tìm kiếm (search)
        if (searchTerm != null && !searchTerm.isEmpty()) {
            // Tìm kiếm trong cả title VÀ city
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(searchTerm, "i"),
                    Criteria.where("city").regex(searchTerm, "i") // <-- DÒNG NÀY ĐÃ ĐƯỢC THÊM VÀO
            );
            query.addCriteria(searchCriteria);
        }

        if (searchTerm != null && !searchTerm.isEmpty()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(searchTerm, "i"),
                    Criteria.where("city").regex(searchTerm, "i")
            ));
        }
        if (maxPrice != null) {
            query.addCriteria(Criteria.where("price").lte(maxPrice));
        }
        if (featured != null) {
            query.addCriteria(Criteria.where("featured").is(featured));
        }
        if (destinationId != null && !destinationId.isEmpty()) {
            query.addCriteria(Criteria.where("destination._id").is(destinationId));
        }

        List<Tour> tours = mongoTemplate.find(query, Tour.class);

        return PageableExecutionUtils.getPage(
                tours,
                pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Tour.class)
        );
    }

    public Tour findTourById(String id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tour với ID: " + id));
    }
}