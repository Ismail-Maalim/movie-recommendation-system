// CineMatch Application Logic

// API Configuration
const API_BASE = '/api';

// Global Application State
const state = {
    currentUser: null,
    moviesList: [],
    watchlist: [],
    currentMovie: null,
    activeView: 'dashboard',
    selectedPrefGenres: new Set()
};

// DOM Elements
const elements = {
    // Navigation & Header
    navItems: document.querySelectorAll('.nav-item'),
    views: document.querySelectorAll('.view'),
    searchInput: document.getElementById('search-input'),
    profileSection: document.getElementById('user-profile-section'),
    loggedOutView: document.getElementById('logged-out-view'),
    loggedInView: document.getElementById('logged-in-view'),
    profileAvatar: document.getElementById('profile-avatar'),
    profileUsername: document.getElementById('profile-username'),
    currentUserStatus: document.getElementById('current-user-status'),
    userStatusDot: document.getElementById('user-status-dot'),
    
    // Triggers
    btnLoginTrigger: document.getElementById('btn-login-trigger'),
    btnPrefTrigger: document.getElementById('btn-pref-trigger'),
    btnLogout: document.getElementById('btn-logout'),
    
    // Dashboard & Grids
    heroSpotlight: document.getElementById('hero-spotlight'),
    heroBg: document.getElementById('hero-bg'),
    heroTitle: document.getElementById('hero-title'),
    heroYear: document.getElementById('hero-year'),
    heroRating: document.getElementById('hero-rating'),
    heroImdbContainer: document.getElementById('hero-imdb-rating-container'),
    heroImdbRating: document.getElementById('hero-imdb-rating'),
    heroDesc: document.getElementById('hero-desc'),
    heroDetailsBtn: document.getElementById('hero-details-btn'),
    heroWatchlistBtn: document.getElementById('hero-watchlist-btn'),
    heroWatchlistIcon: document.getElementById('hero-watchlist-icon'),
    trendingGrid: document.getElementById('trending-grid'),
    discoverGrid: document.getElementById('discover-grid'),
    recommendationsGrid: document.getElementById('recommendations-grid'),
    watchlistGrid: document.getElementById('watchlist-grid'),
    genrePills: document.getElementById('genre-pills'),
    recsExplainerDesc: document.getElementById('recs-explainer-desc'),
    
    // Movie Details Modal
    movieDetailsModal: document.getElementById('movie-details-modal'),
    btnCloseDetails: document.getElementById('btn-close-details'),
    modalBackdropImg: document.getElementById('modal-backdrop-img'),
    modalPosterImg: document.getElementById('modal-poster-img'),
    modalTitle: document.getElementById('modal-title'),
    modalGenres: document.getElementById('modal-genres'),
    modalYear: document.getElementById('modal-year'),
    modalDirector: document.getElementById('modal-director'),
    modalRating: document.getElementById('modal-rating'),
    modalImdbContainer: document.getElementById('modal-imdb-rating-container'),
    modalImdbRating: document.getElementById('modal-imdb-rating'),
    modalPlot: document.getElementById('modal-plot'),
    modalCast: document.getElementById('modal-cast'),
    modalReviewsList: document.getElementById('modal-reviews-list'),
    starRatingInput: document.getElementById('star-rating-input'),
    ratingStatusText: document.getElementById('rating-status-text'),
    reviewTextarea: document.getElementById('review-textarea'),
    btnSubmitReview: document.getElementById('btn-submit-review'),
    
    // Auth Modal
    authModal: document.getElementById('auth-modal'),
    btnCloseAuth: document.getElementById('btn-close-auth'),
    loginPanel: document.getElementById('login-panel'),
    registerPanel: document.getElementById('register-panel'),
    switchToRegister: document.getElementById('switch-to-register'),
    switchToLogin: document.getElementById('switch-to-login'),
    loginForm: document.getElementById('login-form'),
    registerForm: document.getElementById('register-form'),
    
    // Preferences Modal
    preferencesModal: document.getElementById('preferences-modal'),
    btnClosePref: document.getElementById('btn-close-pref'),
    prefGenresGrid: document.getElementById('pref-genres-grid'),
    btnSavePreferences: document.getElementById('btn-save-preferences'),
    
    // Toast Container
    toastContainer: document.getElementById('toast-container')
};

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

