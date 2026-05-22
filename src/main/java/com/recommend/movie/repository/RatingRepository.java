package com.recommend.movie.repository;

import com.recommend.movie.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByUserId(Long userId);
    List<Rating> findByMovieId(Long movieId);
    Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId);
}
