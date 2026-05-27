// CineMatch Application Logic

// API Configuration
const API_BASE = (() => {
    if (window.location.protocol === 'file:') {
        return 'http://localhost:8080/api';
    }
    const localHosts = ['localhost', '127.0.0.1', '192.168.', '10.'];
    const isLocal = localHosts.some(host => window.location.hostname.includes(host));
    if (isLocal && window.location.port !== '8080') {
        return 'http://localhost:8080/api';
    }
    return '/api';
})();

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
    trendingRow: document.getElementById('trending-row'),
    recommendedRow: document.getElementById('recommended-row'),
    recommendedRowContainer: document.getElementById('recommended-row-container'),
    becauseLikedRow: document.getElementById('because-liked-row'),
    becauseLikedRowContainer: document.getElementById('because-liked-row-container'),
    becauseLikedTitle: document.getElementById('because-liked-title'),
    scifiActionRow: document.getElementById('scifi-action-row'),
    dramaClassicsRow: document.getElementById('drama-classics-row'),
    discoverGrid: document.getElementById('discover-grid'),
    searchGuessContainer: document.getElementById('search-guess-container'),
    recommendationsGrid: document.getElementById('recommendations-grid'),
    watchlistGrid: document.getElementById('watchlist-grid'),
    genrePills: document.getElementById('genre-pills'),
    discoverSort: document.getElementById('discover-sort'),
    discoverYear: document.getElementById('discover-year'),
    recsExplainerDesc: document.getElementById('recs-explainer-desc'),
    
    // Onboarding Wizard
    onboardingWizardModal: document.getElementById('onboarding-wizard-modal'),
    onboardingGenresGrid: document.getElementById('onboarding-genres-grid'),
    btnOnboardingNext1: document.getElementById('btn-onboarding-next-1'),
    onboardingRateSubtitle: document.getElementById('onboarding-rate-subtitle'),
    onboardingMoviesGrid: document.getElementById('onboarding-movies-grid'),
    btnOnboardingPrev2: document.getElementById('btn-onboarding-prev-2'),
    btnOnboardingNext2: document.getElementById('btn-onboarding-next-2'),
    onboardingLoaderStatus: document.getElementById('onboarding-loader-status'),
    onboardingProgressFill: document.getElementById('onboarding-progress-fill'),
    onboardingStepPanel1: document.getElementById('onboarding-step-panel-1'),
    onboardingStepPanel2: document.getElementById('onboarding-step-panel-2'),
    onboardingStepPanel3: document.getElementById('onboarding-step-panel-3'),
    
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
    modalEpisodesSection: document.getElementById('modal-episodes-section'),
    episodesSeasonSelect: document.getElementById('episodes-season-select'),
    modalEpisodesContainer: document.getElementById('modal-episodes-container'),
    episodesTitleLabel: document.getElementById('episodes-title-label'),
    
    // Auth Modal
    authModal: document.getElementById('auth-modal'),
    btnCloseAuth: document.getElementById('btn-close-auth'),
    loginPanel: document.getElementById('login-panel'),
    registerPanel: document.getElementById('register-panel'),
    switchToRegister: document.getElementById('switch-to-register'),
    switchToLogin: document.getElementById('switch-to-login'),
    loginForm: document.getElementById('login-form'),
    registerForm: document.getElementById('register-form'),
    otpPanel: document.getElementById('otp-panel'),
    otpForm: document.getElementById('otp-form'),
    otpCode: document.getElementById('otp-code'),
    cancelOtp: document.getElementById('cancel-otp'),
    oauthEmailPanel: document.getElementById('oauth-email-panel'),
    oauthEmailTitle: document.getElementById('oauth-email-title'),
    oauthEmailForm: document.getElementById('oauth-email-form'),
    oauthEmailInput: document.getElementById('oauth-email-input'),
    cancelOauthEmail: document.getElementById('cancel-oauth-email'),
    googleChooserPanel: document.getElementById('google-chooser-panel'),
    cancelGoogleChooser: document.getElementById('cancel-google-chooser'),
    appleChooserPanel: document.getElementById('apple-chooser-panel'),
    cancelAppleChooser: document.getElementById('cancel-apple-chooser'),
    
    // Settings Modal
    btnSettingsTrigger: document.getElementById('btn-settings-trigger'),
    settingsModal: document.getElementById('settings-drawer-overlay'),
    btnCloseSettings: document.getElementById('btn-close-settings'),
    settingsForm: document.getElementById('settings-form'),
    settingsUsername: document.getElementById('settings-username'),
    settingsEmail: document.getElementById('settings-email'),
    settingsAvatarPreview: document.getElementById('settings-avatar-preview'),
    
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

    // Check if view parameter is passed in URL
    const urlParams = new URLSearchParams(window.location.search);
    const viewParam = urlParams.get('view');
    if (viewParam) {
        switchView(viewParam);
    }
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
        // Reset password input and validation status
        const registerPasswordInput = document.getElementById('register-password');
        if (registerPasswordInput) {
            registerPasswordInput.value = '';
            validatePasswordRules('');
        }
    });
    elements.switchToLogin.addEventListener('click', () => {
        elements.registerPanel.style.display = 'none';
        elements.loginPanel.style.display = 'block';
    });

    // Toggle password visibility eye icons
    // Password visibility toggle is handled globally via inline onclick in index.html to ensure 100% reliability and click capture.

    // Password strength indicator event listener
    const registerPasswordInput = document.getElementById('register-password');
    if (registerPasswordInput) {
        registerPasswordInput.addEventListener('input', (e) => {
            const pwd = e.target.value;
            validatePasswordRules(pwd);
        });
    }

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

    // Onboarding page trigger
    const btnOnboardingTrigger = document.getElementById('btn-onboarding-trigger');
    if (btnOnboardingTrigger) {
        btnOnboardingTrigger.addEventListener('click', () => {
            if (state.currentUser) {
                window.location.href = `onboarding.html?userId=${state.currentUser.id}`;
            }
        });
    }

    // Auth form submissions
    elements.loginForm.addEventListener('submit', handleLogin);
    elements.registerForm.addEventListener('submit', handleRegister);

    // Settings modal events
    if (elements.btnSettingsTrigger) {
        elements.btnSettingsTrigger.addEventListener('click', openSettingsModal);
    }
    if (elements.btnCloseSettings) {
        elements.btnCloseSettings.addEventListener('click', () => closeModal(elements.settingsModal));
    }
    if (elements.settingsForm) {
        elements.settingsForm.addEventListener('submit', handleSaveSettings);
    }

    // Avatar selector interactive events
    document.querySelectorAll('.avatar-icon-option').forEach(el => {
        el.addEventListener('click', (e) => {
            document.querySelectorAll('.avatar-icon-option').forEach(opt => opt.classList.remove('active'));
            el.classList.add('active');
            updateAvatarPreview();
        });
    });

    document.querySelectorAll('.avatar-gradient-option').forEach(el => {
        el.addEventListener('click', (e) => {
            document.querySelectorAll('.avatar-gradient-option').forEach(opt => opt.classList.remove('active'));
            el.classList.add('active');
            updateAvatarPreview();
        });
    });

    // OAuth simulated events
    if (elements.cancelGoogleChooser) {
        elements.cancelGoogleChooser.addEventListener('click', () => {
            if (elements.googleChooserPanel) elements.googleChooserPanel.style.display = 'none';
            elements.loginPanel.style.display = 'block';
        });
    }
    if (elements.cancelAppleChooser) {
        elements.cancelAppleChooser.addEventListener('click', () => {
            if (elements.appleChooserPanel) elements.appleChooserPanel.style.display = 'none';
            elements.loginPanel.style.display = 'block';
        });
    }
    if (elements.cancelOauthEmail) {
        elements.cancelOauthEmail.addEventListener('click', () => {
            elements.oauthEmailPanel.style.display = 'none';
            if (state.oauthProvider === 'google' && elements.googleChooserPanel) {
                elements.googleChooserPanel.style.display = 'block';
            } else if (state.oauthProvider === 'apple' && elements.appleChooserPanel) {
                elements.appleChooserPanel.style.display = 'block';
            } else {
                elements.loginPanel.style.display = 'block';
            }
        });
    }
    if (elements.oauthEmailForm) {
        elements.oauthEmailForm.addEventListener('submit', handleOAuthEmailSubmit);
    }
    if (elements.cancelOtp) {
        elements.cancelOtp.addEventListener('click', () => {
            elements.otpPanel.style.display = 'none';
            if (state.oauthSource === 'chooser') {
                if (state.oauthProvider === 'google' && elements.googleChooserPanel) {
                    elements.googleChooserPanel.style.display = 'block';
                } else if (state.oauthProvider === 'apple' && elements.appleChooserPanel) {
                    elements.appleChooserPanel.style.display = 'block';
                } else {
                    elements.loginPanel.style.display = 'block';
                }
            } else {
                elements.oauthEmailPanel.style.display = 'block';
            }
        });
    }
    if (elements.otpForm) {
        elements.otpForm.addEventListener('submit', handleOtpSubmit);
    }

    // Genre pill filtering
    elements.genrePills.addEventListener('click', (e) => {
        if (e.target.classList.contains('genre-pill')) {
            document.querySelectorAll('.genre-pill').forEach(p => p.classList.remove('active'));
            e.target.classList.add('active');
            applyDiscoverFilters();
        }
    });

    // Discover sorting and era filtering
    if (elements.discoverSort) {
        elements.discoverSort.addEventListener('change', applyDiscoverFilters);
    }
    if (elements.discoverYear) {
        elements.discoverYear.addEventListener('change', applyDiscoverFilters);
    }

    // Close modals on clicking outside container
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal-overlay') || e.target.classList.contains('drawer-overlay')) {
            closeModal(e.target);
        }
    });

    // Row navigation scroll button click event
    document.querySelectorAll('.row-nav-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const targetId = btn.getAttribute('data-target');
            const targetRow = document.getElementById(targetId);
            if (targetRow) {
                const scrollAmount = 600;
                if (btn.classList.contains('prev')) {
                    targetRow.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
                } else {
                    targetRow.scrollBy({ left: scrollAmount, behavior: 'smooth' });
                }
            }
        });
    });

    // Initialize onboarding event listeners
    setupOnboardingEventListeners();

    // FAQ accordion init
    setupFaqAccordion();

    // CineBot AI agent assistant init
    setupCineBot();
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
        if (elements.discoverSort) elements.discoverSort.value = 'default';
        if (elements.discoverYear) elements.discoverYear.value = 'all';
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