async function initApp() {
    setupEventListeners();
    setupStarRating();
    
    // Attempt local storage auto-login
    const savedUser = localStorage.getItem('cinematch_user');
    if (savedUser) {
        state.currentUser = JSON.parse(savedUser);
        showToast(`Welcome back, ${state.currentUser.username}!`, 'success');
        updateUserUI();
        await fetchWatchlist();
    } else {
        updateUserUI();
    }

    // Load initial data
    await fetchMovies();
    renderDashboard();
}

// Event Listeners Setup
function setupEventListeners() {
    // View navigation
    elements.navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            const viewName = item.getAttribute('data-view');
            switchView(viewName);
        });
    });

    // Auth modal triggers
    elements.btnLoginTrigger.addEventListener('click', () => openModal(elements.authModal));
    elements.btnCloseAuth.addEventListener('click', () => closeModal(elements.authModal));
    elements.switchToRegister.addEventListener('click', () => {
        elements.loginPanel.style.display = 'none';
        elements.registerPanel.style.display = 'block';
    });
    elements.switchToLogin.addEventListener('click', () => {
        elements.registerPanel.style.display = 'none';
        elements.loginPanel.style.display = 'block';
    });

    // Preferences modal triggers
    elements.btnPrefTrigger.addEventListener('click', openPreferencesModal);
    elements.btnClosePref.addEventListener('click', () => closeModal(elements.preferencesModal));
    elements.btnSavePreferences.addEventListener('click', savePreferences);

    // Close movie details modal
    elements.btnCloseDetails.addEventListener('click', () => closeModal(elements.movieDetailsModal));

    // Submit rating/review
    elements.btnSubmitReview.addEventListener('click', submitReview);

    // Search bar event
    elements.searchInput.addEventListener('input', debounce(handleSearch, 300));

    // Sign out button
    elements.btnLogout.addEventListener('click', handleLogout);

    // Auth form submissions
    elements.loginForm.addEventListener('submit', handleLogin);
    elements.registerForm.addEventListener('submit', handleRegister);

    // Genre pill filtering
    elements.genrePills.addEventListener('click', (e) => {
        if (e.target.classList.contains('genre-pill')) {
            document.querySelectorAll('.genre-pill').forEach(p => p.classList.remove('active'));
            e.target.classList.add('active');
            const genre = e.target.getAttribute('data-genre');
            filterDiscoverByGenre(genre);
        }
    });

    // Close modals on clicking outside container
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal-overlay')) {
            closeModal(e.target);
        }
    });
}

