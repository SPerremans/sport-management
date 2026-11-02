"""Small, slow captures of public league data from toernooi.nl into timestamped local files.

Writes to a scrapes folder next to this script: scrapes/<league>/<sport>-<country>-<timestamp>/ per league, and
scrapes/rankings/<sport>-<country>-<timestamp>/ for the national ranking. Several runs may work at once: they take turns,
so together they keep the pace of one run, and a refusal of the site stops all of them.
Public browsing with necessary cookies only, no sign-in. robots.txt of the site disallows these paths and its terms
speak against excessive automated use, so every run is filtered, throttled, capped and stops on the first refusal.
Youth events and youth ranking entries are never stored. Member numbers, birth dates and contacts are never stored.
Never invents data: what the source does not publish stays empty, and what cannot be read becomes a manifest warning.

Usage: see docs/development/toernooi-scraper.md
"""
from bs4 import BeautifulSoup
from contextlib import contextmanager
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from urllib.parse import parse_qs, urlencode, urlparse
from zoneinfo import ZoneInfo
import argparse, hashlib, http.cookiejar, json, os, random, re, sys, time, urllib.error, urllib.request

SCRAPES = Path(__file__).resolve().parent / "scrapes"
CACHE = SCRAPES / "_cache"
BASE = "https://www.toernooi.nl"
BRUSSELS = ZoneInfo("Europe/Brussels")
USER_AGENT = "Mozilla/5.0 (local development sample capture)"
MIN_DELAY = 3.0
COOLDOWN_SECONDS = 3600
RANKINGS_FOLDER = "rankings"
SCHEMA_VERSION = 1

# site: the id of the sport selection of the site. games: the labels of the games on a team match sheet.
SPORTS = {"badminton": dict(site=2, games=r"(?:HD|DD|GD|HE|DE)\d*")}
# The national ranking per sport and country, as the rid of /ranking/ranking.aspx. Check a new one by its club names.
RANKINGS = {("badminton", "BEL"): 334}
YOUTH = re.compile(r"(?i)jeugd|jeunes|youth|junior|ipjo|\bU-?\d{1,2}\b")
U19_AGE_GROUP = 9
ENTITIES = ["clubs", "players", "rankings", "matches"]


class StopScrape(Exception):
    """The run ends here and keeps what it has: a refusal of the site or the request cap."""


def soup(html):
    return BeautifulSoup(html, "html.parser")

def text(node):
    return node.get_text(" ", strip=True) if node else ""

def query(href, param):
    values = {k.lower(): v for k, v in parse_qs(urlparse(href).query).items()}
    return values[param.lower()][0] if param.lower() in values else None

def number(raw):
    return int(raw) if re.fullmatch(r"\d+", (raw or "").strip()) else None

def rows(table):
    return table.select(":scope > tbody > tr") or table.find_all("tr", recursive=False)

def name_and_level(raw):
    """'Nico Van Vliet (7)' is a name and the level printed at that match."""
    m = re.search(r"\((\d{1,2})\)\s*$", raw)
    return re.sub(r"\s*\(\d{1,2}\)\s*$", "", raw).strip(), int(m[1]) if m else None

def display_name(raw):
    """The player list prints 'Vliet, Nico'; the match sheets print 'Nico Vliet'."""
    last, _, first = raw.partition(",")
    return f"{first.strip()} {last.strip()}" if first.strip() else raw.strip()

def planned(raw):
    """A date without a published time stays a date, never a fabricated midnight."""
    m = re.search(r"(\d{1,2}-\d{1,2}-\d{4})(?:\s+(\d\d:\d\d))?", raw)
    if not m:
        return None, None
    local = datetime.strptime(m[1] + " " + (m[2] or "00:00"), "%d-%m-%Y %H:%M")
    return local.date().isoformat(), local.replace(tzinfo=BRUSSELS).isoformat() if m[2] else None


# Parsers: page text in, plain data out, no requests.

def parse_league_search(html):
    leagues = {}
    for item in soup(html).select("li.list__item"):
        link = item.select_one("a.media__link")
        if not link:
            continue
        times = [t.get("datetime", "")[:10] for t in item.select("time")]
        organizer, _, place = text(item.select_one(".media__subheading")).partition("|")
        ident = query(link["href"], "id")
        leagues[ident.upper()] = dict(id=ident.upper(), name=link.get("title") or text(link), organizer=organizer.strip(), place=place.strip(),
                                      startsOn=times[0] if times else None, endsOn=times[1] if len(times) > 1 else None,
                                      url=f"{BASE}/sport/league?id={ident}")
    return list(leagues.values())

