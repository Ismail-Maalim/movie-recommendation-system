package com.recommend.movie.repository;

import com.recommend.movie.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE LOWER(g) = LOWER(:genre)")
    List<Movie> findByGenreIgnoreCase(@Param("genre") String genre);
}