// Switch Views
function switchView(viewName) {
    if (viewName === 'recommendations' && !state.currentUser) {
        showToast('Please sign in to view recommendations!', 'error');
        openModal(elements.authModal);
        return;
    }

    state.activeView = viewName;
    
    // Update nav active classes
    elements.navItems.forEach(item => {
        if (item.getAttribute('data-view') === viewName) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Update visibility
    elements.views.forEach(view => {
        if (view.id === `view-${viewName}`) {
            view.classList.add('active');
        } else {
            view.classList.remove('active');
        }
    });

    // Fetch view specific content
    if (viewName === 'dashboard') {
        renderDashboard();
    } else if (viewName === 'discover') {
        // Reset search field and filter pill
        elements.searchInput.value = '';
        document.querySelectorAll('.genre-pill').forEach(p => p.classList.remove('active'));
        document.querySelector('.genre-pill[data-genre="all"]').classList.add('active');
        renderDiscover(state.moviesList);
    } else if (viewName === 'recommendations') {
        fetchRecommendations();
    } else if (viewName === 'watchlist') {
        renderWatchlist();
    }
}

// Fetch Movies
async function fetchMovies() {
    try {
        const response = await fetch(`${API_BASE}/movies`);
        if (response.ok) {
            state.moviesList = await response.json();
        } else {
            showToast('Failed to load movies catalog', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Server connection failed', 'error');
    }
}

// Fetch Watchlist
async function fetchWatchlist() {
    if (!state.currentUser) return;
    try {
        const response = await fetch(`${API_BASE}/movies/watchlist?userId=${state.currentUser.id}`);
        if (response.ok) {
            state.watchlist = await response.json();
        }
    } catch (e) {
        console.error(e);
    }
}

// Fetch Recommendations
async function fetchRecommendations() {
    if (!state.currentUser) return;
    
    elements.recommendationsGrid.innerHTML = '<div class="no-reviews"><i class="fa-solid fa-spinner fa-spin" style="font-size: 24px; color: var(--primary);"></i><p style="margin-top: 10px;">Analyzing taste profile...</p></div>';
    
    try {
        const response = await fetch(`${API_BASE}/recommendations?userId=${state.currentUser.id}`);
        if (response.ok) {
            const recs = await response.json();
            renderRecommendations(recs);
        } else {
            showToast('Failed to retrieve recommendations', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Server error during recommendation mapping', 'error');
    }
}

// Render Dashboard (Spotlight & Grid)
function renderDashboard() {
    if (state.moviesList.length === 0) return;

    // Set Spotlight Movie (default to highest rated)
    const sorted = [...state.moviesList].sort((a, b) => b.averageRating - a.averageRating);
    const spotlight = sorted[0];

    // Setup Spotlight HTML
    elements.heroTitle.textContent = spotlight.title;
    elements.heroBg.style.backgroundImage = `url('${spotlight.backdropUrl || spotlight.posterUrl}')`;
    elements.heroYear.textContent = spotlight.releaseYear;
    elements.heroRating.textContent = spotlight.averageRating.toFixed(1);
    elements.heroDesc.textContent = spotlight.description;
    
    // Setup Spotlight IMDb Rating
    if (spotlight.imdbRating > 0) {
        if (elements.heroImdbContainer) {
            elements.heroImdbContainer.style.display = 'inline-flex';
            elements.heroImdbRating.textContent = spotlight.imdbRating.toFixed(1);
        }
    } else {
        if (elements.heroImdbContainer) {
            elements.heroImdbContainer.style.display = 'none';
        }
    }
    
    // Set Spotlight click events
    elements.heroDetailsBtn.onclick = () => openMovieDetails(spotlight);
    
    // Check Watchlist status for spotlight
    const inWatchlist = state.watchlist.some(m => m.id === spotlight.id);
    updateWatchlistButtonState(elements.heroWatchlistBtn, elements.heroWatchlistIcon, inWatchlist);
    elements.heroWatchlistBtn.onclick = (e) => {
        e.stopPropagation();
        toggleWatchlist(spotlight, elements.heroWatchlistBtn, elements.heroWatchlistIcon);
    };

    // Render Trending grid (exclude spotlight, show next 6 highest rated)
    const trending = sorted.slice(1, 9);
    renderMoviesGrid(elements.trendingGrid, trending);
}

// Render Discover Grid
function renderDiscover(movies) {
    renderMoviesGrid(elements.discoverGrid, movies);
}

// Render Recommendations Grid
function renderRecommendations(recs) {
    elements.recommendationsGrid.innerHTML = '';
    
    if (recs.length === 0) {
        elements.recommendationsGrid.innerHTML = '<div class="no-reviews"><p>No recommendations available. Rate some movies to help us understand your tastes!</p></div>';
        return;
    }

    // Set Explainer Description text based on what is recommended
    const hasCF = recs.some(r => r.recommendationType === 'COLLABORATIVE' || r.recommendationType === 'HYBRID');
    if (hasCF) {
        elements.recsExplainerDesc.innerHTML = `We successfully computed a **Collaborative profile** based on users with matching rating habits, blended with **Content-Based overlap** on your favorite genres: <strong style="color: var(--secondary);">${state.currentUser.preferredGenres.join(', ') || 'None'}</strong>.`;
    } else {
        elements.recsExplainerDesc.innerHTML = `Since we are bootstrapping your profile, recommendations are populated from your **explicit favorite genres**: <strong style="color: var(--secondary);">${state.currentUser.preferredGenres.join(', ') || 'None'}</strong> combined with popular items. **Rate more movies** to trigger Collaborative predictions!`;
    }

    recs.forEach(rec => {
        const movieCard = createMovieCard(rec.movie, rec);
        elements.recommendationsGrid.appendChild(movieCard);
    });
}

// Render Watchlist Grid
function renderWatchlist() {
    elements.watchlistGrid.innerHTML = '';
    if (state.watchlist.length === 0) {
        elements.watchlistGrid.innerHTML = '<div class="no-reviews"><i class="fa-solid fa-bookmark" style="font-size: 24px; margin-bottom:10px;"></i><p>Your watchlist is empty. Tap bookmarks on movie cards to add them here.</p></div>';
        return;
    }
    renderMoviesGrid(elements.watchlistGrid, state.watchlist);
}

// Generic Movies Grid Builder
function renderMoviesGrid(container, movies) {
    container.innerHTML = '';
    if (movies.length === 0) {
        container.innerHTML = '<div class="no-reviews"><p>No movies matched your search criteria.</p></div>';
        return;
    }
    movies.forEach(movie => {
        const card = createMovieCard(movie);
        container.appendChild(card);
    });
}

// Create Card Element
function createMovieCard(movie, recommendation = null) {
    const card = document.createElement('div');
    card.className = 'movie-card';
    
    const inWatchlist = state.watchlist.some(m => m.id === movie.id);
    
    // Compute badges
    let matchBadgeHtml = '';
    let typeBadgeHtml = '';
    if (recommendation) {
        matchBadgeHtml = `<div class="recommendation-match">${recommendation.matchPercentage}% Match</div>`;
        
        let typeText = recommendation.recommendationType.replace('_', ' ');
        typeBadgeHtml = `<div class="recommendation-type">${typeText}</div>`;
    }

    card.innerHTML = `
        <div class="card-poster">
            <img src="${movie.posterUrl}" alt="${movie.title}" onerror="this.src='https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500&q=80'">
            ${matchBadgeHtml}
            ${typeBadgeHtml}
            <div class="card-action-overlay">
                <button class="card-action-btn ${inWatchlist ? 'active' : ''}" data-action="watchlist" title="Add to Watchlist">
                    <i class="fa-solid fa-bookmark"></i>
                </button>
            </div>
        </div>
        <div class="card-content">
            <div>
                <h4 class="card-title">${movie.title}</h4>
                <div class="card-genres">${movie.genres.join(', ')}</div>
            </div>
            <div class="card-footer">
                <span class="card-year">${movie.releaseYear}</span>
                <div style="display: flex; align-items: center; gap: 8px;">
                    ${movie.imdbRating > 0 ? `<div class="imdb-badge">IMDb ${movie.imdbRating.toFixed(1)}</div>` : ''}
                    <span class="card-rating">
                        <i class="fa-solid fa-star"></i>
                        ${movie.averageRating > 0 ? movie.averageRating.toFixed(1) : 'Unrated'}
                    </span>
                </div>
            </div>
        </div>
    `;

    // Click handler to open details
    card.addEventListener('click', (e) => {
        // Prevent opening if clicking action button
        if (e.target.closest('.card-action-btn')) return;
        openMovieDetails(movie);
    });

    // Add Action Button listeners
    const watchlistBtn = card.querySelector('[data-action="watchlist"]');
    watchlistBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleWatchlist(movie, watchlistBtn);
    });

    return card;
}

// Filter Discover View by Genre
function filterDiscoverByGenre(genre) {
    if (genre === 'all') {
        renderDiscover(state.moviesList);
    } else {
        const filtered = state.moviesList.filter(m => 
            m.genres.some(g => g.toLowerCase() === genre.toLowerCase())
        );
        renderDiscover(filtered);
    }
}

// Search Logic
async function handleSearch() {
    const query = elements.searchInput.value.trim();
    
    // Force view switch to discover to see search results
    if (state.activeView !== 'discover') {
        switchView('discover');
        elements.searchInput.value = query; // repopulate search field
    }

    if (query.length === 0) {
        renderDiscover(state.moviesList);
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/movies?search=${encodeURIComponent(query)}`);
        if (response.ok) {
            const results = await response.json();
            renderDiscover(results);
        }
    } catch (e) {
        console.error(e);
    }
}

// Toggle Watchlist Operation
async function toggleWatchlist(movie, button, icon = null) {
    if (!state.currentUser) {
        showToast('Please sign in to manage your watchlist!', 'error');
        openModal(elements.authModal);
        return;
    }

    const inWatchlist = state.watchlist.some(m => m.id === movie.id);
    const url = `${API_BASE}/movies/watchlist/${inWatchlist ? 'remove' : 'add'}`;
    
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: state.currentUser.id,
                movieId: movie.id
            })
        });

        if (response.ok) {
            if (inWatchlist) {
                state.watchlist = state.watchlist.filter(m => m.id !== movie.id);
                showToast(`Removed "${movie.title}" from Watchlist`, 'success');
                updateWatchlistButtonState(button, icon, false);
            } else {
                state.watchlist.push(movie);
                showToast(`Added "${movie.title}" to Watchlist`, 'success');
                updateWatchlistButtonState(button, icon, true);
            }

            // Sync watchlist view if open
            if (state.activeView === 'watchlist') {
                renderWatchlist();
            }
            // Sync dashboard spotlight buttons
            if (state.activeView === 'dashboard') {
                renderDashboard();
            }
        } else {
            showToast('Unable to update watchlist', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Connection error updating watchlist', 'error');
    }
}

function updateWatchlistButtonState(button, icon, isActive) {
    if (button.id === 'hero-watchlist-btn') {
        if (isActive) {
            button.classList.add('active');
            button.style.backgroundColor = 'var(--secondary)';
            button.style.borderColor = 'var(--secondary)';
            if (icon) {
                icon.className = 'fa-solid fa-check';
            }
            button.innerHTML = '<i class="fa-solid fa-check"></i> In Watchlist';
        } else {
            button.classList.remove('active');
            button.style.backgroundColor = 'rgba(255, 255, 255, 0.08)';
            button.style.borderColor = 'var(--glass-border)';
            if (icon) {
                icon.className = 'fa-solid fa-plus';
            }
            button.innerHTML = '<i class="fa-solid fa-plus"></i> Watchlist';
        }
    } else {
        // Cards icon toggle
        if (isActive) {
            button.classList.add('active');
        } else {
            button.classList.remove('active');
        }
    }
}

// Modal open/close actions
function openModal(modal) {
    modal.classList.add('active');
}

function closeModal(modal) {
    modal.classList.remove('active');
    
    // Clear inputs in movie modal if closed
    if (modal.id === 'movie-details-modal') {
        state.currentMovie = null;
        elements.reviewTextarea.value = '';
    }
}

// Movie Details Modal
async function openMovieDetails(movie) {
    state.currentMovie = movie;
    
    // Basic Details
    elements.modalTitle.textContent = movie.title;
    elements.modalBackdropImg.src = movie.backdropUrl || movie.posterUrl;
    elements.modalPosterImg.src = movie.posterUrl;
    elements.modalYear.textContent = movie.releaseYear;
    elements.modalDirector.textContent = `Director: ${movie.director}`;
    elements.modalRating.textContent = movie.averageRating > 0 ? movie.averageRating.toFixed(1) : 'Unrated';
    elements.modalPlot.textContent = movie.description;
    elements.modalCast.textContent = movie.castMembers;

    // IMDb Rating Setup
    if (movie.imdbRating > 0) {
        if (elements.modalImdbContainer) {
            elements.modalImdbContainer.style.display = 'inline-flex';
            elements.modalImdbRating.textContent = movie.imdbRating.toFixed(1);
        }
    } else {
        if (elements.modalImdbContainer) {
            elements.modalImdbContainer.style.display = 'none';
        }
    }

    // Genres Tags
    elements.modalGenres.innerHTML = '';
    movie.genres.forEach(g => {
        const tag = document.createElement('span');
        tag.className = 'modal-genre-tag';
        tag.textContent = g;
        elements.modalGenres.appendChild(tag);
    });

    // Rating Widget Setup
    resetStars();
    if (state.currentUser) {
        fetchUserRating(movie.id);
    } else {
        elements.ratingStatusText.textContent = 'Sign in to rate this movie';
        elements.starRatingInput.style.pointerEvents = 'none';
    }

    // Reviews List
    await fetchReviews(movie.id);

    openModal(elements.movieDetailsModal);
}

// Fetch Current User's Rating for Movie
async function fetchUserRating(movieId) {
    try {
        const response = await fetch(`${API_BASE}/movies/${movieId}/rating?userId=${state.currentUser.id}`);
        if (response.ok) {
            const rating = await response.json();
            if (rating.score > 0) {
                highlightStars(rating.score);
                elements.ratingStatusText.textContent = `You rated this movie ${rating.score} stars`;
            } else {
                resetStars();
                elements.ratingStatusText.textContent = 'Click stars to rate';
            }
            elements.starRatingInput.style.pointerEvents = 'auto';
        }
    } catch (e) {
        console.error(e);
    }
}

// Fetch Movie Reviews
async function fetchReviews(movieId) {
    elements.modalReviewsList.innerHTML = '<div style="text-align: center; color: var(--text-dimmed);"><i class="fa-solid fa-circle-notch fa-spin"></i> Loading reviews...</div>';
    
    try {
        const response = await fetch(`${API_BASE}/movies/${movieId}/reviews`);
        if (response.ok) {
            const reviews = await response.json();
            elements.modalReviewsList.innerHTML = '';
            
            if (reviews.length === 0) {
                elements.modalReviewsList.innerHTML = '<div class="no-reviews">Be the first to review this movie!</div>';
                return;
            }

            reviews.sort((a,b) => b.timestamp - a.timestamp).forEach(rev => {
                const date = new Date(rev.timestamp).toLocaleDateString(undefined, {month: 'short', day: 'numeric', year: 'numeric'});
                const revCard = document.createElement('div');
                revCard.className = 'review-card';
                revCard.innerHTML = `
                    <div class="review-header">
                        <span class="review-author">@${rev.username}</span>
                        <span class="review-date">${date}</span>
                    </div>
                    <p class="review-body">${escapeHTML(rev.reviewText)}</p>
                `;
                elements.modalReviewsList.appendChild(revCard);
            });
        }
    } catch (e) {
        console.error(e);
        elements.modalReviewsList.innerHTML = '<div class="no-reviews">Failed to load reviews.</div>';
    }
}

// Interactive Star Ratings Selection
function setupStarRating() {
    const stars = elements.starRatingInput.querySelectorAll('i');
    
    stars.forEach(star => {
        star.addEventListener('mouseover', (e) => {
            const score = parseInt(e.target.getAttribute('data-score'));
            hoverStars(score);
        });

        star.addEventListener('mouseout', () => {
            clearHoverStars();
        });

        star.addEventListener('click', async (e) => {
            if (!state.currentUser) return;
            const score = parseInt(e.target.getAttribute('data-score'));
            await submitRating(score);
        });
    });
}

function hoverStars(score) {
    const stars = elements.starRatingInput.querySelectorAll('i');
    stars.forEach(star => {
        const idx = parseInt(star.getAttribute('data-score'));
        if (idx <= score) {
            star.className = 'fa-solid fa-star hovered';
        } else {
            star.className = 'fa-regular fa-star';
        }
    });
}

function clearHoverStars() {
    const stars = elements.starRatingInput.querySelectorAll('i');
    stars.forEach(star => {
        if (star.classList.contains('selected')) {
            star.className = 'fa-solid fa-star selected';
        } else {
            star.className = 'fa-regular fa-star';
        }
    });
}

function highlightStars(score) {
    const stars = elements.starRatingInput.querySelectorAll('i');
    stars.forEach(star => {
        const idx = parseInt(star.getAttribute('data-score'));
        if (idx <= score) {
            star.className = 'fa-solid fa-star selected';
        } else {
            star.className = 'fa-regular fa-star';
            star.classList.remove('selected');
        }
    });
}

function resetStars() {
    const stars = elements.starRatingInput.querySelectorAll('i');
    stars.forEach(star => {
        star.className = 'fa-regular fa-star';
        star.classList.remove('selected');
    });
}

// Submit rating to API
async function submitRating(score) {
    if (!state.currentMovie || !state.currentUser) return;

    try {
        const response = await fetch(`${API_BASE}/movies/${state.currentMovie.id}/rate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: state.currentUser.id,
                score: score
            })
        });

        if (response.ok) {
            showToast(`Rated ${state.currentMovie.title} - ${score} Stars`, 'success');
            highlightStars(score);
            elements.ratingStatusText.textContent = `You rated this movie ${score} stars`;
            
            // Recalculate movie local average rating and sync app catalogs
            await fetchMovies();
            
            // Find current movie in the refreshed array and update rating representation
            const updatedMovie = state.moviesList.find(m => m.id === state.currentMovie.id);
            if (updatedMovie) {
                elements.modalRating.textContent = updatedMovie.averageRating.toFixed(1);
            }
            
            // Sync recommendation views and dashboard
            if (state.activeView === 'dashboard') renderDashboard();
        } else {
            showToast('Rating submission failed', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Connection error submitting rating', 'error');
    }
}

// Submit Review to API
async function submitReview() {
    if (!state.currentUser) {
        showToast('Please sign in to submit a review!', 'error');
        return;
    }

    const reviewText = elements.reviewTextarea.value.trim();
    if (reviewText.length === 0) {
        showToast('Review text cannot be empty', 'error');
        return;
    }

    elements.btnSubmitReview.disabled = true;
    elements.btnSubmitReview.textContent = 'Submitting...';

    try {
        const response = await fetch(`${API_BASE}/movies/${state.currentMovie.id}/reviews`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: state.currentUser.id,
                username: state.currentUser.username,
                reviewText: reviewText
            })
        });

        if (response.ok) {
            showToast('Review submitted successfully!', 'success');
            elements.reviewTextarea.value = '';
            // Refresh list
            await fetchReviews(state.currentMovie.id);
        } else {
            showToast('Failed to submit review', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Server connection issues', 'error');
    } finally {
        elements.btnSubmitReview.disabled = false;
        elements.btnSubmitReview.textContent = 'Submit';
    }
}

// Login Handler
async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value.trim();

    try {
        const response = await fetch(`${API_BASE}/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const user = await response.json();
            state.currentUser = user;
            localStorage.setItem('cinematch_user', JSON.stringify(user));
            
            showToast(`Welcome back, ${user.username}!`, 'success');
            closeModal(elements.authModal);
            
            updateUserUI();
            await fetchWatchlist();
            
            // Reload views
            if (state.activeView === 'dashboard') {
                renderDashboard();
            } else if (state.activeView === 'recommendations') {
                fetchRecommendations();
            }
        } else {
            const err = await response.json();
            showToast(err.message || 'Login failed', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server login failure', 'error');
    }
}

// Register Handler
async function handleRegister(e) {
    e.preventDefault();
    const username = document.getElementById('register-username').value.trim();
    const email = document.getElementById('register-email').value.trim();
    const password = document.getElementById('register-password').value.trim();

    try {
        const response = await fetch(`${API_BASE}/users/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username,
                email,
                password,
                preferredGenres: ['Sci-Fi', 'Action'] // default seed
            })
        });

        if (response.ok) {
            const user = await response.json();
            state.currentUser = user;
            localStorage.setItem('cinematch_user', JSON.stringify(user));
            
            showToast(`Account created! Welcome, ${user.username}`, 'success');
            closeModal(elements.authModal);
            
            updateUserUI();
            
            // Redirect to Preferences Selection to onboard user
            openPreferencesModal();
        } else {
            const err = await response.json();
            showToast(err.message || 'Registration failed', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server registration failure', 'error');
    }
}