// Render Dashboard (Spotlight & Scrolling Rows)
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

    // Render Scrolling Rows
    // 1. Trending Now
    renderMoviesRow(elements.trendingRow, sorted);

    // 2. Sci-Fi & Action Hits
    const scifiAction = state.moviesList.filter(m => 
        m.genres.some(g => g === 'Sci-Fi' || g === 'Action')
    ).sort((a, b) => b.averageRating - a.averageRating);
    renderMoviesRow(elements.scifiActionRow, scifiAction);

    // 3. Dramas & Classics
    const dramaClassics = state.moviesList.filter(m => 
        m.genres.some(g => g === 'Drama' || g === 'Classics' || g === 'Mystery')
    ).sort((a, b) => b.averageRating - a.averageRating);
    renderMoviesRow(elements.dramaClassicsRow, dramaClassics);

    // 4. Personalized rows (only shown if logged in)
    if (state.currentUser) {
        if (elements.recommendedRowContainer) {
            elements.recommendedRowContainer.style.display = 'block';
            fetchRowRecommendations();
        }
        renderBecauseLikedRow();
    } else {
        if (elements.recommendedRowContainer) {
            elements.recommendedRowContainer.style.display = 'none';
        }
        if (elements.becauseLikedRowContainer) {
            elements.becauseLikedRowContainer.style.display = 'none';
        }
    }
}

// Helper to render movies to a horizontal scrolling row
function renderMoviesRow(container, movies) {
    if (!container) return;
    container.innerHTML = '';
    if (movies.length === 0) {
        container.innerHTML = '<div class="no-reviews"><p>No movies available in this category.</p></div>';
        return;
    }
    movies.forEach(movie => {
        const card = createMovieCard(movie);
        container.appendChild(card);
    });
}

// Helper to fetch recommendations for dashboard row
async function fetchRowRecommendations() {
    if (!state.currentUser || !elements.recommendedRow) return;
    
    elements.recommendedRow.innerHTML = '<div style="padding: 20px; display: flex; align-items: center; justify-content: center; width: 100%;"><i class="fa-solid fa-spinner fa-spin" style="font-size: 24px; color: var(--primary);"></i></div>';
    
    try {
        const response = await fetch(`${API_BASE}/recommendations?userId=${state.currentUser.id}`);
        if (response.ok) {
            const recs = await response.json();
            elements.recommendedRow.innerHTML = '';
            if (recs.length === 0) {
                elements.recommendedRow.innerHTML = '<div style="padding: 20px; color: var(--text-dimmed); text-align: center; width: 100%;">Rate some movies to get personalized recommendations!</div>';
            } else {
                recs.forEach(rec => {
                    const movieCard = createMovieCard(rec.movie, rec);
                    elements.recommendedRow.appendChild(movieCard);
                });
            }
        } else {
            elements.recommendedRow.innerHTML = '<div style="padding: 20px; color: var(--text-dimmed); text-align: center; width: 100%;">Failed to load recommendations.</div>';
        }
    } catch (e) {
        console.error(e);
        elements.recommendedRow.innerHTML = '<div style="padding: 20px; color: var(--text-dimmed); text-align: center; width: 100%;">Connection error.</div>';
    }
}

