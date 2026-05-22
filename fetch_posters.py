import urllib.request
import urllib.parse
import re
import ssl
import json
import time

# Disable SSL verification for ease
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

def search_tmdb(title, year=None, is_tv=False):
    query = title
    if year and not is_tv:
        query = f"{title} y:{year}"
    
    url = f"https://www.themoviedb.org/search?query={urllib.parse.quote(query)}"
    print(f"Searching: {url}")
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx) as response:
            html = response.read().decode('utf-8')
            
            # Let's find links to movies or tv shows
            # e.g., href="/movie/27205-inception" or href="/tv/66732-stranger-things"
            pattern = r'href="/(movie|tv)/(\d+)-[^"]*"'
            matches = re.findall(pattern, html)
            if not matches:
                # Let's try searching without year
                url = f"https://www.themoviedb.org/search?query={urllib.parse.quote(title)}"
                req = urllib.request.Request(url, headers=headers)
                with urllib.request.urlopen(req, context=ctx) as response2:
                    html = response2.read().decode('utf-8')
                    matches = re.findall(pattern, html)
            
            if matches:
                # Find the first match that corresponds to the requested type (movie or tv)
                target_type = 'tv' if is_tv else 'movie'
                best_match = None
                for m_type, m_id in matches:
                    if m_type == target_type:
                        best_match = (m_type, m_id)
                        break
                if not best_match:
                    best_match = matches[0] # Fallback to first match
                
                return best_match
    except Exception as e:
        print(f"Error searching for {title}: {e}")
    return None

def get_movie_images(media_type, media_id):
    url = f"https://www.themoviedb.org/{media_type}/{media_id}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx) as response:
            html = response.read().decode('utf-8')
            
            # Extract og:image
            # e.g. <meta property="og:image" content="https://media.themoviedb.org/t/p/w500/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg">
            poster_match = re.search(r'property="og:image"\s+content="([^"]+)"', html)
            
            # Extract backdrop image
            # e.g. background-image: url('https://media.themoviedb.org/t/p/w1920_and_h800_multi_faces/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg');
            backdrop_match = re.search(r"background-image:\s*url\('([^']+)'\)", html)
            
            poster = poster_match.group(1) if poster_match else None
            backdrop = backdrop_match.group(1) if backdrop_match else None
            
            # Clean paths to match the format
            if poster:
                poster = poster.replace("https://media.themoviedb.org", "https://image.tmdb.org")
            if backdrop:
                backdrop = backdrop.replace("https://media.themoviedb.org", "https://image.tmdb.org")
                
            return poster, backdrop
    except Exception as e:
        print(f"Error fetching images for {media_type}/{media_id}: {e}")
    return None, None

# Test with Inception
m_type, m_id = search_tmdb("Inception", 2010)
print(f"Match found: {m_type}/{m_id}")
if m_id:
    poster, backdrop = get_movie_images(m_type, m_id)
    print(f"Poster: {poster}")
    print(f"Backdrop: {backdrop}")