// Logout Handler
function handleLogout() {
    const oldUsername = state.currentUser ? state.currentUser.username : '';
    state.currentUser = null;
    state.watchlist = [];
    localStorage.removeItem('cinematch_user');
    
    showToast(`Logged out successfully`, 'success');
    updateUserUI();
    
    // If on recommendation view, force redirect back to dashboard
    if (state.activeView === 'recommendations' || state.activeView === 'watchlist') {
        switchView('dashboard');
    } else {
        renderDashboard(); // refreshes watchlist checks on grids
    }
}

// Sync UI components to current User login status
function updateUserUI() {
    if (state.currentUser) {
        // Authenticated State
        elements.loggedOutView.style.display = 'none';
        elements.loggedInView.style.display = 'flex';
        
        // Avatar Initial
        const firstLetter = state.currentUser.username.substring(0,1).toUpperCase();
        elements.profileAvatar.textContent = firstLetter;
        elements.profileUsername.textContent = state.currentUser.username;
        elements.currentUserStatus.textContent = state.currentUser.username;
        elements.userStatusDot.style.color = '#10b981'; // Green active status
    } else {
        // Guest State
        elements.loggedInView.style.display = 'none';
        elements.loggedOutView.style.display = 'flex';
        elements.currentUserStatus.textContent = 'Guest Mode';
        elements.userStatusDot.style.color = 'var(--text-dimmed)';
    }
}