def parse_clubs(html):
    clubs = []
    for row in soup(html).select("table.ruler tr"):
        cells = row.find_all("td", recursive=False)
        link = cells[0].select_one('a[href*="club.aspx?"]') if cells else None
        if not link:
            continue
        counts = [number(text(c)) for c in cells[1:6]] + [None] * 5
        place = next((text(c) for c in cells[6:] if text(c)), "")
        clubs.append(dict(id=query(link["href"], "club"), name=text(link), number=counts[0], teamCount=counts[1], playerCount=counts[2],
                          menCount=counts[3], womenCount=counts[4], place=place))
    return clubs

def parse_club_teams(html):
    teams = {}
    for link in soup(html).select('a[href*="team.aspx?"]'):
        ident = query(link["href"], "team")
        if ident:
            teams[ident] = dict(id=ident, name=text(link))
    return list(teams.values())

def parse_draws(html):
    draws = {}
    for link in soup(html).select('a[href*="draw.aspx?"]'):
        ident = query(link["href"], "draw")
        if ident and text(link):
            draws.setdefault(ident, dict(id=ident, name=text(link)))
    return list(draws.values())

def parse_draw_teams(html):
    """Team name to team id, from the standing of a draw."""
    members = {}
    for table in soup(html).select("table.ruler:not(.matches)"):
        for link in table.select('a[href*="team.aspx?"]'):
            if query(link["href"], "team"):
                members[text(link)] = query(link["href"], "team")
    return members

def parse_draw_matches(html):
    fixtures = []
    for table in soup(html).select("table.matches"):
        for row in rows(table):
            sides = row.select("a.teamname")
            if len(sides) != 2:
                continue
            cells = row.find_all("td", recursive=False)
            day, starts = planned(text(row.select_one(".plannedtime")))
            home_at = next(n for n, c in enumerate(cells) if sides[0] in c.descendants)
            away_at = next(n for n, c in enumerate(cells) if sides[1] in c.descendants)
            score_at = next((n for n, c in enumerate(cells) if c.select_one(".score")), None)
            venue_at = (score_at if score_at is not None else away_at + 1) + 1
            fixtures.append(dict(id=query(sides[0]["href"], "match"), home=text(sides[0]), away=text(sides[1]), date=day, startsAt=starts,
                                 round=" / ".join(text(c) for c in cells[2:home_at] if text(c)),
                                 venue=text(cells[venue_at]) if venue_at < len(cells) else "",
                                 sourceScore=text(row.select_one(".score")),
                                 sourceStatus="; ".join(i.get("title", "") for i in row.select("img[title]"))))
    return fixtures

def outcome_of(raw, home_cell, away_cell):
    if "Opgave Team 1" in raw:
        return "HomeRetired"
    if "Opgave Team 2" in raw:
        return "AwayRetired"
    if "geen speler van Team 1" in raw:
        return "HomeForfeit"
    if "geen speler van Team 2" in raw:
        return "AwayForfeit"
    if "geen spelers" in raw:
        return "NotPlayed"
    if "Opgave" in raw:
        # Only infer the retiring side when the source explicitly highlights the winner.
        return "AwayRetired" if home_cell.select_one("strong") else "HomeRetired" if away_cell.select_one("strong") else "Pending"
    return "Played" if re.search(r"\d+\s*-\s*\d+", raw) else "Pending"

def sheet_side(cell):
    side = []
    for link in cell.select('a[href*="player="]'):
        name, level = name_and_level(text(link))
        side.append(dict(sourceId=query(link["href"], "player"), name=name, level=level))
    return side