// Helper to calculate "Because You Liked [Movie]" row
function renderBecauseLikedRow() {
    if (!state.currentUser || !elements.becauseLikedRowContainer || !elements.becauseLikedRow) return;

    const ratingKey = `cinematch_ratings_${state.currentUser.id}`;
    const userRatings = JSON.parse(localStorage.getItem(ratingKey) || '{}');

    // Find user's highest rated movie ID
    let highestMovieId = null;
    let highestScore = 0;

    for (const [movieId, score] of Object.entries(userRatings)) {
        if (score > highestScore) {
            highestScore = score;
            highestMovieId = parseInt(movieId);
        }
    }

    if (!highestMovieId) {
        elements.becauseLikedRowContainer.style.display = 'none';
        return;
    }

    const highestMovie = state.moviesList.find(m => m.id === highestMovieId);
    if (!highestMovie) {
        elements.becauseLikedRowContainer.style.display = 'none';
        return;
    }

    // Set row title text dynamically
    if (elements.becauseLikedTitle) {
        elements.becauseLikedTitle.innerHTML = `<i class="fa-solid fa-heart" style="margin-right: 8px; color: #ef4444;"></i>Because You Liked <strong>${highestMovie.title}</strong>`;
    }

    // Find movies sharing genres with the favorite movie (excluding itself)
    const targetGenres = highestMovie.genres;
    const recommendedMovies = state.moviesList.filter(m => 
        m.id !== highestMovie.id && 
        m.genres.some(g => targetGenres.includes(g))
    ).sort((a, b) => b.averageRating - a.averageRating);

    elements.becauseLikedRowContainer.style.display = 'block';
    renderMoviesRow(elements.becauseLikedRow, recommendedMovies);
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
        if (!typeText.toLowerCase().includes('hybrid') && !typeText.toLowerCase().includes('oracle')) {
            typeBadgeHtml = `<div class="recommendation-type">${typeText}</div>`;
        }
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

// Apply Discover Filters & Sorting
function applyDiscoverFilters() {
    const activePill = document.querySelector('.genre-pill.active');
    const genre = activePill ? activePill.getAttribute('data-genre') : 'all';
    
    // Fallback: Query DOM dynamically to ensure elements are resolved
    const sortSelect = document.getElementById('discover-sort');
    const yearSelect = document.getElementById('discover-year');
    const sortBy = sortSelect ? sortSelect.value : 'default';
    const era = yearSelect ? yearSelect.value : 'all';

    let filtered = [...state.moviesList];

    // 1. Genre filter
    if (genre && genre !== 'all') {
        filtered = filtered.filter(m => 
            m.genres.some(g => g.toLowerCase() === genre.toLowerCase())
        );
    }

    // 2. Era filter
    if (era && era !== 'all') {
        if (era === '2020s') {
            filtered = filtered.filter(m => m.releaseYear >= 2020);
        } else if (era === '2010s') {
            filtered = filtered.filter(m => m.releaseYear >= 2010 && m.releaseYear <= 2019);
        } else if (era === '2000s') {
            filtered = filtered.filter(m => m.releaseYear >= 2000 && m.releaseYear <= 2009);
        } else if (era === '1990s') {
            filtered = filtered.filter(m => m.releaseYear >= 1990 && m.releaseYear <= 1999);
        } else if (era === 'classic') {
            filtered = filtered.filter(m => m.releaseYear < 1990);
        }
    }

    // 3. Sorting
    if (sortBy === 'recent') {
        filtered.sort((a, b) => b.releaseYear - a.releaseYear);
    } else if (sortBy === 'popular') {
        // Sort by average rating, fallback to IMDb, fallback to release year
        filtered.sort((a, b) => {
            if (b.averageRating !== a.averageRating) {
                return b.averageRating - a.averageRating;
            }
            if (b.imdbRating !== a.imdbRating) {
                return b.imdbRating - a.imdbRating;
            }
            return b.releaseYear - a.releaseYear;
        });
    } else if (sortBy === 'imdb') {
        filtered.sort((a, b) => b.imdbRating - a.imdbRating);
    } else if (sortBy === 'az') {
        filtered.sort((a, b) => a.title.localeCompare(b.title));
    }

    renderDiscover(filtered);
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
        if (elements.searchGuessContainer) elements.searchGuessContainer.style.display = 'none';
        renderDiscover(state.moviesList);
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/movies?search=${encodeURIComponent(query)}`);
        if (response.ok) {
            const results = await response.json();
            
            if (results.length > 0) {
                // Clear search guess container since matches exist locally
                if (elements.searchGuessContainer) elements.searchGuessContainer.style.display = 'none';
                renderDiscover(results);
            } else {
                // No local match: search internet (TMDB) to guess title and recommend similar
                guessAndSuggestFromInternet(query);
            }
        }
    } catch (e) {
        console.error(e);
        showNoLocalResults(query);
    }
}

// Autocomplete and Guess from Internet
async function guessAndSuggestFromInternet(query) {
    try {
        const tmdbApiKey = '8265bd1679663a7ea12ac168ea84d2e8';
        const url = `https://api.themoviedb.org/3/search/multi?api_key=${tmdbApiKey}&query=${encodeURIComponent(query)}`;
        const res = await fetch(url);
        if (!res.ok) {
            showNoLocalResults(query);
            return;
        }

        const data = await res.json();
        if (!data.results || data.results.length === 0) {
            showNoLocalResults(query);
            return;
        }

        // Retrieve first relevant search guess
        const guess = data.results[0];
        const guessedTitle = guess.title || guess.name;
        
        if (!guessedTitle) {
            showNoLocalResults(query);
            return;
        }

        // Map TMDB genre IDs to our local categories
        const tmdbGenreIds = guess.genre_ids || [];
        const TMDB_GENRE_MAP = {
            28: "Action", 12: "Adventure", 16: "Animation", 35: "Comedy", 80: "Crime",
            99: "Documentary", 18: "Drama", 10751: "Children's", 14: "Fantasy",
            36: "History", 27: "Horror", 10402: "Musical", 9648: "Mystery",
            10749: "Romance", 878: "Sci-Fi", 53: "Thriller", 10752: "War", 37: "Western",
            10759: "Action", 10762: "Children's", 10765: "Sci-Fi"
        };

        const targetGenres = tmdbGenreIds.map(id => TMDB_GENRE_MAP[id]).filter(Boolean);

        // Find similar movies locally
        let recommendations = [];
        if (targetGenres.length > 0) {
            recommendations = state.moviesList.filter(m => 
                m.genres.some(g => targetGenres.some(tg => tg.toLowerCase() === g.toLowerCase()))
            );
        }

        if (recommendations.length === 0) {
            recommendations = [...state.moviesList];
        }

        // Sort suggestions by popularity and rating
        recommendations.sort((a, b) => {
            if (b.averageRating !== a.averageRating) return b.averageRating - a.averageRating;
            return b.imdbRating - a.imdbRating;
        });

        const limitedRecs = recommendations.slice(0, 12);

        // Render guess header and recommendations grid
        if (elements.searchGuessContainer) {
            elements.searchGuessContainer.innerHTML = `
                <div class="search-guess-banner">
                    <div class="search-guess-icon">🍿</div>
                    <div class="search-guess-text">
                        Looking for <strong style="color: var(--secondary);">"${guessedTitle}"</strong>? We don't have that yet, but you may like:
                    </div>
                </div>
            `;
            elements.searchGuessContainer.style.display = 'block';
        }

        renderDiscover(limitedRecs);

    } catch (err) {
        console.error("TMDB autocomplete guess failed:", err);
        showNoLocalResults(query);
    }
}

