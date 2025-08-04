package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Repo.DestinationRepository;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TourService {

    @Autowired
    private TourRepository tourRepository;
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    public Page<Tour> findTours(String searchTerm, String sortBy, int page, int limit, Double maxPrice, Boolean featured, String destinationId) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Query query = new Query().with(pageable);

        List<Criteria> criteriaList = new java.util.ArrayList<>();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(searchTerm, "i"),
                    Criteria.where("city").regex(searchTerm, "i")));
        }
        if (maxPrice != null) {
            criteriaList.add(Criteria.where("price").lte(maxPrice));
        }
        if (featured != null) {
            criteriaList.add(Criteria.where("featured").is(featured));
        }
        if (destinationId != null && !destinationId.isEmpty()) {
            criteriaList.add(Criteria.where("destinationId").is(destinationId));
        }
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList));
        }
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = sortBy.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
            String sortField = sortBy.startsWith("-") ? sortBy.substring(1) : sortBy;
            query.with(Sort.by(direction, sortField));
        }

        List<Tour> tours = mongoTemplate.find(query, Tour.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Tour.class);

        // Bước làm đầy dữ liệu thủ công
        populateDestinationsForTours(tours);

        return PageableExecutionUtils.getPage(tours, pageable, () -> total);
    }

    public Tour findTourById(String id) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tour với ID: " + id));
        populateDestinationsForTours(List.of(tour));
        return tour;
    }

    // Hàm tiện ích để làm đầy thông tin destination
    private void populateDestinationsForTours(List<Tour> tours) {
        if (tours == null || tours.isEmpty()) return;

        List<String> destIds = tours.stream()
                .map(Tour::getDestinationId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (destIds.isEmpty()) return;

        List<Destination> destinations = destinationRepository.findAllById(destIds);
        Map<String, Destination> destMap = destinations.stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity(), (existing, replacement) -> existing));

        for (Tour tour : tours) {
            if (tour.getDestinationId() != null) {
                tour.setDestination(destMap.get(tour.getDestinationId()));
            }
        }
    }

}