def parse_match_sheet(html, game_labels):
    """The games and the published facts of a team match sheet. A sheet without games gives an empty list."""
    page = soup(html)
    games = []
    table = page.select_one("table.matches")
    for row in rows(table) if table else []:
        cells = row.find_all("td", recursive=False)
        at = next((n for n, c in enumerate(cells) if re.fullmatch(game_labels, text(c))), None)
        if at is None or len(cells) < at + 5:
            continue
        home, away, raw = cells[at + 1], cells[at + 3], text(cells[at + 4])
        won_home, won_away = home.select_one("strong") is not None, away.select_one("strong") is not None
        games.append(dict(name=text(cells[at]), discipline=re.sub(r"\d", "", text(cells[at])), home=sheet_side(home), away=sheet_side(away),
                          sets=[[int(a), int(b)] for a, b in re.findall(r"(\d+)\s*-\s*(\d+)", raw)], sourceScore=raw,
                          outcome=outcome_of(raw, home, away), winner="home" if won_home and not won_away else "away" if won_away and not won_home else None))
    info = {}
    labels = {"Wedstrijdleider:": "leader", "Gebruikte shuttle:": "shuttle", "Aanvangsuur:": "startedAt", "Einduur:": "endedAt"}
    for th in page.select("th"):
        value = text(th.find_next_sibling("td"))
        if text(th) in labels and value:
            info[labels[text(th)]] = value
    return games, info

def parse_league_players(html):
    players = {}
    for link in soup(html).select('table.players a[href*="player="]'):
        players[query(link["href"], "player")] = display_name(text(link))
    return players

def parse_ranking_overview(html):
    page = soup(html)
    week = page.select_one('select[name$="dlPublication"] option[selected]') or page.select_one('select[name$="dlPublication"] option')
    categories = {}
    for link in page.select('table.ruler th a[href*="category="]'):
        categories.setdefault(query(link["href"], "category"), text(link))
    return dict(publicationId=week.get("value") if week else None, week=text(week),
                categories=[dict(id=k, name=v) for k, v in categories.items()])

def parse_ranking_page(html):
    """Rows of one ranking page. The member number and the profile link that holds it are left out on purpose."""
    page = soup(html)
    entries = []
    table = page.select_one("table.ruler")
    for row in table.select("tr") if table else []:
        cells = row.find_all("td", recursive=False)
        player = row.select_one('a[href*="player.aspx"]')
        if not player or len(cells) < 6:
            continue
        previous = re.search(r"(\d+)", next((c.get("title", "") for c in cells if "rank_" in " ".join(c.get("class", []))), ""))
        entries.append(dict(rank=number(text(row.select_one("td.rank"))), previousRank=int(previous[1]) if previous else None,
                            sourceId=query(player["href"], "player"), name=text(player),
                            club=text(cells[-3]), level=number(text(cells[-2])), points=number(text(cells[-1]))))
    paging = re.search(r"(?:Pagina|Page) (\d+) (?:van|of) (\d+) - (\d+)", page.get_text(" ", strip=True))
    return dict(entries=entries, pages=int(paging[2]) if paging else 1, total=int(paging[3]) if paging else len(entries))

def selected_age_group(html):
    selected = soup(html).select_one('select[name$="CS"] option[selected]')
    return number(selected.get("value")) if selected else None


class SharedPace:
    """The turns of all runs that use one scrapes folder, so concurrent runs together keep the pace of one run."""

    def __init__(self, folder):
        folder.mkdir(parents=True, exist_ok=True)
        self.state, self.lock = folder / "_pace.json", folder / "_pace.lock"

    @contextmanager
    def _locked(self):
        while True:
            try:
                os.close(os.open(self.lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY))
                break
            except (FileExistsError, PermissionError):
                # A run that was killed while holding the lock must not block the others for ever.
                try:
                    if time.time() - self.lock.stat().st_mtime > 30:
                        self.lock.unlink()
                except OSError:
                    pass
                time.sleep(0.05)
        try:
            yield json.loads(self.state.read_text(encoding="utf-8")) if self.state.exists() else {}
        finally:
            self.lock.unlink()

    def _stop_if_refused(self, state):
        if time.time() - state.get("refusedAt", 0) < COOLDOWN_SECONDS:
            minutes = int((state["refusedAt"] + COOLDOWN_SECONDS - time.time()) / 60) + 1
            raise StopScrape(f"A run was refused by the site: {state.get('reason')} Nothing is sent for {minutes} more minutes. Deleting {self.state.name} ends this wait.")

    def turn(self, delay):
        with self._locked() as state:
            self._stop_if_refused(state)
            now = time.time()
            slot = max(now, state.get("next", 0))
            state["next"] = slot + delay + random.uniform(0, delay / 2)
            self.state.write_text(json.dumps(state), encoding="utf-8")
        if slot > now:
            time.sleep(slot - now)
        with self._locked() as state:
            self._stop_if_refused(state)

    def refused(self, reason):
        with self._locked() as state:
            state.update(refusedAt=time.time(), reason=reason)
            self.state.write_text(json.dumps(state), encoding="utf-8")


