// CineMatch Onboarding Script
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

const state = {
    currentUser: null,
    selectedGenres: new Set(),
    ratings: {},
    moviesList: []
};

const elements = {
    toastContainer: document.getElementById('toast-container'),
    genresGrid: document.getElementById('onboarding-genres-grid'),
    btnNext1: document.getElementById('btn-onboarding-next-1'),
    btnPrev2: document.getElementById('btn-onboarding-prev-2'),
    btnNext2: document.getElementById('btn-onboarding-next-2'),
    moviesGrid: document.getElementById('onboarding-movies-grid'),
    rateSubtitle: document.getElementById('onboarding-rate-subtitle'),
    loaderStatus: document.getElementById('onboarding-loader-status'),
    progressFill: document.getElementById('onboarding-progress-fill'),
    panel1: document.getElementById('onboarding-step-panel-1'),
    panel2: document.getElementById('onboarding-step-panel-2'),
    panel3: document.getElementById('onboarding-step-panel-3'),
};

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-triangle-exclamation';
    
    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    elements.toastContainer.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s reverse forwards';
        toast.addEventListener('animationend', () => {
            toast.remove();
        });
    }, 3000);
}

// Check auth and init
document.addEventListener('DOMContentLoaded', async () => {
    await init();
});

async function init() {
    // 1. Identify User
    const urlParams = new URLSearchParams(window.location.search);
    const userIdParam = urlParams.get('userId');
    const savedUserStr = localStorage.getItem('cinematch_user');
    
    let user = null;
    if (savedUserStr) {
        user = JSON.parse(savedUserStr);
    }
    
    // If URL contains user ID, try fetching it to make sure it is correct/sync'd
    if (userIdParam) {
        try {
            const res = await fetch(`${API_BASE}/users/${userIdParam}`);
            if (res.ok) {
                user = await res.json();
                localStorage.setItem('cinematch_user', JSON.stringify(user));
            }
        } catch (e) {
            console.error('Failed to fetch user from param:', e);
        }
    }
    
    if (!user) {
        showToast('Please sign in or create an account to start onboarding!', 'error');
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 2000);
        return;
    }
    
    state.currentUser = user;
    
    // 2. Setup Step 1 Genre Listeners
    setupGenreSelection();
    
    // 3. Fetch Movies Catalog
    await fetchMovies();
}

async function fetchMovies() {
    try {
        const res = await fetch(`${API_BASE}/movies`);
        if (res.ok) {
            state.moviesList = await res.json();
        } else {
            showToast('Failed to load movie options', 'error');
        }
    } catch (e) {
        console.error(e);
        showToast('Network error loading movies', 'error');
    }
}

