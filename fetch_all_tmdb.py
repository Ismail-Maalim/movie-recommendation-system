import urllib.request
import urllib.parse
import re
import ssl
import json
import time

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

movies_to_fetch = [
    {"variable": "inception", "title": "Inception", "year": 2010, "is_tv": False},
    {"variable": "interstellar", "title": "Interstellar", "year": 2014, "is_tv": False},
    {"variable": "darkKnight", "title": "The Dark Knight", "year": 2008, "is_tv": False},
    {"variable": "pulpFiction", "title": "Pulp Fiction", "year": 1994, "is_tv": False},
    {"variable": "matrix", "title": "The Matrix", "year": 1999, "is_tv": False},
    {"variable": "avatar", "title": "Avatar", "year": 2009, "is_tv": False},
    {"variable": "titanic", "title": "Titanic", "year": 1997, "is_tv": False},
    {"variable": "spiritedAway", "title": "Spirited Away", "year": 2001, "is_tv": False},
    {"variable": "godfather", "title": "The Godfather", "year": 1972, "is_tv": False},
    {"variable": "laLaLand", "title": "La La Land", "year": 2016, "is_tv": False},
    {"variable": "parasite", "title": "Parasite", "year": 2019, "is_tv": False},
    {"variable": "knivesOut", "title": "Knives Out", "year": 2019, "is_tv": False},
    {"variable": "gladiator", "title": "Gladiator", "year": 2000, "is_tv": False},
    {"variable": "avengersEndgame", "title": "Avengers: Endgame", "year": 2019, "is_tv": False},
    {"variable": "duneI", "title": "Dune I", "search_title": "Dune", "year": 2021, "is_tv": False},
    {"variable": "duneII", "title": "Dune II", "search_title": "Dune: Part Two", "year": 2024, "is_tv": False},
    {"variable": "duneIII", "title": "Dune III", "search_title": "Dune: Part Three", "fallback_title": "Dune: Part Two", "year": 2026, "is_tv": False},
    {"variable": "prometheus", "title": "Prometheus", "year": 2012, "is_tv": False},
    {"variable": "blacklist", "title": "The Blacklist", "year": 2013, "is_tv": True},
    {"variable": "personOfInterest", "title": "Person of Interest", "year": 2011, "is_tv": True},
    {"variable": "moneyHeist", "title": "Money Heist", "year": 2017, "is_tv": True},
    {"variable": "supacell", "title": "Supacell", "year": 2024, "is_tv": True},
    {"variable": "from", "title": "From", "year": 2022, "is_tv": True},
    {"variable": "apex", "title": "Apex", "year": 2021, "is_tv": False},
    {"variable": "orangeIsNewBlack", "title": "Orange Is the New Black", "year": 2013, "is_tv": True},
    {"variable": "untouchable", "title": "Untouchable", "search_title": "The Intouchables", "year": 2011, "is_tv": False},
    {"variable": "atlas", "title": "Atlas", "year": 2024, "is_tv": False},
    {"variable": "theCore", "title": "The Core", "year": 2003, "is_tv": False},
    {"variable": "ghostRider", "title": "Ghost Rider", "year": 2007, "is_tv": False},
    {"variable": "pandora", "title": "Pandora", "year": 2016, "is_tv": False},
    {"variable": "horizonLine", "title": "Horizon Line", "year": 2020, "is_tv": False},
    {"variable": "strangerThings", "title": "Stranger Things", "year": 2016, "is_tv": True},
    {"variable": "threeSixtyFiveDays", "title": "365 Days", "year": 2020, "is_tv": False},
    {"variable": "nowhere", "title": "Nowhere", "year": 2023, "is_tv": False},
    {"variable": "furiosa", "title": "Furiosa: A Mad Max Saga", "year": 2024, "is_tv": False},
    {"variable": "angelEyes", "title": "Angel Eyes", "year": 2001, "is_tv": False},
    {"variable": "rampage", "title": "Rampage", "year": 2018, "is_tv": False},
    {"variable": "legends", "title": "Legends", "search_title": "I Am Legend", "year": 2007, "is_tv": False},
    {"variable": "snowpiercer", "title": "Snowpiercer", "year": 2013, "is_tv": False},
    {"variable": "damsel", "title": "Damsel", "year": 2024, "is_tv": False},
    {"variable": "sisu", "title": "Sisu", "year": 2022, "is_tv": False},
    {"variable": "carryOn", "title": "Carry-On", "search_title": "Carry-On", "year": 2024, "is_tv": False},
    {"variable": "abigail", "title": "Abigail", "year": 2024, "is_tv": False},
    {"variable": "theGreatFlood", "title": "The Great Flood", "search_title": "Great Flood", "year": 2024, "is_tv": False},
    {"variable": "sixUnderground", "title": "6 Underground", "year": 2019, "is_tv": False}
]