def write_atomic(file, text):
    """Concurrent runs share the cache, so a page appears whole or not at all."""
    temporary = file.with_name(f"{file.name}.{os.getpid()}.tmp")
    temporary.write_text(text, encoding="utf-8")
    for attempt in range(20):
        try:
            return os.replace(temporary, file)
        except PermissionError:
            # Windows refuses the replacement while another run is reading the file.
            time.sleep(0.1)
    temporary.unlink()

def read_shared(file):
    for attempt in range(20):
        try:
            return file.read_text(encoding="utf-8")
        except PermissionError:
            time.sleep(0.1)
    return file.read_text(encoding="utf-8")


class Fetcher:
    """Every request of the scraper goes through here: one at a time, slow, capped, and stopping on a refusal."""

    def __init__(self, delay, max_requests, offline=False, list_hours=24, sheet_days=30):
        self.delay, self.max_requests, self.offline = max(delay, MIN_DELAY), max_requests, offline
        self.list_age, self.sheet_age = timedelta(hours=list_hours), timedelta(days=sheet_days)
        self.sent, self.reused, self.sources = 0, 0, []
        self.pace = SharedPace(SCRAPES)
        self.search_sources = []
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.opener.addheaders = [("User-Agent", USER_AGENT)]
        self.consented, self.sport_selected = False, None
        CACHE.mkdir(parents=True, exist_ok=True)

    def _take_turn(self):
        if self.sent >= self.max_requests:
            raise StopScrape(f"Request cap of {self.max_requests} reached. Run the same command again to continue from the cache.")
        self.pace.turn(self.delay)

    def _send(self, path, data=None):
        self.sent += 1
        try:
            with self.opener.open(BASE + path, data, timeout=60) as response:
                return response.read().decode("utf-8", "replace"), response.geturl()
        except urllib.error.HTTPError as error:
            if error.code == 404:
                return None, path
            reason = f"The site answered {error.code} for {path}."
            self.pace.refused(reason)
            raise StopScrape(f"{reason} Stopped at once, no retry. Wait before trying again.") from error
        except OSError as error:
            raise StopScrape(f"No answer for {path}: {error}. Stopped at once, no retry.") from error
        finally:
            if self.sent % 25 == 0:
                print(f"  {self.sent} requests sent", flush=True)

    def _consent(self):
        # CookiePurposes=1 is the necessary-cookies-only choice of the cookie settings of the site.
        self._take_turn()
        self._send("/cookiewall/Save", urlencode({"ReturnUrl": "/", "SettingsOpen": "true", "CookiePurposes": "1"}).encode())
        self.consented = True

    def select_sport(self, site_id):
        if not self.offline and self.sport_selected != site_id:
            if not self.consented:
                self._consent()
            self._take_turn()
            self._send(f"/sportselection/setsportselection/{site_id}?returnUrl=%2Fleagues")
            self.sport_selected = site_id

    def get(self, path, form=None, max_age=None):
        """The page text, from the cache while it is young enough. None when the site has no such page."""
        data = urlencode(form).encode() if form else None
        digest = hashlib.sha256((path + (data or b"").decode()).encode()).hexdigest()[:16]
        file = CACHE / f'{re.sub(r"[^A-Za-z0-9]+", "_", path)[:80]}_{digest}.html'
        def from_cache():
            fresh = file.exists() and datetime.now(timezone.utc) - datetime.fromtimestamp(file.stat().st_mtime, timezone.utc) <= (max_age or self.list_age)
            if not fresh and not (self.offline and file.exists()):
                return None
            self.reused += 1
            html = read_shared(file)
            self._record(path, html, file, True)
            return html
        cached = from_cache()
        if cached is not None:
            return cached
        if self.offline:
            raise StopScrape(f"Offline, and {path} is not in the cache.")
        if not self.consented:
            self._consent()
        self._take_turn()
        # Another run may have fetched this page while this one waited for its turn.
        cached = from_cache()
        if cached is not None:
            return cached
        html, final = self._send(path, data)
        if html is not None and "/cookiewall" in final:
            self._consent()
            self._take_turn()
            html, final = self._send(path, data)
            if "/cookiewall" in final:
                raise StopScrape("The cookie wall came back after choosing necessary cookies. Stopped.")
        if html is None:
            return None
        write_atomic(file, html)
        self._record(path, html, file, False)
        return html

    def _record(self, path, html, file, from_cache):
        fetched = datetime.fromtimestamp(file.stat().st_mtime, BRUSSELS).isoformat(timespec="seconds")
        self.sources.append(dict(url=BASE + path, fetchedAt=fetched, fromCache=from_cache, sha256=hashlib.sha256(html.encode()).hexdigest()))