function showNoLocalResults(query) {
    if (elements.searchGuessContainer) {
        elements.searchGuessContainer.style.display = 'none';
    }
    elements.discoverGrid.innerHTML = `
        <div class="no-reviews">
            <i class="fa-solid fa-face-frown" style="font-size: 28px; color: var(--text-muted); margin-bottom: 12px;"></i>
            <p>We couldn't find any results for "${query}".</p>
        </div>
    `;
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

    // Episodes List
    elements.modalEpisodesSection.style.display = 'block';
    elements.episodesSeasonSelect.style.display = 'none';
    elements.modalEpisodesContainer.innerHTML = `
        <div class="episode-skeleton"></div>
        <div class="episode-skeleton"></div>
        <div class="episode-skeleton"></div>
    `;
    fetchEpisodes(movie.id);

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

// Fetch and Render Episodes
async function fetchEpisodes(movieId) {
    try {
        const response = await fetch(`${API_BASE}/movies/${movieId}/episodes`);
        if (!response.ok) {
            throw new Error('Failed to fetch episodes');
        }
        const episodes = await response.json();
        
        if (!episodes || episodes.length === 0) {
            elements.modalEpisodesSection.style.display = 'none';
            return;
        }

        // TV show vs. standard Movie detection
        const isSeries = episodes.length > 1;

        if (isSeries) {
            // Group episodes by season
            const seasonsMap = {};
            episodes.forEach(ep => {
                const s = ep.seasonNumber || 1;
                if (!seasonsMap[s]) {
                    seasonsMap[s] = [];
                }
                seasonsMap[s].push(ep);
            });

            // Sort episodes inside seasons
            Object.keys(seasonsMap).forEach(s => {
                seasonsMap[s].sort((a, b) => a.episodeNumber - b.episodeNumber);
            });

            const seasons = Object.keys(seasonsMap).sort((a, b) => a - b);
            
            // Populate season dropdown
            elements.episodesSeasonSelect.innerHTML = '';
            seasons.forEach(s => {
                const opt = document.createElement('option');
                opt.value = s;
                opt.textContent = `Season ${s}`;
                elements.episodesSeasonSelect.appendChild(opt);
            });

            elements.episodesSeasonSelect.style.display = 'block';
            elements.episodesTitleLabel.textContent = 'Episodes';

            // Show episodes of the first season by default
            const renderSeasonEpisodes = (seasonNum) => {
                elements.modalEpisodesContainer.innerHTML = '';
                const seasonEps = seasonsMap[seasonNum] || [];
                
                seasonEps.forEach(ep => {
                    const card = document.createElement('div');
                    card.className = 'episode-card';
                    card.innerHTML = `
                        <div class="episode-number-badge">${ep.episodeNumber}</div>
                        <div class="episode-details-info">
                            <div class="episode-title-row">
                                <div class="episode-title">${ep.title || `Episode ${ep.episodeNumber}`}</div>
                                <div class="episode-duration">${ep.durationMinutes ? ep.durationMinutes + 'm' : ''}</div>
                            </div>
                            <div class="episode-desc">${ep.description || 'No description available.'}</div>
                        </div>
                        <div class="episode-play-icon">
                            <i class="fa-solid fa-play"></i>
                        </div>
                    `;
                    card.addEventListener('click', () => {
                        showToast(`Playing Season ${seasonNum} Episode ${ep.episodeNumber}: "${ep.title}"...`, 'success');
                    });
                    elements.modalEpisodesContainer.appendChild(card);
                });
            };

            renderSeasonEpisodes(seasons[0]);

            // Handle season change select trigger
            elements.episodesSeasonSelect.onchange = (e) => {
                renderSeasonEpisodes(e.target.value);
            };
        } else {
            // Standard movie mode: show a single play feature card
            elements.episodesSeasonSelect.style.display = 'none';
            elements.episodesTitleLabel.textContent = 'Playback';
            elements.modalEpisodesContainer.innerHTML = `
                <div class="movie-play-card" id="btn-play-movie-feature">
                    <div class="movie-play-btn-circle">
                        <i class="fa-solid fa-play"></i>
                    </div>
                    <span style="font-weight: 600; font-size: 15px; color: var(--text-main);">Watch Feature Film</span>
                    <span style="font-size: 13px; color: var(--text-muted);">Stream this title now in Ultra HD 4K</span>
                </div>
            `;
            
            document.getElementById('btn-play-movie-feature').addEventListener('click', () => {
                showToast(`Starting stream: "${state.currentMovie.title}"...`, 'success');
            });
        }
    } catch (e) {
        console.error('Error fetching episodes:', e);
        elements.modalEpisodesSection.style.display = 'none';
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
            
            // Save to local storage ratings log
            const ratingKey = `cinematch_ratings_${state.currentUser.id}`;
            let userRatings = JSON.parse(localStorage.getItem(ratingKey) || '{}');
            userRatings[state.currentMovie.id] = score;
            localStorage.setItem(ratingKey, JSON.stringify(userRatings));
            
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
            
            // Redirect to onboarding page instead of showing the modal
            window.location.href = `onboarding.html?userId=${user.id}`;
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
        
        // Avatar Selection Render
        if (state.currentUser.avatar && state.currentUser.avatar.includes('|')) {
            const [iconClass, gradientClass] = state.currentUser.avatar.split('|');
            elements.profileAvatar.innerHTML = `<i class="fa-solid ${iconClass}"></i>`;
            elements.profileAvatar.className = `user-avatar ${gradientClass}`;
        } else {
            const firstLetter = state.currentUser.username.substring(0,1).toUpperCase();
            elements.profileAvatar.textContent = firstLetter;
            elements.profileAvatar.className = 'user-avatar';
        }
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

// ==========================================
// Onboarding Wizard Implementation
// ==========================================

function startOnboardingWizard() {
    state.onboardingSelectedGenres = new Set();
    state.onboardingRatings = {};
    
    // Reset selected states on step 1 genres
    if (elements.onboardingGenresGrid) {
        elements.onboardingGenresGrid.querySelectorAll('.pref-checkbox-btn').forEach(btn => {
            btn.classList.remove('selected');
        });
    }
    
    if (elements.btnOnboardingNext1) elements.btnOnboardingNext1.disabled = true;
    if (elements.btnOnboardingNext2) elements.btnOnboardingNext2.disabled = true;
    
    // Display the first step panel
    showOnboardingStep(1);
    
    openModal(elements.onboardingWizardModal);
}

function showOnboardingStep(stepNum) {
    if (!elements.onboardingStepPanel1 || !elements.onboardingStepPanel2 || !elements.onboardingStepPanel3) return;
    
    // Toggle active panel classes
    elements.onboardingStepPanel1.classList.remove('active');
    elements.onboardingStepPanel2.classList.remove('active');
    elements.onboardingStepPanel3.classList.remove('active');
    
    const targetPanel = document.getElementById(`onboarding-step-panel-${stepNum}`);
    if (targetPanel) {
        targetPanel.classList.add('active');
    }
    
    // Update visual progress headers
    for (let i = 1; i <= 3; i++) {
        const ind = document.getElementById(`onboarding-step-ind-${i}`);
        const line = document.getElementById(`onboarding-step-line-${i}`);
        
        if (ind) {
            if (i < stepNum) {
                ind.className = 'onboarding-step-indicator completed';
            } else if (i === stepNum) {
                ind.className = 'onboarding-step-indicator active';
            } else {
                ind.className = 'onboarding-step-indicator';
            }
        }
        
        if (line) {
            if (i < stepNum) {
                line.className = 'onboarding-step-line completed';
            } else {
                line.className = 'onboarding-step-line';
            }
        }
    }
}

function setupOnboardingEventListeners() {
    if (!elements.onboardingGenresGrid) return;
    
    // Genres click handler
    const genreBtns = elements.onboardingGenresGrid.querySelectorAll('.pref-checkbox-btn');
    genreBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const genre = btn.getAttribute('data-genre');
            if (state.onboardingSelectedGenres.has(genre)) {
                state.onboardingSelectedGenres.delete(genre);
                btn.classList.remove('selected');
            } else {
                state.onboardingSelectedGenres.add(genre);
                btn.classList.add('selected');
            }
            
            // Enable next button if at least 3 genres are selected
            if (elements.btnOnboardingNext1) {
                elements.btnOnboardingNext1.disabled = state.onboardingSelectedGenres.size < 3;
            }
        });
    });

    // Step 1 -> Step 2
    if (elements.btnOnboardingNext1) {
        elements.btnOnboardingNext1.addEventListener('click', () => {
            renderOnboardingSeedingMovies();
            showOnboardingStep(2);
        });
    }

    // Step 2 Back -> Step 1
    if (elements.btnOnboardingPrev2) {
        elements.btnOnboardingPrev2.addEventListener('click', () => {
            showOnboardingStep(1);
        });
    }

    // Step 2 Complete -> Step 3
    if (elements.btnOnboardingNext2) {
        elements.btnOnboardingNext2.addEventListener('click', () => {
            startTasteCalculationAnimation();
        });
    }
}

