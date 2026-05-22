package com.recommend.movie.repository;

import com.recommend.movie.model.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    List<Episode> findByMovieIdOrderBySeasonNumberAscEpisodeNumberAsc(Long movieId);
}