def season_window(season):
    m = re.fullmatch(r"(\d{4})-(\d{4})", season or "")
    if not m or int(m[2]) != int(m[1]) + 1:
        raise SystemExit(f"--season must look like 2026-2027, not {season!r}.")
    return date(int(m[1]), 7, 1), date(int(m[2]), 6, 30)

def find_leagues(fetcher, sport, country, season):
    """Every league of the country that starts in the season, youth left out and reported separately."""
    start, end = season_window(season)
    fetcher.select_sport(SPORTS[sport]["site"])
    found = {}
    for page in range(1, 21):
        form = {"Page": page, "LeagueFilter.Q": "", "LeagueFilter.StartDate": f"{start}T00:00", "LeagueFilter.EndDate": f"{end}T00:00", "LeagueFilter.CountryCode": country}
        before = len(found)
        for league in parse_league_search(fetcher.get("/find/league/DoSearch", form) or ""):
            found[league["id"]] = league
        if len(found) == before:
            break
    inside = [l for l in found.values() if l["startsOn"] and start.isoformat() <= l["startsOn"] <= end.isoformat()]
    return [l for l in inside if not YOUTH.search(l["name"])], [l for l in inside if YOUTH.search(l["name"])]


class Run:
    def __init__(self, args, fetcher):
        self.args, self.fetcher = args, fetcher
        self.leagues, self.clubs, self.teams, self.players, self.rankings, self.matches, self.warnings = [], [], [], [], [], [], []
        self.excluded_youth = 0
        self.sent_before, self.reused_before = fetcher.sent, fetcher.reused
        fetcher.sources = list(fetcher.search_sources)
        self.division = re.compile(args.division, re.I) if args.division else None

    def warn(self, message):
        self.warnings.append(message)
        print("  warning:", message, flush=True)

    def wanted(self, entity):
        return entity in self.args.include

    def league(self, league):
        print(f"League: {league['name']}", flush=True)
        self.leagues.append(league)
        ident = league["id"]
        if self.wanted("clubs") or self.wanted("matches"):
            self.read_clubs(ident)
        if self.wanted("players"):
            self.read_players(ident)
        if self.wanted("matches"):
            self.read_matches(ident)

    def read_clubs(self, ident):
        for club in parse_clubs(self.fetcher.get(f"/sport/clubs.aspx?id={ident}") or ""):
            self.clubs.append(dict(club, leagueId=ident, key=f"{ident}:{club['id']}"))
            for team in parse_club_teams(self.fetcher.get(f"/sport/clubteams.aspx?id={ident}&cid={club['id']}") or ""):
                self.teams.append(dict(team, leagueId=ident, clubId=club["id"], key=f"{ident}:{team['id']}"))

    def read_players(self, ident):
        for source_id, name in parse_league_players(self.fetcher.get(f"/sport/players.aspx?id={ident}") or "").items():
            self.players.append(dict(key=f"{ident}:{source_id}", leagueId=ident, sourceId=source_id, name=name, teams=[], levels={}))

    def in_dates(self, day):
        return day is None or ((not self.args.since or day >= self.args.since) and (not self.args.until or day <= self.args.until))

    def read_matches(self, ident):
        for draw in parse_draws(self.fetcher.get(f"/sport/draws.aspx?id={ident}") or ""):
            if YOUTH.search(draw["name"]) or (self.division and not self.division.search(draw["name"])):
                continue
            print(f"  Division: {draw['name']}", flush=True)
            members = parse_draw_teams(self.fetcher.get(f"/sport/draw.aspx?id={ident}&draw={draw['id']}") or "")
            for fixture in parse_draw_matches(self.fetcher.get(f"/sport/drawmatches.aspx?id={ident}&draw={draw['id']}") or ""):
                if self.in_dates(fixture["date"]):
                    self.read_match(ident, draw, members, fixture)

    def read_match(self, ident, draw, members, fixture):
        url = f"/sport/teammatch.aspx?id={ident}&match={fixture['id']}"
        match = dict(fixture, key=f"{ident}:{fixture['id']}", leagueId=ident, drawId=draw["id"], division=draw["name"],
                     homeTeamId=members.get(fixture["home"]), awayTeamId=members.get(fixture["away"]), url=BASE + url, games=[])
        for side in ("home", "away"):
            if not members.get(fixture[side]):
                self.warn(f"Match {match['key']}: team {fixture[side]!r} is not in the standing of {draw['name']!r}.")
        if fixture["sourceScore"]:
            html = self.fetcher.get(url, max_age=self.fetcher.sheet_age)
            match["games"], info = parse_match_sheet(html or "", SPORTS[self.args.sport]["games"])
            match.update(info)
            if not match["games"]:
                self.warn(f"Match {match['key']}: published score {fixture['sourceScore']!r} but no games could be read.")
        self.matches.append(match)

    def read_rankings(self):
        rid = RANKINGS.get((self.args.sport, self.args.country))
        if not rid:
            return self.warn(f"No national ranking is configured for {self.args.sport} in {self.args.country}.")
        overview = parse_ranking_overview(self.fetcher.get(f"/ranking/ranking.aspx?rid={rid}") or "")
        if not overview["publicationId"]:
            return self.warn("The ranking overview could not be read.")
        wanted = re.compile(self.args.ranking_categories, re.I) if self.args.ranking_categories else None
        for category in overview["categories"]:
            if YOUTH.search(category["name"]) or (wanted and not wanted.search(category["name"])):
                continue
            print(f"Ranking: {category['name']} week {overview['week']}", flush=True)
            self.read_ranking(rid, overview, category)

    def ranking_url(self, publication, category, age_group, page):
        c = f"C{category}"
        return f"/ranking/category.aspx?id={publication}&category={category}&{c}CS={age_group}&{c}FTYAF=0&{c}FTYAT=0&{c}FOG_4_F2048=&p={page}&ps=100"

    def youth_ids(self, publication, category):
        """Only used to leave these players out: this list is never written. None when the filter did not visibly apply."""
        ids, page, pages = set(), 1, 1
        while page <= pages:
            html = self.fetcher.get(self.ranking_url(publication, category, U19_AGE_GROUP, page)) or ""
            if selected_age_group(html) != U19_AGE_GROUP:
                return None
            parsed = parse_ranking_page(html)
            pages = parsed["pages"]
            ids.update(e["sourceId"] for e in parsed["entries"])
            page += 1
        return ids

    def read_ranking(self, rid, overview, category):
        publication = overview["publicationId"]
        youth = self.youth_ids(publication, category["id"])
        if youth is None:
            # Without a working youth filter nothing of this category is stored.
            return self.warn(f"Ranking {category['name']}: the U19 filter did not apply, so the category is skipped.")
        page, pages, kept = 1, 1, 0
        while page <= pages and (not self.args.ranking_top or kept < self.args.ranking_top):
            parsed = parse_ranking_page(self.fetcher.get(self.ranking_url(publication, category["id"], 0, page)) or "")
            pages = parsed["pages"]
            for entry in parsed["entries"]:
                if entry["sourceId"] in youth:
                    self.excluded_youth += 1
                elif not self.args.ranking_top or kept < self.args.ranking_top:
                    kept += 1
                    self.rankings.append(dict(entry, rankingId=rid, publicationId=publication, week=overview["week"], categoryId=category["id"], category=category["name"]))
            page += 1

    def finish_players(self):
        """Team, display name and latest level per discipline of each player, as seen on the match sheets."""
        by_key = {p["key"]: p for p in self.players}
        team_names = {t["key"]: t["name"] for t in self.teams}
        latest = {}
        for match in sorted(self.matches, key=lambda m: m["date"] or ""):
            for game in match["games"]:
                for side, team in (("home", match["homeTeamId"]), ("away", match["awayTeamId"])):
                    for seen in game[side]:
                        player = by_key.get(f"{match['leagueId']}:{seen['sourceId']}")
                        if not player:
                            continue
                        player["name"] = seen["name"]
                        team_key = f"{match['leagueId']}:{team}"
                        if team and team_key not in {t["key"] for t in player["teams"]}:
                            player["teams"].append(dict(key=team_key, name=team_names.get(team_key, "")))
                        if seen["level"] is not None:
                            player["levels"][game["discipline"]] = seen["level"]