function showStep(stepNum) {
    elements.panel1.classList.remove('active');
    elements.panel2.classList.remove('active');
    elements.panel3.classList.remove('active');
    
    const targetPanel = document.getElementById(`onboarding-step-panel-${stepNum}`);
    if (targetPanel) {
        targetPanel.classList.add('active');
    }
    
    // Update progress steps
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

function setupGenreSelection() {
    if (!elements.genresGrid) return;
    
    const buttons = elements.genresGrid.querySelectorAll('.pref-checkbox-btn');
    buttons.forEach(btn => {
        btn.addEventListener('click', () => {
            const genre = btn.getAttribute('data-genre');
            if (state.selectedGenres.has(genre)) {
                state.selectedGenres.delete(genre);
                btn.classList.remove('selected');
            } else {
                state.selectedGenres.add(genre);
                btn.classList.add('selected');
            }
            
            elements.btnNext1.disabled = state.selectedGenres.size < 3;
        });
    });
    
    elements.btnNext1.addEventListener('click', () => {
        renderMovies();
        showStep(2);
    });
    
    elements.btnPrev2.addEventListener('click', () => {
        showStep(1);
    });
    
    elements.btnNext2.addEventListener('click', () => {
        startAnalysis();
    });
}

function renderMovies() {
    if (!elements.moviesGrid || state.moviesList.length === 0) return;
    
    elements.moviesGrid.innerHTML = '';
    
    // Filter movies matching preferred genres if possible, or just top-rated overall
    const genresArr = Array.from(state.selectedGenres).map(g => g.toLowerCase());
    
    // Sort movies by priority: matches preferred genres first, then sorted by averageRating
    const sortedMovies = [...state.moviesList].sort((a, b) => {
        const aMatches = a.genres.some(g => genresArr.includes(g.toLowerCase())) ? 1 : 0;
        const bMatches = b.genres.some(g => genresArr.includes(g.toLowerCase())) ? 1 : 0;
        
        if (aMatches !== bMatches) {
            return bMatches - aMatches; // matching ones first
        }
        return b.averageRating - a.averageRating;
    });
    
    const seedMovies = sortedMovies.slice(0, 12);
    
    seedMovies.forEach(movie => {
        const card = document.createElement('div');
        card.className = 'onboarding-movie-card';
        card.setAttribute('data-id', movie.id);
        
        card.innerHTML = `
            <img class="onboarding-movie-poster" src="${movie.posterUrl}" alt="${movie.title}" onerror="this.src='https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500&q=80'">
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
        
        const stars = card.querySelectorAll('.onboarding-stars i');
        stars.forEach(star => {
            star.addEventListener('mouseover', (e) => {
                const score = parseInt(e.target.getAttribute('data-score'));
                highlightStars(card, score, 'hovered');
            });
            
            star.addEventListener('mouseout', () => {
                clearHover(card);
            });
            
            star.addEventListener('click', (e) => {
                const score = parseInt(e.target.getAttribute('data-score'));
                state.ratings[movie.id] = score;
                card.classList.add('rated');
                highlightStars(card, score, 'selected');
                updateProgressSubtitle();
            });
        });
        
        elements.moviesGrid.appendChild(card);
    });
}

function highlightStars(card, score, className) {
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

function clearHover(card) {
    const stars = card.querySelectorAll('.onboarding-stars i');
    const movieId = card.getAttribute('data-id');
    const savedScore = state.ratings[movieId] || 0;
    
    stars.forEach(star => {
        const starScore = parseInt(star.getAttribute('data-score'));
        if (starScore <= savedScore) {
            star.className = 'fa-solid fa-star selected';
        } else {
            star.className = 'fa-regular fa-star';
        }
    });
}

function updateProgressSubtitle() {
    const ratedCount = Object.keys(state.ratings).length;
    elements.rateSubtitle.textContent = `Rate at least 5 movies/TV shows to initialize your AI recommendations: (${ratedCount}/5 rated)`;
    elements.btnNext2.disabled = ratedCount < 5;
}

function startAnalysis() {
    showStep(3);
    
    let progress = 0;
    elements.progressFill.style.width = '0%';
    
    const interval = setInterval(async () => {
        progress += 2;
        if (progress > 100) progress = 100;
        
        elements.progressFill.style.width = `${progress}%`;
        
        if (progress < 30) {
            elements.loaderStatus.textContent = "Analyzing genre overlaps and similarity scores...";
        } else if (progress < 60) {
            elements.loaderStatus.textContent = "Mapping user taste coordinates to community vector spaces...";
        } else if (progress < 90) {
            elements.loaderStatus.textContent = "Configuring recommendation queues...";
        } else if (progress === 100) {
            elements.loaderStatus.textContent = "Done! Generating your personalized home feed.";
            clearInterval(interval);
            
            await submitOnboarding();
        }
    }, 60);
}

async function submitOnboarding() {
    try {
        const payload = {
            userId: state.currentUser.id,
            preferredGenres: Array.from(state.selectedGenres),
            ratings: state.ratings
        };
        
        const response = await fetch(`${API_BASE}/users/onboarding`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (response.ok) {
            const updatedUser = await response.json();
            // Update current user locally
            localStorage.setItem('cinematch_user', JSON.stringify(updatedUser));
            
            // Store ratings locally for the UI to display user rating scores correctly
            const ratingKey = `cinematch_ratings_${updatedUser.id}`;
            localStorage.setItem(ratingKey, JSON.stringify(state.ratings));
            
            showToast('Profile customizer successfully completed!', 'success');
            
            setTimeout(() => {
                // Redirect back to main page and switch to recommendations
                window.location.href = 'index.html?view=recommendations';
            }, 1000);
        } else {
            const err = await response.json();
            showToast(err.message || 'Failed to submit onboarding data', 'error');
            setTimeout(() => showStep(2), 2000);
        }
    } catch (e) {
        console.error(e);
        showToast('Server communication failure', 'error');
        setTimeout(() => showStep(2), 2000);
    }
}