// Open Preferences Modal
function openPreferencesModal() {
    if (!state.currentUser) return;
    
    state.selectedPrefGenres = new Set(
        (state.currentUser.preferredGenres || []).map(g => g.toLowerCase())
    );
    
    // Highlight correct pills
    const checkboxes = elements.prefGenresGrid.querySelectorAll('.pref-checkbox-btn');
    checkboxes.forEach(box => {
        const genre = box.getAttribute('data-genre').toLowerCase();
        if (state.selectedPrefGenres.has(genre)) {
            box.classList.add('selected');
        } else {
            box.classList.remove('selected');
        }
    });

    // Add checkbox toggle listener
    checkboxes.forEach(box => {
        // Remove previous listeners using cloning
        const newBox = box.cloneNode(true);
        box.parentNode.replaceChild(newBox, box);
        
        newBox.addEventListener('click', (e) => {
            const genreName = e.target.getAttribute('data-genre');
            const genreLower = genreName.toLowerCase();
            
            if (state.selectedPrefGenres.has(genreLower)) {
                state.selectedPrefGenres.delete(genreLower);
                e.target.classList.remove('selected');
            } else {
                state.selectedPrefGenres.add(genreLower);
                e.target.classList.add('selected');
            }
        });
    });

    openModal(elements.preferencesModal);
}