function renderOnboardingSeedingMovies() {
    if (!elements.onboardingMoviesGrid || state.moviesList.length === 0) return;
    
    elements.onboardingMoviesGrid.innerHTML = '';
    
    // Sort movies by averageRating descending and select the top 12
    const seedMovies = [...state.moviesList]
        .sort((a, b) => b.averageRating - a.averageRating)
        .slice(0, 12);
        
    seedMovies.forEach(movie => {
        const card = document.createElement('div');
        card.className = 'onboarding-movie-card';
        card.setAttribute('data-id', movie.id);
        
        card.innerHTML = `
            <div class="onboarding-movie-poster">
                <img src="${movie.posterUrl}" alt="${movie.title}" onerror="this.src='https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500&q=80'">
            </div>
            <div class="onboarding-movie-info">
                <h4 class="onboarding-movie-title">${movie.title}</h4>
                <div class="onboarding-movie-meta">${movie.releaseYear} • ${movie.genres.slice(0, 2).join(', ')}</div>
                <div class="onboarding-stars" data-id="${movie.id}">
                    <i class="fa-regular fa-star" data-score="1"></i>
                    <i class="fa-regular fa-star" data-score="2"></i>
                    <i class="fa-regular fa-star" data-score="3"></i>
                    <i class="fa-regular fa-star" data-score="4"></i>
                    <i class="fa-regular fa-star" data-score="5"></i>
                </div>
            </div>
        `;
        
        // Star interactive events
        const stars = card.querySelectorAll('.onboarding-stars i');
        stars.forEach(star => {
            star.addEventListener('mouseover', (e) => {
                const score = parseInt(e.target.getAttribute('data-score'));
                highlightOnboardingStars(card, score, 'hovered');
            });
            
            star.addEventListener('mouseout', () => {
                clearOnboardingHover(card);
            });
            
            star.addEventListener('click', (e) => {
                const score = parseInt(e.target.getAttribute('data-score'));
                state.onboardingRatings[movie.id] = score;
                card.classList.add('rated');
                highlightOnboardingStars(card, score, 'selected');
                
                updateOnboardingProgressSubtitle();
            });
        });
        
        elements.onboardingMoviesGrid.appendChild(card);
    });
}

function highlightOnboardingStars(card, score, className) {
    const stars = card.querySelectorAll('.onboarding-stars i');
    stars.forEach(star => {
        const starScore = parseInt(star.getAttribute('data-score'));
        if (starScore <= score) {
            star.className = `fa-solid fa-star ${className}`;
        } else {
            star.className = 'fa-regular fa-star';
        }
    });
}

function clearOnboardingHover(card) {
    const stars = card.querySelectorAll('.onboarding-stars i');
    const movieId = card.getAttribute('data-id');
    const savedScore = state.onboardingRatings[movieId] || 0;
    
    stars.forEach(star => {
        const starScore = parseInt(star.getAttribute('data-score'));
        if (starScore <= savedScore) {
            star.className = 'fa-solid fa-star selected';
        } else {
            star.className = 'fa-regular fa-star';
        }
    });
}

function updateOnboardingProgressSubtitle() {
    if (!elements.onboardingRateSubtitle || !elements.btnOnboardingNext2) return;
    
    const ratedCount = Object.keys(state.onboardingRatings).length;
    elements.onboardingRateSubtitle.textContent = `Rate at least 5 movies/TV shows to initialize your AI recommendations: (${ratedCount}/5 rated)`;
    
    // Enable complete button if 5 or more rated
    elements.btnOnboardingNext2.disabled = ratedCount < 5;
}

