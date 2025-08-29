// File: src/main/java/com/example/SpringMongoProject/Service/TourService.java

package com.example.SpringMongoProject.Service;

import com.example.SpringMongoProject.Entity.Destination;
import com.example.SpringMongoProject.Entity.Tour;
import com.example.SpringMongoProject.Repo.DestinationRepository;
import com.example.SpringMongoProject.Repo.TourRepository;
import com.example.SpringMongoProject.dto.ExtractedEntities;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
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

    // PHƯƠNG THỨC NÀY DÀNH CHO TRANG DANH SÁCH TOUR (FILTER THÔNG THƯỜNG)
    public Page<Tour> findTours(String searchTerm, String sortBy, int page, int limit, Long maxPrice, Boolean featured, String destinationId) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Query query = new Query().with(pageable);
        List<Criteria> criteriaList = new ArrayList<>();
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
        populateDestinationsForTours(tours);
        return PageableExecutionUtils.getPage(tours, pageable, () -> total);
    }

    /**
     * ✅ PHƯƠNG THỨC TÌM KIẾM MỚI DÀNH RIÊNG CHO CHATBOT AI
     * Sử dụng Atlas Search ($search) để tìm kiếm thông minh.
     */
    public List<Tour> findToursWithFilters(ExtractedEntities entities) {
        List<AggregationOperation> pipelineOperations = new ArrayList<>();

        // BƯỚC 1: LUÔN LUÔN BẮT ĐẦU VỚI $search
        // Tìm kiếm văn bản bằng Atlas Search Index để thu hẹp kết quả một cách hiệu quả nhất.
        if (StringUtils.hasText(entities.getKeywords())) {
            AggregationOperation searchOperation = context -> new Document("$search",
                    new Document("index", "text_search_index") // Tên index của bạn
                            .append("text", new Document("query", entities.getKeywords())
                                    .append("path", Arrays.asList("title", "description", "city", "category", "tags"))
                            )
            );
            pipelineOperations.add(searchOperation);
        }

        // BƯỚC 2: Nối (join) với collection 'destinations' để lấy thông tin châu lục/tên địa điểm
        AggregationOperation lookupOperation = Aggregation.lookup("destinations", "destinationId", "_id", "destinationDetails");
        pipelineOperations.add(lookupOperation);

        // BƯỚC 3: Lọc chi tiết (match) trên tập dữ liệu đã thu hẹp và join
        List<Criteria> criteriaList = new ArrayList<>();

        // Lọc "cứng" theo địa điểm (location)
        if (StringUtils.hasText(entities.getLocation())) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("city").regex(entities.getLocation(), "i"),
                    // Lọc trên kết quả đã được join từ bước lookup
                    Criteria.where("destinationDetails.name").regex(entities.getLocation(), "i"),
                    Criteria.where("destinationDetails.continent").regex(entities.getLocation(), "i")
            ));
        }

        // Các bộ lọc khác
        if (entities.getMaxPrice() != null && entities.getMaxPrice() > 0) {
            criteriaList.add(Criteria.where("price").lte(entities.getMaxPrice()));
        }
        if (StringUtils.hasText(entities.getCategory())) {
            criteriaList.add(Criteria.where("category").regex(entities.getCategory(), "i"));
        }
        if (StringUtils.hasText(entities.getDuration())) {
            criteriaList.add(Criteria.where("duration").regex(entities.getDuration(), "i"));
        }

        if (!criteriaList.isEmpty()) {
            MatchOperation matchOperation = Aggregation.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
            pipelineOperations.add(matchOperation);
        }

        // BƯỚC 4: Giới hạn số lượng kết quả cuối cùng
        LimitOperation limitOperation = Aggregation.limit(20);
        pipelineOperations.add(limitOperation);

        // Thực thi truy vấn
        Aggregation aggregation = Aggregation.newAggregation(pipelineOperations);
        AggregationResults<Tour> results = mongoTemplate.aggregate(aggregation, "tours", Tour.class);
        List<Tour> candidateTours = results.getMappedResults();

        populateDestinationsForTours(candidateTours);
        return candidateTours;
    }


    // CÁC HÀM CŨ GIỮ NGUYÊN
    public Tour findTourById(String id) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tour với ID: " + id));
        populateDestinationsForTours(List.of(tour));
        return tour;
    }

    /**
     * ✅ Sửa "private" thành "public" để GeminiService có thể gọi
     * Làm đầy thông tin Destination cho một danh sách các Tour.
     */
    public void populateDestinationsForTours(List<Tour> tours) {
        if (tours == null || tours.isEmpty()) return;

        // Lấy danh sách các destinationId duy nhất từ các tour
        List<String> destIds = tours.stream()
                .map(Tour::getDestinationId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (destIds.isEmpty()) return;

        // Tìm tất cả các destination tương ứng trong một lần gọi DB
        List<Destination> destinations = destinationRepository.findAllById(destIds);
        Map<String, Destination> destMap = destinations.stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity()));

        // Gán đối tượng Destination vào từng tour
        tours.forEach(tour -> {
            if (tour.getDestinationId() != null) {
                tour.setDestination(destMap.get(tour.getDestinationId()));
            }
        });
    }

    public Tour createTour(Tour tourData) {
        destinationRepository.findById(tourData.getDestinationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Destination với ID: " + tourData.getDestinationId()));
        return tourRepository.save(tourData);
    }

    public Tour updateTour(String id, Tour tourDetails) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tour với ID: " + id));
        tour.setTitle(tourDetails.getTitle());
        tour.setCity(tourDetails.getCity());
        tour.setDescription(tourDetails.getDescription());
        tour.setDestinationId(tourDetails.getDestinationId());
        tour.setPrice(tourDetails.getPrice());
        tour.setDuration(tourDetails.getDuration());
        tour.setImage(tourDetails.getImage());
        tour.setFeatured(tourDetails.getFeatured());
        tour.setImages(tourDetails.getImages());
        tour.setStartLocation(tourDetails.getStartLocation());
        tour.setEndLocation(tourDetails.getEndLocation());
        tour.setIncluded(tourDetails.getIncluded());
        tour.setExcluded(tourDetails.getExcluded());
        tour.setTags(tourDetails.getTags());
        tour.setCategory(tourDetails.getCategory());
        tour.setDepartures(tourDetails.getDepartures());
        tour.setItinerary(tourDetails.getItinerary());
        return tourRepository.save(tour);
    }

    public void deleteTour(String id) {
        if (!tourRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy tour với ID: " + id);
        }
        tourRepository.deleteById(id);
    }
}