def search_tmdb(title, year=None, is_tv=False, search_title=None, fallback_title=None):
    query_title = search_title if search_title else title
    query = query_title
    if year and not is_tv:
        query = f"{query_title} y:{year}"
    
    url = f"https://www.themoviedb.org/search?query={urllib.parse.quote(query)}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx) as response:
            html = response.read().decode('utf-8')
            pattern = r'href="/(movie|tv)/(\d+)-[^"]*"'
            matches = re.findall(pattern, html)
            
            # If no matches, try searching without the year filter
            if not matches:
                url = f"https://www.themoviedb.org/search?query={urllib.parse.quote(query_title)}"
                req = urllib.request.Request(url, headers=headers)
                with urllib.request.urlopen(req, context=ctx) as response2:
                    html = response2.read().decode('utf-8')
                    matches = re.findall(pattern, html)
            
            # If still no matches and a fallback title is specified, search for fallback title
            if not matches and fallback_title:
                url = f"https://www.themoviedb.org/search?query={urllib.parse.quote(fallback_title)}"
                req = urllib.request.Request(url, headers=headers)
                with urllib.request.urlopen(req, context=ctx) as response3:
                    html = response3.read().decode('utf-8')
                    matches = re.findall(pattern, html)

            if matches:
                target_type = 'tv' if is_tv else 'movie'
                best_match = None
                for m_type, m_id in matches:
                    if m_type == target_type:
                        best_match = (m_type, m_id)
                        break
                if not best_match:
                    best_match = matches[0]
                return best_match
    except Exception as e:
        print(f"Error searching for {title}: {e}")
    return None

def get_images(media_type, media_id):
    url = f"https://www.themoviedb.org/{media_type}/{media_id}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx) as response:
            html = response.read().decode('utf-8')
            
            # og:image (usually the poster)
            poster_match = re.search(r'property="og:image"\s+content="([^"]+)"', html)
            
            # backdrop image
            # Look for multiple types of backdrop representations
            backdrop_match = re.search(r"background-image:\s*url\('([^']+)'\)", html)
            if not backdrop_match:
                # Fallback search for other backdrop images or images on the page
                backdrop_match = re.search(r'src="([^"]+w1920[^"]+)"', html)
            if not backdrop_match:
                backdrop_match = re.search(r'srcset="([^"]+w1000_and_h450[^"]+)"', html)
            
            poster = poster_match.group(1) if poster_match else None
            backdrop = backdrop_match.group(1) if backdrop_match else None
            
            if poster:
                poster = poster.replace("https://media.themoviedb.org", "https://image.tmdb.org")
            if backdrop:
                backdrop = backdrop.split(' ')[0] # if srcset
                backdrop = backdrop.replace("https://media.themoviedb.org", "https://image.tmdb.org")
                # Normalize size to w1280 or original
                if "_multi_faces" in backdrop:
                    # e.g., /t/p/w1920_and_h800_multi_faces/
                    pass
                else:
                    # replace the size indicator with w1280
                    backdrop = re.sub(r'/t/p/w\d+/', '/t/p/w1280/', backdrop)
            
            # If no backdrop, use poster as a fallback or vice-versa
            if poster and not backdrop:
                backdrop = poster
            if backdrop and not poster:
                poster = backdrop
                
            return poster, backdrop
    except Exception as e:
        print(f"Error fetching images for {media_type}/{media_id}: {e}")
    return None, None

results = {}
for item in movies_to_fetch:
    title = item["title"]
    year = item.get("year")
    is_tv = item.get("is_tv", False)
    search_title = item.get("search_title")
    fallback_title = item.get("fallback_title")
    
    print(f"Processing: {title}...", end="", flush=True)
    match = search_tmdb(title, year, is_tv, search_title, fallback_title)
    if match:
        m_type, m_id = match
        poster, backdrop = get_images(m_type, m_id)
        results[item["variable"]] = {
            "title": title,
            "poster": poster,
            "backdrop": backdrop
        }
        print(f" Found {m_type}/{m_id}. Poster: {poster}")
    else:
        print(" NOT FOUND!")
    
    time.sleep(0.5) # Be nice to TMDB

with open(r"C:\Users\zuery\Documents\antigravity\calm-pascal\tmdb_results.json", "w") as f:
    json.dump(results, f, indent=4)

print("Done! Saved to tmdb_results.json")