function startTasteCalculationAnimation() {
    showOnboardingStep(3);
    
    if (!elements.onboardingProgressFill || !elements.onboardingLoaderStatus) return;
    
    let progress = 0;
    elements.onboardingProgressFill.style.width = '0%';
    
    const interval = setInterval(async () => {
        progress += 2;
        if (progress > 100) progress = 100;
        
        elements.onboardingProgressFill.style.width = `${progress}%`;
        
        if (progress < 30) {
            elements.onboardingLoaderStatus.textContent = "Analyzing genre overlaps and similarity scores...";
        } else if (progress < 60) {
            elements.onboardingLoaderStatus.textContent = "Mapping user taste coordinates to community vector spaces...";
        } else if (progress < 90) {
            elements.onboardingLoaderStatus.textContent = "Configuring recommendation queues...";
        } else if (progress === 100) {
            elements.onboardingLoaderStatus.textContent = "Done! Generating your personalized home feed.";
            clearInterval(interval);
            
            await submitOnboardingData();
        }
    }, 60); // Roughly 3 seconds total animation
}

async function submitOnboardingData() {
    if (!state.currentUser) return;
    
    const genresArray = Array.from(state.onboardingSelectedGenres);
    
    try {
        // 1. Submit selected genres preferences
        const prefResponse = await fetch(`${API_BASE}/users/${state.currentUser.id}/preferences`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(genresArray)
        });
        
        if (prefResponse.ok) {
            const updatedUser = await prefResponse.json();
            state.currentUser = updatedUser;
            localStorage.setItem('cinematch_user', JSON.stringify(updatedUser));
        }
        
        // 2. Submit all seeding movie ratings
        const ratingKey = `cinematch_ratings_${state.currentUser.id}`;
        let localRatings = JSON.parse(localStorage.getItem(ratingKey) || '{}');
        
        for (const [movieId, score] of Object.entries(state.onboardingRatings)) {
            await fetch(`${API_BASE}/movies/${movieId}/rate`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: state.currentUser.id,
                    score: score
                })
            });
            localRatings[movieId] = score;
        }
        localStorage.setItem(ratingKey, JSON.stringify(localRatings));
        
        // 3. Reload movie catalogs to sync rating averages
        await fetchMovies();
        
        // 4. Close wizard and refresh feed view
        updateUserUI();
        closeModal(elements.onboardingWizardModal);
        
        showToast('Onboarding completed! Welcome to CineMatch.', 'success');
        
        // Redirect to recommendations tab
        switchView('recommendations');
        
    } catch (e) {
        console.error(e);
        showToast('Error finalizing onboarding profile', 'error');
        closeModal(elements.onboardingWizardModal);
    }
}

// Password strength live validation
function validatePasswordRules(pwd) {
    const specialChars = "!@#$%^&*()-_=+[]{}|;:',.<>?/`~";
    
    const rules = {
        length: pwd.length >= 8,
        upper: /[A-Z]/.test(pwd),
        lower: /[a-z]/.test(pwd),
        digit: /[0-9]/.test(pwd),
        special: [...pwd].some(ch => specialChars.includes(ch))
    };

    for (const [ruleName, isValid] of Object.entries(rules)) {
        const element = document.getElementById(`rule-${ruleName}`);
        if (element) {
            const icon = element.querySelector('i');
            if (isValid) {
                element.classList.add('valid');
                if (icon) {
                    icon.className = 'fa-solid fa-circle-check';
                }
            } else {
                element.classList.remove('valid');
                if (icon) {
                    icon.className = 'fa-regular fa-circle';
                }
            }
        }
    }
}

// OAUTH FLOW & SETTINGS LOGIC

// Start OAuth authentication sequence
function startOAuthFlow(provider) {
    state.oauthProvider = provider;
    elements.loginPanel.style.display = 'none';
    elements.registerPanel.style.display = 'none';
    if (elements.oauthEmailPanel) elements.oauthEmailPanel.style.display = 'none';
    if (elements.otpPanel) elements.otpPanel.style.display = 'none';
    if (elements.googleChooserPanel) elements.googleChooserPanel.style.display = 'none';
    if (elements.appleChooserPanel) elements.appleChooserPanel.style.display = 'none';
    
    if (provider === 'google' && elements.googleChooserPanel) {
        elements.googleChooserPanel.style.display = 'block';
    } else if (provider === 'apple' && elements.appleChooserPanel) {
        elements.appleChooserPanel.style.display = 'block';
    } else {
        const titleEl = document.getElementById('oauth-email-title');
        if (titleEl) {
            titleEl.textContent = `Sign In with ${provider === 'google' ? 'Google' : 'Apple'}`;
        }
        if (elements.oauthEmailInput) {
            elements.oauthEmailInput.value = '';
        }
        if (elements.oauthEmailPanel) {
            elements.oauthEmailPanel.style.display = 'block';
        }
    }
}