def dump(path, payload):
    raw = json.dumps(payload, ensure_ascii=False, indent=1).encode("utf-8")
    path.write_bytes(raw)
    return dict(sha256=hashlib.sha256(raw).hexdigest(), bytes=len(raw))

def league_folder(name):
    """Readable, and short enough for the folder field of the import page."""
    slug = re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")
    return slug[:40].rstrip("-") or "league"

def new_folder(parent, args, started, partial):
    """Two runs that start in the same second get the next free timestamp."""
    moment = started
    while True:
        out = parent / (f"{args.sport}-{args.country.lower()}-{moment:%Y%m%d-%H%M%S}" + ("-partial" if partial else ""))
        try:
            out.mkdir(parents=True)
            return out
        except FileExistsError:
            moment += timedelta(seconds=1)

def write_run(run, folder, league_name, status, reason, started):
    args, fetcher = run.args, run.fetcher
    out = new_folder(SCRAPES / folder, args, started, status != "complete")
    run.finish_players()
    sets = dict(leagues=run.leagues, clubs=run.clubs, teams=run.teams, players=run.players, rankings=run.rankings, matches=run.matches)
    files = {}
    for key, values in sets.items():
        files[f"{key}.json"] = dict(dump(out / f"{key}.json", values), records=len(values))
    with (out / "sources.jsonl").open("w", encoding="utf-8") as stream:
        stream.writelines(json.dumps(s, ensure_ascii=False) + "\n" for s in fetcher.sources)
    manifest = dict(schemaVersion=SCHEMA_VERSION, status=status, stopReason=reason, source=BASE,
                    startedAt=started.isoformat(timespec="seconds"), finishedAt=datetime.now(BRUSSELS).isoformat(timespec="seconds"),
                    filters=dict(sport=args.sport, country=args.country, season=args.season if league_name else None, league=league_name,
                                 division=args.division if league_name else None, since=args.since if league_name else None, until=args.until if league_name else None,
                                 include=[e for e in args.include if (e != "rankings") == bool(league_name)],
                                 rankingCategories=None if league_name else args.ranking_categories, rankingTop=None if league_name else args.ranking_top),
                    requests=dict(sent=fetcher.sent - run.sent_before, reusedFromCache=fetcher.reused - run.reused_before, delaySeconds=fetcher.delay, cap=fetcher.max_requests),
                    counts=dict({k: len(v) for k, v in sets.items()}, games=sum(len(m["games"]) for m in run.matches), youthRankingEntriesLeftOut=run.excluded_youth),
                    files=files, warnings=run.warnings,
                    note="Public results for local development. Real names as published, adults only, no member numbers, birth dates or contacts.")
    dump(out / "manifest.json", manifest)
    return out, manifest