// Save explicit preferences
async function savePreferences() {
    if (!state.currentUser) return;

    // Convert Set back to original casing values mapped to data structure
    const updatedGenres = [];
    const checkboxes = elements.prefGenresGrid.querySelectorAll('.pref-checkbox-btn');
    checkboxes.forEach(box => {
        const genre = box.getAttribute('data-genre');
        if (state.selectedPrefGenres.has(genre.toLowerCase())) {
            updatedGenres.push(genre);
        }
    });

    try {
        const response = await fetch(`${API_BASE}/users/${state.currentUser.id}/preferences`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedGenres)
        });

        if (response.ok) {
            const updatedUser = await response.json();
            state.currentUser = updatedUser;
            localStorage.setItem('cinematch_user', JSON.stringify(updatedUser));
            
            showToast('Preferences updated successfully', 'success');
            closeModal(elements.preferencesModal);

            // Fetch and update recommendations if active
            if (state.activeView === 'recommendations') {
                fetchRecommendations();
            }
        } else {
            showToast('Could not update preferences', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Error syncing preferences to database', 'error');
    }
}

// Show Toast Alerts
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-triangle-exclamation';
    
    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    elements.toastContainer.appendChild(toast);
    
    // Auto dismiss
    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s reverse forwards';
        toast.addEventListener('animationend', () => {
            toast.remove();
        });
    }, 3000);
}

// HTML escape helper to prevent XSS
function escapeHTML(str) {
    return str.replace(/[&<>'"]/g, 
        tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag)
    );
}

// Debounce helper for search performance
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