// Common helper to initiate code dispatch and navigate to OTP panel
async function initiateOAuthCodeDispatch(email) {
    try {
        const response = await fetch(`${API_BASE}/auth/oauth/initiate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email })
        });
        
        if (response.ok) {
            const data = await response.json();
            state.oauthEmail = email;
            state.oauthOtp = data.simulatedToastOtp;
            
            // Show verification notice with simulated OTP
            showToast(`Verification code sent! MOCK CODE: ${data.simulatedToastOtp}`, 'success', 8000);
            
            // Render notice directly on OTP screen for user convenience
            const otpSubtitle = document.getElementById('otp-subtitle');
            if (otpSubtitle) {
                otpSubtitle.innerHTML = `We sent a code to <strong>${email}</strong>.<br><span style="color: var(--secondary); font-weight: bold;">Simulated Code: ${data.simulatedToastOtp}</span>`;
            }
            
            // Reset input
            elements.otpCode.value = '';
            
            // Hide choosers & email panel
            if (elements.googleChooserPanel) elements.googleChooserPanel.style.display = 'none';
            if (elements.appleChooserPanel) elements.appleChooserPanel.style.display = 'none';
            if (elements.oauthEmailPanel) elements.oauthEmailPanel.style.display = 'none';
            
            // Transition to OTP screen
            elements.otpPanel.style.display = 'block';
        } else {
            showToast('Failed to initiate login verification', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Authentication initialization failure', 'error');
    }
}

// Handle email form submit during simulated OAuth
async function handleOAuthEmailSubmit(e) {
    e.preventDefault();
    const email = elements.oauthEmailInput.value.trim();
    if (!email) return;
    
    state.oauthSource = 'email';
    await initiateOAuthCodeDispatch(email);
}

async function selectGoogleAccount(email, name, avatarClass, initials) {
    state.oauthProvider = 'google';
    state.oauthSource = 'chooser';
    await initiateOAuthCodeDispatch(email);
}

async function selectAppleAccount(email, name) {
    state.oauthProvider = 'apple';
    state.oauthSource = 'chooser';
    await initiateOAuthCodeDispatch(email);
}

function useAnotherGoogleAccount() {
    state.oauthProvider = 'google';
    state.oauthSource = 'chooser';
    if (elements.googleChooserPanel) elements.googleChooserPanel.style.display = 'none';
    
    const titleEl = document.getElementById('oauth-email-title');
    if (titleEl) {
        titleEl.textContent = 'Sign In with Google';
    }
    if (elements.oauthEmailInput) {
        elements.oauthEmailInput.value = '';
    }
    if (elements.oauthEmailPanel) {
        elements.oauthEmailPanel.style.display = 'block';
    }
}

function useAnotherAppleAccount() {
    state.oauthProvider = 'apple';
    state.oauthSource = 'chooser';
    if (elements.appleChooserPanel) elements.appleChooserPanel.style.display = 'none';
    
    const titleEl = document.getElementById('oauth-email-title');
    if (titleEl) {
        titleEl.textContent = 'Sign In with Apple';
    }
    if (elements.oauthEmailInput) {
        elements.oauthEmailInput.value = '';
    }
    if (elements.oauthEmailPanel) {
        elements.oauthEmailPanel.style.display = 'block';
    }
}

// Bind OAuth chooser actions globally for button click access
window.startOAuthFlow = startOAuthFlow;
window.selectGoogleAccount = selectGoogleAccount;
window.selectAppleAccount = selectAppleAccount;
window.useAnotherGoogleAccount = useAnotherGoogleAccount;
window.useAnotherAppleAccount = useAnotherAppleAccount;

// Handle OTP code verification submit
async function handleOtpSubmit(e) {
    e.preventDefault();
    const code = elements.otpCode.value.trim();
    const email = state.oauthEmail;
    
    if (!email || !code) return;
    
    try {
        // 1. Verify code on backend AuthController
        const response = await fetch(`${API_BASE}/auth/oauth/verify`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, code })
        });
        
        if (response.ok) {
            // 2. Complete OAuth registration/login on UserController
            const cleanUsername = email.split('@')[0] + '_' + state.oauthProvider;
            const oauthResponse = await fetch(`${API_BASE}/users/oauth`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: email,
                    username: cleanUsername,
                    provider: state.oauthProvider,
                    avatar: "fa-popcorn|avatar-grad-1"
                })
            });
            
            if (oauthResponse.ok) {
                const user = await oauthResponse.json();
                const isNewUser = (user.preferredGenres == null || user.preferredGenres.length === 0);
                
                state.currentUser = user;
                localStorage.setItem('cinematch_user', JSON.stringify(user));
                
                showToast(`Signed in successfully! Welcome, ${user.username}`, 'success');
                closeModal(elements.authModal);
                
                // Hide panels for future openings
                elements.otpPanel.style.display = 'none';
                elements.loginPanel.style.display = 'block';
                
                updateUserUI();
                await fetchWatchlist();
                
                // If new user, forward to onboarding wizard
                if (isNewUser) {
                    window.location.href = `onboarding.html?userId=${user.id}`;
                } else {
                    // Reload views
                    if (state.activeView === 'dashboard') {
                        renderDashboard();
                    } else if (state.activeView === 'recommendations') {
                        fetchRecommendations();
                    }
                }
            } else {
                const err = await oauthResponse.json();
                showToast(err.message || 'OAuth user registration failed', 'error');
            }
        } else {
            showToast('Incorrect verification code. Please try again.', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Verification submission failed', 'error');
    }
}

// Open settings profile modal and populate values
function openSettingsModal() {
    if (!state.currentUser) return;
    
    // Pre-populate fields
    elements.settingsUsername.value = state.currentUser.username || '';
    elements.settingsEmail.value = state.currentUser.email || '';
    
    // Setup active avatar icon and gradient
    const currentAvatar = state.currentUser.avatar || 'fa-popcorn|avatar-grad-1';
    const [iconClass, gradientClass] = currentAvatar.split('|');
    
    document.querySelectorAll('.avatar-icon-option').forEach(el => {
        if (el.getAttribute('data-icon') === iconClass) {
            el.classList.add('active');
        } else {
            el.classList.remove('active');
        }
    });
    
    document.querySelectorAll('.avatar-gradient-option').forEach(el => {
        if (el.getAttribute('data-grad') === gradientClass) {
            el.classList.add('active');
        } else {
            el.classList.remove('active');
        }
    });
    
    updateAvatarPreview();
    openModal(elements.settingsModal);
}

// Synchronize preview badge to selected selector classes
function updateAvatarPreview() {
    const activeIconEl = document.querySelector('.avatar-icon-option.active');
    const activeGradEl = document.querySelector('.avatar-gradient-option.active');
    
    if (activeIconEl && activeGradEl) {
        const iconClass = activeIconEl.getAttribute('data-icon');
        const gradClass = activeGradEl.getAttribute('data-grad');
        
        const previewBadge = elements.settingsAvatarPreview;
        if (previewBadge) {
            previewBadge.className = `avatar-preview-badge ${gradClass}`;
            previewBadge.innerHTML = `<i class="fa-solid ${iconClass}"></i>`;
        }
    }
}

// Submit profile updates to UserControllerSettings
async function handleSaveSettings(e) {
    e.preventDefault();
    if (!state.currentUser) return;
    
    const username = elements.settingsUsername.value.trim();
    const email = elements.settingsEmail.value.trim();
    
    const activeIconEl = document.querySelector('.avatar-icon-option.active');
    const activeGradEl = document.querySelector('.avatar-gradient-option.active');
    
    if (!username || !email || !activeIconEl || !activeGradEl) {
        showToast('Please fill out all settings fields', 'error');
        return;
    }
    
    const iconClass = activeIconEl.getAttribute('data-icon');
    const gradClass = activeGradEl.getAttribute('data-grad');
    const avatarString = `${iconClass}|${gradClass}`;
    
    try {
        const response = await fetch(`${API_BASE}/users/${state.currentUser.id}/settings`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username,
                email,
                avatar: avatarString
            })
        });
        
        if (response.ok) {
            const updatedUser = await response.json();
            
            // Sync session
            state.currentUser = updatedUser;
            localStorage.setItem('cinematch_user', JSON.stringify(updatedUser));
            
            showToast('Settings saved successfully!', 'success');
            closeModal(elements.settingsModal);
            updateUserUI();
            
            // Refresh dashboard reviews and state
            if (state.activeView === 'dashboard') {
                renderDashboard();
            }
        } else {
            const err = await response.json();
            showToast(err.message || 'Failed to save settings changes', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Server profile update error', 'error');
    }
}

// FAQ ACCORDION INTERACTIVE BEHAVIOR
function setupFaqAccordion() {
    const questions = document.querySelectorAll('.faq-question');
    questions.forEach(q => {
        q.addEventListener('click', () => {
            const item = q.parentElement;
            const isActive = item.classList.contains('active');
            
            // Collapse all other FAQ items
            document.querySelectorAll('.faq-item').forEach(el => el.classList.remove('active'));
            
            // Toggle clicked item
            if (!isActive) {
                item.classList.add('active');
            }
        });
    });
}

// CINEBOT AI AGENT ASSISTANT BEHAVIOR
function setupCineBot() {
    const btnToggle = document.getElementById('btn-cinebot-toggle');
    const btnClose = document.getElementById('btn-cinebot-close');
    const windowEl = document.getElementById('cinebot-window');
    const inputEl = document.getElementById('cinebot-input');
    const btnSend = document.getElementById('btn-cinebot-send');
    const messagesEl = document.getElementById('cinebot-messages');
    const chips = document.querySelectorAll('.cinebot-chip');

    if (!btnToggle || !windowEl) return;

    // Toggle Chat Window
    btnToggle.addEventListener('click', () => {
        windowEl.classList.toggle('active');
        if (windowEl.classList.contains('active')) {
            inputEl.focus();
            scrollToBottom();
        }
    });

    // Close Chat Window
    if (btnClose) {
        btnClose.addEventListener('click', () => {
            windowEl.classList.remove('active');
        });
    }

    // Send Message on click
    if (btnSend) {
        btnSend.addEventListener('click', handleSend);
    }

    // Send Message on Enter
    if (inputEl) {
        inputEl.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleSend();
            }
        });
    }

    // Suggestion chips
    chips.forEach(chip => {
        chip.addEventListener('click', () => {
            const text = chip.getAttribute('data-msg');
            if (text) {
                appendMessage(text, 'user');
                sendMessageToApi(text);
            }
        });
    });

    function handleSend() {
        const text = inputEl.value.trim();
        if (!text) return;
        
        inputEl.value = '';
        appendMessage(text, 'user');
        sendMessageToApi(text);
    }

    function appendMessage(text, sender) {
        const msgDiv = document.createElement('div');
        msgDiv.className = `cinebot-msg ${sender}`;
        
        const bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'cinebot-bubble';
        bubbleDiv.innerHTML = text;
        
        msgDiv.appendChild(bubbleDiv);
        messagesEl.appendChild(msgDiv);
        scrollToBottom();
    }

    function appendTypingIndicator() {
        const indicator = document.createElement('div');
        indicator.className = 'cinebot-msg bot typing-indicator-msg';
        indicator.id = 'cinebot-typing-indicator';
        
        const bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'cinebot-bubble';
        bubbleDiv.style.display = 'flex';
        bubbleDiv.style.gap = '4px';
        bubbleDiv.style.alignItems = 'center';
        bubbleDiv.style.padding = '10px 14px';
        bubbleDiv.innerHTML = '<span class="typing-dot" style="width: 5px; height: 5px; border-radius: 50%; background: var(--text-muted); animation: typing-glow 1s infinite alternate 0.1s;"></span>' +
                              '<span class="typing-dot" style="width: 5px; height: 5px; border-radius: 50%; background: var(--text-muted); animation: typing-glow 1s infinite alternate 0.2s;"></span>' +
                              '<span class="typing-dot" style="width: 5px; height: 5px; border-radius: 50%; background: var(--text-muted); animation: typing-glow 1s infinite alternate 0.3s;"></span>';
        
        indicator.appendChild(bubbleDiv);
        messagesEl.appendChild(indicator);
        scrollToBottom();
    }

    function removeTypingIndicator() {
        const indicator = document.getElementById('cinebot-typing-indicator');
        if (indicator) {
            indicator.remove();
        }
    }

    function scrollToBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    async function sendMessageToApi(messageText) {
        appendTypingIndicator();
        
        try {
            const response = await fetch(`${API_BASE}/chat`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: messageText })
            });

            removeTypingIndicator();

            if (response.ok) {
                const data = await response.json();
                
                let botReply = data.reply;
                appendMessage(botReply, 'bot');

                if (data.suggestedMovies && data.suggestedMovies.length > 0) {
                    data.suggestedMovies.forEach(movie => {
                        const card = document.createElement('div');
                        card.className = 'cinebot-movie-card-inline';
                        card.onclick = () => {
                            windowEl.classList.remove('active');
                            showMovieDetails(movie);
                        };

                        const imgUrl = movie.posterUrl || 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=200&q=80';
                        card.innerHTML = `
                            <img src="${imgUrl}" alt="${movie.title}">
                            <div class="cinebot-movie-card-inline-info">
                                <span class="cinebot-movie-card-inline-title">${movie.title}</span>
                                <span class="cinebot-movie-card-inline-meta">${movie.releaseYear} • ⭐ ${movie.imdbRating || movie.averageRating || 'N/A'}</span>
                            </div>
                        `;
                        messagesEl.appendChild(card);
                    });
                    scrollToBottom();
                }
            } else {
                appendFallbackReply(messageText);
            }
        } catch (err) {
            console.error("Chat API error:", err);
            removeTypingIndicator();
            appendFallbackReply(messageText);
        }
    }

    function appendFallbackReply(userMessage) {
        const lower = userMessage.toLowerCase().trim();
        let reply = "";
        let matchedMovies = [];

        if (lower.match(/\b(hi|hello|hey|yo)\b/)) {
            reply = "Hi! I'm CineBot, your offline-mode assistant. Ask me about Sci-Fi, Action, Drama, Comedy, or check what's trending!";
        } else if (lower.includes("trending") || lower.includes("popular")) {
            reply = "Here are a few popular selections from the database:";
            matchedMovies = state.moviesList.slice(0, 3);
        } else if (lower.includes("sci-fi") || lower.includes("science fiction")) {
            reply = "Here are some of our top Sci-Fi recommendations:";
            matchedMovies = state.moviesList.filter(m => m.genres && m.genres.some(g => g.toLowerCase().includes("sci"))).slice(0, 3);
        } else if (lower.includes("action")) {
            reply = "Here are some action-packed favorites:";
            matchedMovies = state.moviesList.filter(m => m.genres && m.genres.some(g => g.toLowerCase().includes("act"))).slice(0, 3);
        } else {
            reply = "CineBot is in local fallback mode. Try checking out our home dashboard recommendations!";
            matchedMovies = state.moviesList.slice(0, 2);
        }

        appendMessage(reply, 'bot');
        if (matchedMovies.length > 0) {
            matchedMovies.forEach(movie => {
                const card = document.createElement('div');
                card.className = 'cinebot-movie-card-inline';
                card.onclick = () => {
                    windowEl.classList.remove('active');
                    showMovieDetails(movie);
                };
                const imgUrl = movie.posterUrl || 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=200&q=80';
                card.innerHTML = `
                    <img src="${imgUrl}" alt="${movie.title}">
                    <div class="cinebot-movie-card-inline-info">
                        <span class="cinebot-movie-card-inline-title">${movie.title}</span>
                        <span class="cinebot-movie-card-inline-meta">${movie.releaseYear} • ⭐ ${movie.imdbRating || movie.averageRating || 'N/A'}</span>
                    </div>
                `;
                messagesEl.appendChild(card);
            });
            scrollToBottom();
        }
    }
}

