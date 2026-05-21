package com.movies.recommendation.controller;

import com.movies.recommendation.service.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final RecommendationService recommendationService;

    public HomeController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) String genre, Model model) {
        model.addAttribute("genre", genre == null ? "" : genre);
        model.addAttribute("recommendations", recommendationService.recommendByGenre(genre));
        return "index";
    }
}