def pick_leagues(leagues, pattern, league_id):
    """An id is exact, for the application's selection; a pattern is for the command line."""
    if league_id:
        return [l for l in leagues if l["id"].lower() == league_id.lower()]
    compiled = re.compile(pattern or "", re.I)
    return [l for l in leagues if compiled.search(l["name"])]

def cmd_leagues(args):
    fetcher = Fetcher(args.delay, args.max_requests, args.offline)
    leagues, youth = find_leagues(fetcher, args.sport, args.country, args.season)
    if args.json:
        print(json.dumps(dict(leagues=leagues, youthLeftOut=len(youth), requests=fetcher.sent), ensure_ascii=False))
        return 0
    for league in leagues:
        print(f"{league['startsOn']}  {league['id']}  {league['name']}  ({league['organizer']})")
    print(f"{len(leagues)} leagues, {len(youth)} youth leagues left out, {fetcher.sent} requests.")

def cmd_run(args):
    args.country = args.country.upper()
    args.include = [e.strip() for e in args.include.split(",") if e.strip()]
    unknown = set(args.include) - set(ENTITIES)
    if unknown:
        raise SystemExit(f"--include knows {', '.join(ENTITIES)}, not {', '.join(sorted(unknown))}.")
    league_data = set(args.include) - {"rankings"}
    if league_data and not (args.league or args.league_id or args.all_leagues):
        raise SystemExit("Choose leagues with --league <text>, or --all-leagues on purpose. See them with the leagues command.")
    if league_data and not args.season:
        raise SystemExit("--season is needed to find leagues, for example --season 2026-2027.")
    fetcher = Fetcher(args.delay, args.max_requests, args.offline, args.list_max_age_hours, args.sheet_max_age_days)
    chosen = []
    if league_data:
        try:
            leagues, _ = find_leagues(fetcher, args.sport, args.country, args.season)
            # leagues.json of every league comes from these search pages.
            fetcher.search_sources = list(fetcher.sources)
        except StopScrape as stop:
            print("Stopped:", stop, flush=True)
            return 2
        chosen = pick_leagues(leagues, args.league, args.league_id)
        if not chosen:
            print(f"No adult league of {args.season} in {args.country} matches {args.league_id or args.league!r}.", flush=True)
            return 2
    for league in chosen:
        if not capture(args, fetcher, league_folder(league["name"]), league["name"], lambda run, l=league: run.league(l)):
            return 2
    if "rankings" in args.include and not capture(args, fetcher, RANKINGS_FOLDER, None, lambda run: run.read_rankings()):
        return 2
    return 0

