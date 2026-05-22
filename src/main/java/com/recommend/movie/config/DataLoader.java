package com.recommend.movie.config;

import com.recommend.movie.model.Movie;
import com.recommend.movie.model.Episode;
import com.recommend.movie.model.Review;
import com.recommend.movie.model.User;
import com.recommend.movie.repository.MovieRepository;
import com.recommend.movie.repository.EpisodeRepository;
import com.recommend.movie.repository.ReviewRepository;
import com.recommend.movie.repository.UserRepository;
import com.recommend.movie.service.MovieService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("null")
public class DataLoader implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final EpisodeRepository episodeRepository;
    private final MovieService movieService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataLoader(MovieRepository movieRepository, UserRepository userRepository,
                      ReviewRepository reviewRepository, EpisodeRepository episodeRepository,
                      MovieService movieService) {
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.episodeRepository = episodeRepository;
        this.movieService = movieService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Initiating movies catalog synchronization from Oracle APEX...");
        try {
            movieService.syncMoviesFromOracle();
        } catch (Exception e) {
            System.err.println("Oracle sync failed: " + e.getMessage());
        }

        if (movieRepository.count() == 0) {
            System.out.println("Seeding database with default movies, users, and ratings (Oracle Sync Fallback)...");
            seedDefaultMoviesAndRatings();
        } else {
            System.out.println("Movies are present in local repository (either synced from Oracle or already seeded).");
            if (userRepository.count() == 0) {
                System.out.println("Seeding users...");
                User alice = new User("alice", "alice@example.com", passwordEncoder.encode("password"), Arrays.asList("Sci-Fi", "Action"));
                User bob = new User("bob", "bob@example.com", passwordEncoder.encode("password"), Arrays.asList("Drama", "Romance"));
                User charlie = new User("charlie", "charlie@example.com", passwordEncoder.encode("password"), Arrays.asList("Crime", "Action"));
                User diana = new User("diana", "diana@example.com", passwordEncoder.encode("password"), Arrays.asList("Animation", "Fantasy"));
                User ethan = new User("ethan", "ethan@example.com", passwordEncoder.encode("password"), Arrays.asList("Drama", "Thriller"));
                userRepository.saveAll(Arrays.asList(alice, bob, charlie, diana, ethan));
            }
        }

        // Import MovieLens movies if present in movies.dat
        importMovieLensMovies();

        // Seed episodes for all movies in the DB that don't have episodes yet
        seedEpisodes();
    }

    private void seedDefaultMoviesAndRatings() throws Exception {

        // 1. Seed Movies
        // Using high-quality curated Unsplash images for posters/backdrops that evoke the theme
        Movie inception = new Movie(
                "Inception",
                "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.O.O., but his tragic past may doom the project.",
                2010,
                Arrays.asList("Sci-Fi", "Action", "Thriller"),
                "https://image.tmdb.org/t/p/w500/xn0Kcg4e6p0mLxVS3nAWhNmW2Ni.jpg", // Neon/film
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
                "Christopher Nolan",
                "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page, Tom Hardy"
        ).withImdbRating(8.8);

        Movie interstellar = new Movie(
                "Interstellar",
                "When Earth becomes uninhabitable, a team of explorers travels through a wormhole in space in an attempt to ensure humanity's survival.",
                2014,
                Arrays.asList("Sci-Fi", "Drama", "Adventure"),
                "https://image.tmdb.org/t/p/w500/xbiycuc84TrieEWwkkuH2hoEa9S.jpg", // Space
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/2ssWTSVklAEc98frZUQhgtGHx7s.jpg",
                "Christopher Nolan",
                "Matthew McConaughey, Anne Hathaway, Jessica Chastain, Mackenzie Foy"
        ).withImdbRating(8.7);

        Movie darkKnight = new Movie(
                "The Dark Knight",
                "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.",
                2008,
                Arrays.asList("Action", "Crime", "Drama"),
                "https://image.tmdb.org/t/p/w500/7IPCEr7ifdH5CtU97QG7XgAAtOp.jpg", // Joker/dark
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/cfT29Im5VDvjE0RpyKOSdCKZal7.jpg",
                "Christopher Nolan",
                "Christian Bale, Heath Ledger, Aaron Eckhart, Maggie Gyllenhaal"
        ).withImdbRating(9.0);

        Movie pulpFiction = new Movie(
                "Pulp Fiction",
                "The lives of two mob hitmen, a boxer, a gangster and his wife, and a pair of diner bandits intertwine in four tales of violence and redemption.",
                1994,
                Arrays.asList("Crime", "Drama"),
                "https://image.tmdb.org/t/p/w500/AgY33Wtg4737MhYopJSFyKWhKsO.jpg", // Retro neon
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/suaEOtk1N1sgg2MTM7oZd2cfVp3.jpg",
                "Quentin Tarantino",
                "John Travolta, Uma Thurman, Samuel L. Jackson, Bruce Willis"
        ).withImdbRating(8.9);

        Movie matrix = new Movie(
                "The Matrix",
                "When a beautiful stranger leads computer hacker Neo to a forbidding underworld, he discovers the shocking truth--the life he knows is the elaborate deception of an evil cyber-intelligence.",
                1999,
                Arrays.asList("Sci-Fi", "Action"),
                "https://image.tmdb.org/t/p/w500/aOIuZAjPaRIE6CMzbazvcHuHXDc.jpg", // Green matrix code
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/tlm8UkiQsitc8rSuIAscQDCnP8d.jpg",
                "Lana Wachowski, Lilly Wachowski",
                "Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss, Hugo Weaving"
        ).withImdbRating(8.7);

        Movie avatar = new Movie(
                "Avatar",
                "A paraplegic Marine dispatched to the moon Pandora on a unique mission becomes torn between following his orders and protecting the world he feels is his home.",
                2009,
                Arrays.asList("Sci-Fi", "Adventure", "Action"),
                "https://image.tmdb.org/t/p/w500/gKY6q7SjCkAU6FqvqWybDYgUKIF.jpg", // Glowing forest
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/vL5LR6WdxWPjLPFRLe133jXWsh5.jpg",
                "James Cameron",
                "Sam Worthington, Zoe Saldana, Sigourney Weaver, Stephen Lang"
        ).withImdbRating(7.9);

        Movie titanic = new Movie(
                "Titanic",
                "A seventeen-year-old aristocrat falls in love with a kind but poor artist aboard the luxurious, ill-fated R.M.S. Titanic.",
                1997,
                Arrays.asList("Drama", "Romance"),
                "https://image.tmdb.org/t/p/w500/hEntfzxB8yUXIxqZY929dELjLsi.jpg", // Ocean
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/xnHVX37XZEp33hhCbYlQFq7ux1J.jpg",
                "James Cameron",
                "Leonardo DiCaprio, Kate Winslet, Billy Zane, Kathy Bates"
        ).withImdbRating(7.9);

        Movie spiritedAway = new Movie(
                "Spirited Away",
                "During her family's move to the suburbs, a sullen 10-year-old girl wanders into a world ruled by gods, witches, and spirits, and where humans are changed into beasts.",
                2001,
                Arrays.asList("Animation", "Fantasy", "Adventure"),
                "https://image.tmdb.org/t/p/w500/rdZ3T34AGXLglT6g6Xto7hPxr7h.jpg", // Anime/fantasy style
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/ynpXfIy775fMdvhed3gY4T5ZbxC.jpg",
                "Hayao Miyazaki",
                "Rumi Hiiragi, Miyu Irino, Mari Natsuki, Takashi Naito"
        ).withImdbRating(8.6);

        Movie godfather = new Movie(
                "The Godfather",
                "Don Vito Corleone, head of a mafia family, decides to hand over his empire to his youngest son Michael. However, his decision unintentionally puts the lives of his loved ones in grave danger.",
                1972,
                Arrays.asList("Crime", "Drama"),
                "https://image.tmdb.org/t/p/w500/vseIVRdN4xasYwStQIi6SI7DcEu.jpg", // Noir lighting
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/tSPT36ZKlP2WVHJLM4cQPLSzv3b.jpg",
                "Francis Ford Coppola",
                "Marlon Brando, Al Pacino, James Caan, Diane Keaton"
        ).withImdbRating(9.2);

        Movie laLaLand = new Movie(
                "La La Land",
                "While navigating their careers in Los Angeles, a pianist and an actress fall in love while attempting to reconcile their aspirations for the future.",
                2016,
                Arrays.asList("Romance", "Drama", "Comedy"),
                "https://image.tmdb.org/t/p/w500/xDBZNak6HyOEjKIbrjqDxllWXRn.jpg", // Purple twilight / music
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/nlPCdZlHtRNcF6C9hzUH4ebmV1w.jpg",
                "Damien Chazelle",
                "Ryan Gosling, Emma Stone, Rosemarie DeWitt, J.K. Simmons"
        ).withImdbRating(8.0);

        Movie parasite = new Movie(
                "Parasite",
                "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim clan.",
                2019,
                Arrays.asList("Thriller", "Drama", "Comedy"),
                "https://image.tmdb.org/t/p/w500/7lUu9vV2tgH2ZGbBaIo5vSLvFle.jpg", // Modern glass house
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/sRQgHkE5Z1wPK8wvVcVjbNBtI6h.jpg",
                "Bong Joon Ho",
                "Song Kang-ho, Lee Sun-kyun, Cho Yeo-jeong, Choi Woo-shik"
        ).withImdbRating(8.5);

        Movie knivesOut = new Movie(
                "Knives Out",
                "A detective investigates the death of the patriarch of an eccentric, combative family.",
                2019,
                Arrays.asList("Comedy", "Mystery", "Crime"),
                "https://image.tmdb.org/t/p/w500/tZ6ABcJr1GeGff7fyEsReWXnXN.jpg", // Mansion library
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/4HWAQu28e2yaWrtupFPGFkdNU7V.jpg",
                "Rian Johnson",
                "Daniel Craig, Chris Evans, Ana de Armas, Jamie Lee Curtis"
        ).withImdbRating(7.9);

        Movie gladiator = new Movie(
                "Gladiator",
                "A former Roman General sets out to exact vengeance against the corrupt emperor who murdered his family and sent him into slavery.",
                2000,
                Arrays.asList("Action", "Drama", "Adventure"),
                "https://image.tmdb.org/t/p/w500/kRSCWiRPEVhUIleK1bZKMZ5uZ4p.jpg", // Rome/Colosseum
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/jhk6D8pim3yaByu1801kMoxXFaX.jpg",
                "Ridley Scott",
                "Russell Crowe, Joaquin Phoenix, Connie Nielsen, Oliver Reed"
        ).withImdbRating(8.5);

        Movie avengersEndgame = new Movie(
                "Avengers: Endgame",
                "After the devastating events of Avengers: Infinity War, the universe is in ruins. With the help of remaining allies, the Avengers assemble once more in order to reverse Thanos' actions.",
                2019,
                Arrays.asList("Action", "Sci-Fi", "Adventure"),
                "https://image.tmdb.org/t/p/w500/zx1uMVXSJrzYCA8KTpmeEBqBx66.jpg", // Superhero silhouette
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/7RyHsO4yDXtBv1zUU3mTpHeQ0d5.jpg",
                "Anthony Russo, Joe Russo",
                "Robert Downey Jr., Chris Evans, Mark Ruffalo, Chris Hemsworth, Scarlett Johansson"
        ).withImdbRating(8.4);

        // Additional Movies and Shows requested by the user
        Movie duneI = new Movie(
                "Dune I",
                "Paul Atreides, a brilliant and gifted young man born into a great destiny beyond his understanding, must travel to the most dangerous planet in the universe to ensure the future of his family and his people.",
                2021,
                Arrays.asList("Sci-Fi", "Adventure", "Drama"),
                "https://image.tmdb.org/t/p/w500/roeYIqWHyVHcjWDk6SOaepNEk0y.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/zRKQW58MBEY078AxkHxEJzUskCl.jpg",
                "Denis Villeneuve",
                "Timothée Chalamet, Rebecca Ferguson, Oscar Isaac"
        ).withImdbRating(8.0);

        Movie duneII = new Movie(
                "Dune II",
                "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
                2024,
                Arrays.asList("Sci-Fi", "Adventure", "Action"),
                "https://image.tmdb.org/t/p/w500/tihf8Trht9zP3scmUQfvGlAY9FU.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg",
                "Denis Villeneuve",
                "Timothée Chalamet, Zendaya, Rebecca Ferguson"
        ).withImdbRating(8.6);

        Movie duneIII = new Movie(
                "Dune III",
                "The epic continuation of Paul Atreides' destiny as he navigates the complex political and religious consequences of his rule on Arrakis.",
                2026,
                Arrays.asList("Sci-Fi", "Adventure", "Drama"),
                "https://image.tmdb.org/t/p/w500/b4wekkUaxExzOeGe7hKXzhnyXHt.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/5jnVGQXsc0oXLlWD9q6KuwacWQ2.jpg",
                "Denis Villeneuve",
                "Timothée Chalamet, Zendaya, Florence Pugh"
        ).withImdbRating(8.2);

        Movie prometheus = new Movie(
                "Prometheus",
                "Following clues to the origin of mankind, a team finds a structure on a distant moon, but they soon realize they are not alone.",
                2012,
                Arrays.asList("Sci-Fi", "Adventure", "Mystery"),
                "https://image.tmdb.org/t/p/w500/4IZIY5Nx72P9nqEVLEh46xZCEwy.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/sM42bpH73dmf3kJrE8xr4kztYqR.jpg",
                "Ridley Scott",
                "Noomi Rapace, Michael Fassbender, Charlize Theron"
        ).withImdbRating(7.0);

        Movie blacklist = new Movie(
                "The Blacklist",
                "A new FBI profiler, Elizabeth Keen, has her entire life uprooted when a mysterious criminal, Raymond Reddington, who has eluded capture for decades, turns himself in and insists on speaking only to her.",
                2013,
                Arrays.asList("Crime", "Drama", "Mystery"),
                "https://image.tmdb.org/t/p/w500/4HTfd1PhgFUenJxVuBDNdLmdr0c.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/2eIlCirgcvEwmCSYh2wDfz5Sxvz.jpg",
                "Jon Bokenkamp",
                "James Spader, Megan Boone, Diego Klattenhoff"
        ).withImdbRating(8.0);

        Movie personOfInterest = new Movie(
                "Person of Interest",
                "An ex-CIA agent and a mysterious billionaire computer programmer prevent violent crimes in New York City.",
                2011,
                Arrays.asList("Action", "Crime", "Drama"),
                "https://image.tmdb.org/t/p/w500/f8aIvYk5h7Z8EP3dinCmVgQFYow.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/1UkDX6CK8hJ0oUtUycDTp5Vh9wO.jpg",
                "Jonathan Nolan",
                "Jim Caviezel, Michael Emerson, Taraji P. Henson"
        ).withImdbRating(8.5);

        Movie moneyHeist = new Movie(
                "Money Heist",
                "An unusual group of robbers attempt to carry out the most perfect robbery in Spanish history - stealing 2.4 billion euros from the Royal Mint of Spain.",
                2017,
                Arrays.asList("Action", "Crime", "Drama"),
                "https://image.tmdb.org/t/p/w500/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/gFZriCkpJYsApPZEF3jhxL4yLzG.jpg",
                "Álex Pina",
                "Úrsula Corberó, Álvaro Morte, Itziar Ituño"
        ).withImdbRating(8.2);

        Movie supacell = new Movie(
                "Supacell",
                "A group of ordinary people from South London unexpectedly develop superpowers, with no clear connection between them other than they are all Black.",
                2024,
                Arrays.asList("Action", "Sci-Fi", "Adventure"),
                "https://image.tmdb.org/t/p/w500/vHtNgRLWdMk7wIV2WbqkOzU7HHI.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/st2VoiCq6RVKsHoLFAchB9cTmKO.jpg",
                "Rapman",
                "Tosin Cole, Adelayo Adedayo, Yasmin Monet Prince"
        ).withImdbRating(7.3);

        Movie from = new Movie(
                "From",
                "Unravel the mystery of a nightmarish town in middle America that traps all those who enter.",
                2022,
                Arrays.asList("Mystery", "Thriller", "Horror"),
                "https://image.tmdb.org/t/p/w500/pRtJagIxpfODzzb0T0NAvZSzErC.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/auyEi4Xho43HFvVQnd42LzqBdiV.jpg",
                "John Griffin",
                "Harold Perrineau, Catalina Sandino Moreno, Eion Bailey"
        ).withImdbRating(7.7);

        Movie apex = new Movie(
                "Apex",
                "Five elite hunters pay to hunt down a man on a deserted island, only to find themselves becoming the prey.",
                2021,
                Arrays.asList("Action", "Sci-Fi", "Thriller"),
                "https://image.tmdb.org/t/p/w500/chTkFGToW5bsyw3hgLAe4S5Gt3.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/4gKxQIW91hOTELjY5lzjMbLoGxB.jpg",
                "Edward Drake",
                "Bruce Willis, Neal McDonough, Lochlyn Munro"
        ).withImdbRating(4.8);

        Movie orangeIsNewBlack = new Movie(
                "Orange Is the New Black",
                "Convicted of a decade-old crime of transporting drug money to an ex-girlfriend, normally law-abiding Piper Chapman is sentenced to a year and a half in a women's federal prison.",
                2013,
                Arrays.asList("Comedy", "Drama", "Crime"),
                "https://image.tmdb.org/t/p/w500/ekaa7YjGPTkFLcPhwWXTnARuCEU.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/zJiWhsEYCy5BkFCBONoMnUunyZQ.jpg",
                "Jenji Kohan",
                "Taylor Schilling, Danielle Brooks, Taryn Manning"
        ).withImdbRating(8.0);

        Movie untouchable = new Movie(
                "Untouchable",
                "After he becomes a quadriplegic from a paragliding accident, an aristocrat hires a young man from the projects to be his caregiver.",
                2011,
                Arrays.asList("Comedy", "Drama", "Biography"),
                "https://image.tmdb.org/t/p/w500/tiyHlhIS4Tm6HK8XdwRZX4cc4tS.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/cK070s3Qdn1Ib7Gq8RgIyJKgvu3.jpg",
                "Olivier Nakache, Éric Toledano",
                "François Cluzet, Omar Sy, Anne Le Ny"
        ).withImdbRating(8.5);

        Movie atlas = new Movie(
                "Atlas",
                "A brilliant counterterrorism analyst with a deep distrust of AI discovers it might be her only hope when a mission to capture a renegade robot goes awry.",
                2024,
                Arrays.asList("Action", "Sci-Fi", "Adventure"),
                "https://image.tmdb.org/t/p/w500/bcM2Tl5HlsvPBnL8DKP9Ie6vU4r.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/3TNSoa0UHGEzEz5ndXGjJVKo8RJ.jpg",
                "Brad Peyton",
                "Jennifer Lopez, Simu Liu, Sterling K. Brown"
        ).withImdbRating(5.6);

        Movie theCore = new Movie(
                "The Core",
                "The only way to save Earth from catastrophe is to drill down to its core and set off a series of nuclear explosions.",
                2003,
                Arrays.asList("Action", "Sci-Fi", "Thriller"),
                "https://image.tmdb.org/t/p/w500/iMPR3OFhKNVvJw4eZoRhf9RzfHJ.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/v2YxWPX2yKdo6arIAknJzKLxhZY.jpg",
                "Jon Amiel",
                "Aaron Eckhart, Hilary Swank, Delroy Lindo"
        ).withImdbRating(5.5);

        Movie ghostRider = new Movie(
                "Ghost Rider",
                "When motorcycle stuntman Johnny Blaze sells his soul to the Devil to save his father's life, he is transformed into a fiery agent of vengeance.",
                2007,
                Arrays.asList("Action", "Fantasy", "Thriller"),
                "https://image.tmdb.org/t/p/w500/aUJxqFhCrwg7rKFQq0sfvmzI0eJ.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/zbV32WKHFuEYLXLTpr0rmVjQn11.jpg",
                "Mark Steven Johnson",
                "Nicolas Cage, Eva Mendes, Sam Elliott"
        ).withImdbRating(5.3);

        Movie pandora = new Movie(
                "Pandora",
                "When an earthquake hits a Korean town housing a nuclear power plant, a man risks his life to save the country from disaster.",
                2016,
                Arrays.asList("Action", "Thriller", "Drama"),
                "https://image.tmdb.org/t/p/w500/qwYZSH7ysgM5nIR12hgr1x29Bv2.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces_filter(blur)/qwYZSH7ysgM5nIR12hgr1x29Bv2.jpg",
                "Park Jung-woo",
                "Kim Nam-gil, Kim Young-ae, Moon Jeong-hee"
        ).withImdbRating(6.9);

        Movie horizonLine = new Movie(
                "Horizon Line",
                "A couple flying on a small plane to a tropical island wedding must fight for survival after their pilot suffers a fatal heart attack.",
                2020,
                Arrays.asList("Thriller", "Adventure", "Drama"),
                "https://image.tmdb.org/t/p/w500/hR5rqyLd6jo6URQ0vE4zrgVW3R3.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/2KX79ljurDq6bBXcyoQLROVxkU8.jpg",
                "Mikael Marcimain",
                "Alexander Dreymon, Allison Williams, Keith David"
        ).withImdbRating(4.8);

        Movie strangerThings = new Movie(
                "Stranger Things",
                "When a young boy vanishes, a town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
                2016,
                Arrays.asList("Sci-Fi", "Drama", "Fantasy"),
                "https://image.tmdb.org/t/p/w500/hjVNQA2a12OxkpDEOQTBMbKVZ1K.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                "The Duffer Brothers",
                "Millie Bobby Brown, Winona Ryder, David Harbour"
        ).withImdbRating(8.7);

        Movie threeSixtyFiveDays = new Movie(
                "365 Days",
                "Laura, a fiery young woman, is kidnapped by a dominant Sicilian mafia boss who locks her up and gives her 365 days to fall in love with him.",
                2020,
                Arrays.asList("Drama", "Romance"),
                "https://image.tmdb.org/t/p/w500/jlJsRsfdUocJU5Za7exTIwBDK8E.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/29mZ5bR5m2w3xuvwJH7BHMFQQwH.jpg",
                "Barbara Białowąs, Tomasz Mandes",
                "Michele Morrone, Anna-Maria Sieklucka, Bronisław Wrocławski"
        ).withImdbRating(3.3);

        Movie nowhere = new Movie(
                "Nowhere",
                "A young pregnant woman escapes from a country at war by hiding in a maritime container aboard a cargo ship. After a violent storm, she gives birth to her child while lost at sea.",
                2023,
                Arrays.asList("Thriller", "Drama"),
                "https://image.tmdb.org/t/p/w500/8uvCXfpaU1VveV5w9h05OxN0zdN.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/lUIAlOzNVKunMHo7lFDqH8eGsWK.jpg",
                "Albert Pintó",
                "Anna Castillo, Tamar Novas, Tony Corvillo"
        ).withImdbRating(6.3);

        Movie furiosa = new Movie(
                "Furiosa: A Mad Max Saga",
                "The origin story of renegade warrior Furiosa before her encounter and alliance with Mad Max.",
                2024,
                Arrays.asList("Action", "Sci-Fi", "Adventure"),
                "https://image.tmdb.org/t/p/w500/n1AkCpSi2KylD97xf1PUur3YJRk.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/raph7qjAGTMXaIjVxt6ZDSXRzUr.jpg",
                "George Miller",
                "Anya Taylor-Joy, Chris Hemsworth, Tom Burke"
        ).withImdbRating(7.6);

        Movie angelEyes = new Movie(
                "Angel Eyes",
                "A mysterious man falls in love with a female Chicago police officer, helping her face her traumatic past.",
                2001,
                Arrays.asList("Drama", "Romance", "Thriller"),
                "https://image.tmdb.org/t/p/w500/6ZeMoynnz4pRweKKZ9c2G1absMJ.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/6r38uhBZTZl0KGfEcfi3HNP2N0z.jpg",
                "Luis Mandoki",
                "Jennifer Lopez, Jim Caviezel, Sônia Braga"
        ).withImdbRating(5.7);

        Movie rampage = new Movie(
                "Rampage",
                "When three different animals become infected with a dangerous pathogen, a primatologist must find an antidote to save Chicago.",
                2018,
                Arrays.asList("Action", "Sci-Fi", "Adventure"),
                "https://image.tmdb.org/t/p/w500/nuBHGAMC61h9tb2pg6UGjn11svM.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/vm8mhrjk5nQ562UQ0KbUzi0yOHr.jpg",
                "Brad Peyton",
                "Dwayne Johnson, Naomie Harris, Malin Åkerman"
        ).withImdbRating(6.1);

        Movie legends = new Movie(
                "Legends",
                "Years after a plague kills most of humanity and transforms the rest into monsters, the sole survivor in New York City struggles valiantly to find a cure.",
                2007,
                Arrays.asList("Sci-Fi", "Action", "Thriller"),
                "https://image.tmdb.org/t/p/w500/6aN0R1fmC4H5CeWWOm6WikyNgMP.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/aTLq0TMKdsmIy1ZyFM1LfPs326d.jpg",
                "Francis Lawrence",
                "Will Smith, Alice Braga, Charlie Tahan"
        ).withImdbRating(7.2);

        Movie snowpiercer = new Movie(
                "Snowpiercer",
                "In a future where a failed climate-change experiment has killed all life except for the lucky few who boarded the Snowpiercer, a train that travels around the globe, a new class system emerges.",
                2013,
                Arrays.asList("Sci-Fi", "Action", "Drama"),
                "https://image.tmdb.org/t/p/w500/i3jVigrbPMknlz27VMLTvYGAqnI.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/53TORYZzluJ5cV2EW59j4Axlb5W.jpg",
                "Bong Joon Ho",
                "Chris Evans, Song Kang-ho, Tilda Swinton"
        ).withImdbRating(7.1);

        Movie damsel = new Movie(
                "Damsel",
                "A dutiful damsel agrees to marry a handsome prince, only to find the royal family has recruited her as a sacrifice to repay an ancient debt.",
                2024,
                Arrays.asList("Fantasy", "Adventure", "Action"),
                "https://image.tmdb.org/t/p/w500/AgHbB9DCE9aE57zkHjSmseszh6e.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/deLWkOLZmBNkm8p16igfapQyqeq.jpg",
                "Juan Carlos Fresnadillo",
                "Millie Bobby Brown, Ray Winstone, Robin Wright"
        ).withImdbRating(6.1);

        Movie sisu = new Movie(
                "Sisu",
                "When an ex-soldier who discovers gold in the wilderness of Lapland tries to take the loot into the city, Nazi soldiers led by a brutal SS officer try to take it from him.",
                2022,
                Arrays.asList("Action", "History", "War"),
                "https://image.tmdb.org/t/p/w500/tzJUDVWiPIPShIQxA6wUz3bBtJv.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/pBdQ4iorzRV2G38mdS6rzrmUfMA.jpg",
                "Jalmari Helander",
                "Jorma Tommila, Aksel Hennie, Jack Doolan"
        ).withImdbRating(6.9);

        Movie carryOn = new Movie(
                "Carry-On",
                "A mysterious traveler blackmails a young TSA agent to let a dangerous package slip through security on Christmas Eve.",
                2024,
                Arrays.asList("Thriller", "Action", "Crime"),
                "https://image.tmdb.org/t/p/w500/aIcEk2fIN6ob7OtIBBKod6YCnZB.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/rhc8Mtuo3Kh8CndnlmTNMF8o9pU.jpg",
                "Jaume Collet-Serra",
                "Taron Egerton, Jason Bateman, Sofia Carson"
        ).withImdbRating(6.7);

        Movie abigail = new Movie(
                "Abigail",
                "After a group of criminals kidnap the ballerina daughter of a powerful underworld figure, they retreat to an isolated mansion, unaware that they're locked inside with no normal little girl.",
                2024,
                Arrays.asList("Horror", "Thriller"),
                "https://image.tmdb.org/t/p/w500/zT8lFDAHxlNCCxxmn72daL2ENbk.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/2TPoqmatGDfBOiRxqNoL11ncCJe.jpg",
                "Matt Bettinelli-Olpin, Tyler Gillett",
                "Melissa Barrera, Dan Stevens, Alisha Weir"
        ).withImdbRating(6.6);

        Movie theGreatFlood = new Movie(
                "The Great Flood",
                "A scientific team in the future attempts to rescue key survivors during an apocalyptic flood that threatens all of humanity.",
                2023,
                Arrays.asList("Sci-Fi", "Action", "Drama"),
                "https://image.tmdb.org/t/p/w500/sdkuOaiZf2Bfcr5zrQmdrXggSEe.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/b6VfIRoiIErOihQ3JXgWYhCJXDA.jpg",
                "Byun Seung-min",
                "Lee Byung-hun, Park Seo-joon, Park Bo-young"
        ).withImdbRating(6.0);

        Movie sixUnderground = new Movie(
                "6 Underground",
                "Meet a new kind of action hero. Six agents from all around the globe, each the very best at what they do, have been chosen not only for their skill, but for a unique desire to delete their pasts to change the future.",
                2019,
                Arrays.asList("Action", "Thriller", "Comedy"),
                "https://image.tmdb.org/t/p/w500/lnWkyG3LLgbbrIEeyl5mK5VRFe4.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/eFw5YSorHidsajLTayo1noueIxI.jpg",
                "Michael Bay",
                "Ryan Reynolds, Mélanie Laurent, Manuel Garcia-Rulfo"
        ).withImdbRating(6.1);

        Movie breakingBad = new Movie(
                "Breaking Bad",
                "A high school chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing and selling methamphetamine with a former student in order to secure his family's future.",
                2008,
                Arrays.asList("Crime", "Drama", "Thriller"),
                "https://image.tmdb.org/t/p/w500/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/bsNm9z2TJfe0WO3RedPGWQ8mG1X.jpg",
                "Vince Gilligan",
                "Bryan Cranston, Aaron Paul, Bob Odenkirk"
        ).withImdbRating(9.5);

        Movie youShow = new Movie(
                "You",
                "A dangerously charming, intensely obsessive young man goes to extreme measures to insert himself into the lives of those he is transfixed by.",
                2018,
                Arrays.asList("Thriller", "Drama", "Crime"),
                "https://image.tmdb.org/t/p/w500/oANi0vEE92nuijiZQgPZ88FSxqQ.jpg",
                "https://image.tmdb.org/t/p/w1920_and_h800_multi_faces/gzOIymABxmetAECXtazEYCpMmfb.jpg",
                "Greg Berlanti, Sera Gamble",
                "Penn Badgley, Elizabeth Lail, Victoria Pedretti"
        ).withImdbRating(7.7);

        movieRepository.saveAll(Arrays.asList(
                inception, interstellar, darkKnight, pulpFiction, matrix,
                avatar, titanic, spiritedAway, godfather, laLaLand,
                parasite, knivesOut, gladiator, avengersEndgame,
                duneI, duneII, duneIII, prometheus, blacklist, personOfInterest,
                moneyHeist, supacell, from, apex, orangeIsNewBlack, untouchable,
                atlas, theCore, ghostRider, pandora, horizonLine, strangerThings,
                threeSixtyFiveDays, nowhere, furiosa, angelEyes, rampage, legends,
                snowpiercer, damsel, sisu, carryOn, abigail, theGreatFlood, sixUnderground,
                breakingBad, youShow
        ));


        // 2. Seed Users
        User alice = new User("alice", "alice@example.com", passwordEncoder.encode("password"), Arrays.asList("Sci-Fi", "Action"));
        User bob = new User("bob", "bob@example.com", passwordEncoder.encode("password"), Arrays.asList("Drama", "Romance"));
        User charlie = new User("charlie", "charlie@example.com", passwordEncoder.encode("password"), Arrays.asList("Crime", "Action"));
        User diana = new User("diana", "diana@example.com", passwordEncoder.encode("password"), Arrays.asList("Animation", "Fantasy"));
        User ethan = new User("ethan", "ethan@example.com", passwordEncoder.encode("password"), Arrays.asList("Drama", "Thriller"));

        userRepository.saveAll(Arrays.asList(alice, bob, charlie, diana, ethan));

        // Fetch back seeded entity ids to map correctly
        List<Movie> movies = movieRepository.findAll();
        Movie dbInception = movies.stream().filter(m -> m.getTitle().equals("Inception")).findFirst().get();
        Movie dbInterstellar = movies.stream().filter(m -> m.getTitle().equals("Interstellar")).findFirst().get();
        Movie dbDarkKnight = movies.stream().filter(m -> m.getTitle().equals("The Dark Knight")).findFirst().get();
        Movie dbPulpFiction = movies.stream().filter(m -> m.getTitle().equals("Pulp Fiction")).findFirst().get();
        Movie dbMatrix = movies.stream().filter(m -> m.getTitle().equals("The Matrix")).findFirst().get();
        Movie dbAvatar = movies.stream().filter(m -> m.getTitle().equals("Avatar")).findFirst().get();
        Movie dbTitanic = movies.stream().filter(m -> m.getTitle().equals("Titanic")).findFirst().get();
        Movie dbSpiritedAway = movies.stream().filter(m -> m.getTitle().equals("Spirited Away")).findFirst().get();
        Movie dbGodfather = movies.stream().filter(m -> m.getTitle().equals("The Godfather")).findFirst().get();
        Movie dbLaLaLand = movies.stream().filter(m -> m.getTitle().equals("La La Land")).findFirst().get();
        Movie dbParasite = movies.stream().filter(m -> m.getTitle().equals("Parasite")).findFirst().get();
        Movie dbKnivesOut = movies.stream().filter(m -> m.getTitle().equals("Knives Out")).findFirst().get();
        Movie dbGladiator = movies.stream().filter(m -> m.getTitle().equals("Gladiator")).findFirst().get();

        // 3. Seed Ratings (Dense matrix to make Collaborative Filtering work perfectly)
        
        // Alice (Likes Sci-Fi / Action)
        movieService.rateMovie(alice.getId(), dbInception.getId(), 5);
        movieService.rateMovie(alice.getId(), dbInterstellar.getId(), 5);
        movieService.rateMovie(alice.getId(), dbMatrix.getId(), 5);
        movieService.rateMovie(alice.getId(), dbDarkKnight.getId(), 4);
        movieService.rateMovie(alice.getId(), dbAvatar.getId(), 4);
        movieService.rateMovie(alice.getId(), dbPulpFiction.getId(), 2);
        movieService.rateMovie(alice.getId(), dbTitanic.getId(), 2);
        
        // Bob (Likes Drama / Romance / Musical)
        movieService.rateMovie(bob.getId(), dbTitanic.getId(), 5);
        movieService.rateMovie(bob.getId(), dbLaLaLand.getId(), 5);
        movieService.rateMovie(bob.getId(), dbInterstellar.getId(), 4);
        movieService.rateMovie(bob.getId(), dbSpiritedAway.getId(), 4);
        movieService.rateMovie(bob.getId(), dbGodfather.getId(), 3);
        movieService.rateMovie(bob.getId(), dbInception.getId(), 2);
        movieService.rateMovie(bob.getId(), dbMatrix.getId(), 1);

        // Charlie (Tastes similar to Alice: likes Action, Crime, Sci-Fi)
        movieService.rateMovie(charlie.getId(), dbInception.getId(), 5);
        movieService.rateMovie(charlie.getId(), dbInterstellar.getId(), 5);
        movieService.rateMovie(charlie.getId(), dbMatrix.getId(), 5);
        movieService.rateMovie(charlie.getId(), dbDarkKnight.getId(), 5);
        movieService.rateMovie(charlie.getId(), dbGodfather.getId(), 5); // Will recommend to Alice (Collab)
        movieService.rateMovie(charlie.getId(), dbPulpFiction.getId(), 3);
        movieService.rateMovie(charlie.getId(), dbGladiator.getId(), 4);

        // Diana (Likes Animation / Fantasy / Comedy)
        movieService.rateMovie(diana.getId(), dbSpiritedAway.getId(), 5);
        movieService.rateMovie(diana.getId(), dbKnivesOut.getId(), 5);
        movieService.rateMovie(diana.getId(), dbAvatar.getId(), 4);
        movieService.rateMovie(diana.getId(), dbLaLaLand.getId(), 4);
        movieService.rateMovie(diana.getId(), dbInception.getId(), 3);

        // Ethan (Likes Drama / Thriller / Crime - Tastes slightly similar to Charlie)
        movieService.rateMovie(ethan.getId(), dbGodfather.getId(), 5);
        movieService.rateMovie(ethan.getId(), dbPulpFiction.getId(), 5);
        movieService.rateMovie(ethan.getId(), dbDarkKnight.getId(), 4);
        movieService.rateMovie(ethan.getId(), dbParasite.getId(), 5); // Will recommend to Charlie
        movieService.rateMovie(ethan.getId(), dbInception.getId(), 3);
        movieService.rateMovie(ethan.getId(), dbTitanic.getId(), 3);

        // 4. Seed Reviews
        reviewRepository.save(new Review(alice.getId(), "alice", dbInception.getId(), "Mind-bending masterpiece! The visual effects and sound design are out of this world. Nolan does it again."));
        reviewRepository.save(new Review(bob.getId(), "bob", dbInception.getId(), "A bit too convoluted for my taste, though the acting is top notch."));
        reviewRepository.save(new Review(charlie.getId(), "charlie", dbInception.getId(), "Amazing conceptual execution. The hallway fight scene is iconic."));

        reviewRepository.save(new Review(alice.getId(), "alice", dbInterstellar.getId(), "Cried like a baby. The organ music score by Hans Zimmer gives me goosebumps every single time."));
        reviewRepository.save(new Review(bob.getId(), "bob", dbLaLaLand.getId(), "A beautiful love letter to cinema and jazz. Emma Stone and Ryan Gosling have incredible chemistry."));
        reviewRepository.save(new Review(ethan.getId(), "ethan", dbGodfather.getId(), "The pinnacle of cinematic storytelling. Every shot, every line of dialogue is perfection. Marlon Brando's presence is legendary."));
        reviewRepository.save(new Review(diana.getId(), "diana", dbSpiritedAway.getId(), "Pure magic. The hand-drawn animation is gorgeous and the story is incredibly moving. Miyazaki is a genius."));

        System.out.println("Default seeding completed successfully.");
    }

    private void importMovieLensMovies() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/movies.dat")) {
            if (is != null) {
                System.out.println("Found movies.dat in classpath. Parsing and importing MovieLens dataset...");
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                String line;
                List<Movie> moviesToSave = new ArrayList<>();
                
                // Get existing movie titles & years in db to avoid duplicate insert
                List<Movie> existingMovies = movieRepository.findAll();
                Set<String> existingKeys = existingMovies.stream()
                        .map(m -> m.getTitle().toLowerCase() + "_" + m.getReleaseYear())
                        .collect(Collectors.toSet());
                
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split("::");
                    if (parts.length >= 3) {
                        try {
                            String titleWithYear = parts[1].trim();
                            String genresStr = parts[2].trim();
                            
                            // Extract title and year (e.g., "Toy Story (1995)")
                            String title = titleWithYear;
                            int year = 1995;
                            int openParen = titleWithYear.lastIndexOf('(');
                            int closeParen = titleWithYear.lastIndexOf(')');
                            if (openParen != -1 && closeParen != -1 && openParen < closeParen) {
                                String yearStr = titleWithYear.substring(openParen + 1, closeParen);
                                try {
                                    year = Integer.parseInt(yearStr);
                                    title = titleWithYear.substring(0, openParen).trim();
                                } catch (NumberFormatException e) {
                                    // ignore
                                }
                            }
                            
                            String key = title.toLowerCase() + "_" + year;
                            if (existingKeys.contains(key)) {
                                continue;
                            }
                            
                            // Parse genres split by |
                            List<String> genres = Arrays.asList(genresStr.split("\\|"));
                            
                            // Select background and poster based on primary genre
                            String primaryGenre = genres.isEmpty() ? "Default" : genres.get(0);
                            String poster = getUnsplashPoster(primaryGenre);
                            String backdrop = getUnsplashBackdrop(primaryGenre);
                            
                            Movie movie = new Movie(
                                    title,
                                    "MovieLens classic movie: " + title + " (" + year + ") in genres: " + genresStr.replace("|", ", ") + ".",
                                    year,
                                    genres,
                                    poster,
                                    backdrop,
                                    "Unknown Director",
                                    "Cast unknown"
                            ).withImdbRating(7.0); // default rating
                            
                            moviesToSave.add(movie);
                        } catch (Exception e) {
                            System.err.println("Error parsing MovieLens line: " + line + " - " + e.getMessage());
                        }
                    }
                }
                
                if (!moviesToSave.isEmpty()) {
                    movieRepository.saveAll(moviesToSave);
                    System.out.println("Imported " + moviesToSave.size() + " MovieLens movies successfully.");
                } else {
                    System.out.println("All MovieLens movies from movies.dat are already imported.");
                }
            } else {
                System.err.println("movies.dat not found in resources directory!");
            }
        } catch (Exception e) {
            System.err.println("Error reading movies.dat: " + e.getMessage());
        }
    }

    private String getUnsplashPoster(String genre) {
        if (genre == null) return "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&q=80";
        switch (genre.trim()) {
            case "Action":
                return "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80";
            case "Adventure":
                return "https://images.unsplash.com/photo-1501555088652-021faa106b9b?w=500&q=80";
            case "Animation":
                return "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&q=80";
            case "Children's":
            case "Children":
                return "https://images.unsplash.com/photo-1485546246426-74dc88dec4d9?w=500&q=80";
            case "Comedy":
                return "https://images.unsplash.com/photo-1514306191717-452ec28c7814?w=500&q=80";
            case "Crime":
                return "https://images.unsplash.com/photo-1506869642237-8f2e7124d53d?w=500&q=80";
            case "Documentary":
                return "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&q=80";
            case "Drama":
                return "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500&q=80";
            case "Fantasy":
                return "https://images.unsplash.com/photo-1519074069444-1ba4e6664104?w=500&q=80";
            case "Film-Noir":
                return "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500&q=80";
            case "Horror":
                return "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=500&q=80";
            case "Musical":
                return "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80";
            case "Mystery":
                return "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=500&q=80";
            case "Romance":
                return "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=500&q=80";
            case "Sci-Fi":
                return "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&q=80";
            case "Thriller":
                return "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=500&q=80";
            case "War":
                return "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80";
            case "Western":
                return "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=500&q=80";
            default:
                return "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&q=80";
        }
    }

    private String getUnsplashBackdrop(String genre) {
        if (genre == null) return "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1920&q=80";
        switch (genre.trim()) {
            case "Action":
                return "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=1920&q=80";
            case "Adventure":
                return "https://images.unsplash.com/photo-1501555088652-021faa106b9b?w=1920&q=80";
            case "Animation":
                return "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1920&q=80";
            case "Children's":
            case "Children":
                return "https://images.unsplash.com/photo-1485546246426-74dc88dec4d9?w=1920&q=80";
            case "Comedy":
                return "https://images.unsplash.com/photo-1514306191717-452ec28c7814?w=1920&q=80";
            case "Crime":
                return "https://images.unsplash.com/photo-1506869642237-8f2e7124d53d?w=1920&q=80";
            case "Documentary":
                return "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1920&q=80";
            case "Drama":
                return "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1920&q=80";
            case "Fantasy":
                return "https://images.unsplash.com/photo-1519074069444-1ba4e6664104?w=1920&q=80";
            case "Film-Noir":
                return "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=1920&q=80";
            case "Horror":
                return "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=1920&q=80";
            case "Musical":
                return "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1920&q=80";
            case "Mystery":
                return "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1920&q=80";
            case "Romance":
                return "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=1920&q=80";
            case "Sci-Fi":
                return "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920&q=80";
            case "Thriller":
                return "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=1920&q=80";
            case "War":
                return "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=1920&q=80";
            case "Western":
                return "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1920&q=80";
            default:
                return "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1920&q=80";
        }
    }

    private void seedEpisodes() {
        System.out.println("Checking and seeding episodes for all movies...");
        Set<String> tvShows = new HashSet<>(Arrays.asList(
            "Stranger Things", "The Blacklist", "Person of Interest", "Money Heist",
            "Breaking Bad", "You", "Orange Is the New Black", "From", "Supacell"
        ));

        List<Episode> allEpisodes = episodeRepository.findAll();
        Set<Long> movieIdsWithEpisodes = allEpisodes.stream()
                .map(ep -> ep.getMovie().getId())
                .collect(Collectors.toSet());

        List<Movie> allMovies = movieRepository.findAll();
        List<Episode> episodesToSave = new ArrayList<>();
        
        for (Movie m : allMovies) {
            if (movieIdsWithEpisodes.contains(m.getId())) {
                continue;
            }
            
            if (tvShows.contains(m.getTitle())) {
                // Seed 5 episodes for Season 1
                episodesToSave.add(new Episode(m, 1, 1, "Chapter One: Start", "The beginning of the mystery unfolds.", "2016-07-15", 45));
                episodesToSave.add(new Episode(m, 1, 2, "Chapter Two: The Discovery", "Secrets begin to leak as investigations heat up.", "2016-07-22", 48));
                episodesToSave.add(new Episode(m, 1, 3, "Chapter Three: Escalation", "Tension reaches a boiling point between key characters.", "2016-07-29", 50));
                episodesToSave.add(new Episode(m, 1, 4, "Chapter Four: Crossroads", "Hard decisions must be made to protect the group.", "2016-08-05", 52));
                episodesToSave.add(new Episode(m, 1, 5, "Chapter Five: Climax", "The truth is revealed in a stunning season finale.", "2016-08-12", 55));
            } else {
                // Movie: Seed 1 feature-length episode
                String title = "Main Feature";
                String desc = m.getDescription();
                Integer duration = 120; // default movie length
                if (m.getTitle() != null) {
                    if (m.getTitle().contains("Dune")) {
                        duration = 155;
                    } else if (m.getTitle().contains("Interstellar")) {
                        duration = 169;
                    } else if (m.getTitle().contains("Inception")) {
                        duration = 148;
                    } else if (m.getTitle().contains("The Dark Knight")) {
                        duration = 152;
                    }
                }
                episodesToSave.add(new Episode(m, 1, 1, title, desc, String.valueOf(m.getReleaseYear()) + "-10-01", duration));
            }
        }
        
        if (!episodesToSave.isEmpty()) {
            episodeRepository.saveAll(episodesToSave);
            System.out.println("Seeded episodes for " + episodesToSave.size() + " movies/shows.");
        } else {
            System.out.println("All movies/shows already have episodes.");
        }
        System.out.println("Episodes seeding complete.");
    }
}