def capture(args, fetcher, folder, league_name, work):
    """One output folder. False when the run stopped early, which also ends the rest of the command."""
    started = datetime.now(BRUSSELS)
    run = Run(args, fetcher)
    status, reason = "complete", None
    try:
        work(run)
    except StopScrape as stop:
        status, reason = "partial", str(stop)
        print("Stopped:", stop, flush=True)
    out, manifest = write_run(run, folder, league_name, status, reason, started)
    print(json.dumps(dict(status=status, folder=str(out), counts=manifest["counts"], requests=manifest["requests"], warnings=len(run.warnings)), ensure_ascii=False, indent=1))
    return status == "complete"


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="Small, slow captures of public league data from toernooi.nl.")
    sub = parser.add_subparsers(dest="command", required=True)
    def common(p):
        p.add_argument("--sport", default="badminton", choices=sorted(SPORTS))
        p.add_argument("--country", default="BEL", help="three letter code of the site, BEL by default")
        p.add_argument("--season", help="for example 2026-2027")
        p.add_argument("--delay", type=float, default=5.0, help=f"seconds between requests plus up to half of that at random, at least {MIN_DELAY:.0f}")
        p.add_argument("--max-requests", type=int, default=400, help="the run stops and keeps its files at this number of requests")
        p.add_argument("--offline", action="store_true", help="use only cached pages and send nothing")
    leagues = sub.add_parser("leagues", help="list the adult leagues of a season")
    common(leagues)
    leagues.add_argument("--json", action="store_true", help="print the leagues as JSON, for the application")
    leagues.set_defaults(func=cmd_leagues)
    run = sub.add_parser("run", help="capture into a new timestamped folder")
    common(run)
    run.add_argument("--league", help="only leagues whose name contains this (regular expression)")
    run.add_argument("--league-id", help="exactly this league, by the id the leagues command shows")
    run.add_argument("--all-leagues", action="store_true", help="every adult league of the season: many requests")
    run.add_argument("--division", help="only divisions whose name contains this (regular expression)")
    run.add_argument("--since", help="only matches on or after this date, YYYY-MM-DD")
    run.add_argument("--until", help="only matches on or before this date, YYYY-MM-DD")
    run.add_argument("--include", default=",".join(ENTITIES), help="comma list of " + ", ".join(ENTITIES))
    run.add_argument("--ranking-categories", help="only ranking categories whose name contains this (regular expression)")
    run.add_argument("--ranking-top", type=int, default=200, help="adults kept per ranking category, 0 for all")
    run.add_argument("--list-max-age-hours", type=float, default=24, help="reuse a cached list page up to this age")
    run.add_argument("--sheet-max-age-days", type=float, default=30, help="reuse a cached played match sheet up to this age")
    run.set_defaults(func=cmd_run)
    args = parser.parse_args()
    args.country = args.country.upper()
    return args.func(args) or 0


if __name__ == "__main__":
    sys.exit(main())
