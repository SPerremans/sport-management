"""Frozen public game history for the ranking simulation samples (Belgian adult badminton league sheets and adult tennis tournaments).

Public browsing with necessary cookies only, no sign-in, throttled, pages cached in deployment/tmp/ranking-history so a rerun sends no new requests.
Youth events are never captured. Never invents a game: a game without a published winner is skipped and counted.
Usage: python -X utf8 docs/development/build-ranking-history.py [--offline]
"""
from pathlib import Path
from bs4 import BeautifulSoup
from urllib.parse import urlencode
import http.cookiejar, json, re, sys, time, hashlib, urllib.request, collections

ROOT = Path(__file__).resolve().parents[2]
CACHE = ROOT / "deployment/tmp/ranking-history"
OUTPUT = ROOT / "resources/ranking-history-public.json"
CAPTURED = "2026-10-31"
DELAY = 1.5
BASE = "https://www.toernooi.nl"
# Provincial adult league of Antwerp: the three highest provincial classes, so the same players recur across seasons and the request count stays modest.
BADMINTON = [("541D9693-BA5C-49F3-B354-186D1A701882", "PBA competitie 2024-2025"), ("A0E9B3BE-1D47-428D-8EB5-7807839F7498", "PBA competitie 2025-2026"), ("88774B10-ACF3-4F2F-B616-9A803C8B3880", "PBA competitie 2026-2027")]
DIVISIONS = re.compile(r"(?i)\b[123]e\s+provinciale")
TENNIS = [("89AD5181-7BE4-468B-A827-D2E6E812A2F6", "Field Recreatief 2023"), ("07BBDB14-00BD-4328-968E-66882E94567F", "Verschueren Installatietechniek dubbeltoernooi"),
    ("13505BD7-0419-47B9-A759-B8A948453B1B", "Tennisfun Dubbeltoernooi 2024"), ("EF79436B-0067-4073-9135-339C08299CE0", "Clubkampioenschap LRTC 2024"), ("C21E48C4-3616-4B2D-91A2-8B5BEAA5808E", "Kermistornooi LRTC 2024"),
    ("B1D2FF75-B7BB-4445-A451-BB5F21635CB0", "Goldcup NTC 2024"), ("FEA0D837-D1D1-484E-AF56-1D0740BAC4BA", "Field Open Recreatief 2024"), ("0518542C-096B-4E38-9FE6-4CEB0246E2FF", "Tennisfun Dubbeltoernooi 2025"),
    ("BC371582-3CA9-4E7F-A585-5F0E1AD46C2C", "Kermistornooi LRTC 2025"), ("7DE19FD2-6436-4E89-806B-5F2A5636E649", "Goldcup NTC 2025"), ("DB3D8914-36D8-4BC9-9E05-088567EA3A38", "Field Recreatief 2025"),
    ("18132F17-494D-4AF7-BC6A-8D0014BD3D8F", "Clubkampioenschap LRTC 2025"), ("0868EB45-F7B1-417F-B25B-E69F28B6507C", "Kermistoernooi LRTC 2026"), ("763272AB-8D18-4461-A40B-51A5EC7C581C", "Goldcup NTC 2026"),
    ("81AAD17E-51E2-4D47-80E7-305D8FFD198C", "Field Recreatief 2026"), ("0006EC58-0121-4330-BFBA-B2D87F8247AB", "Clubkampioenschap LRTC 2026")]
OFFLINE = "--offline" in sys.argv
CACHE.mkdir(parents=True, exist_ok=True)
jar = http.cookiejar.MozillaCookieJar(str(CACHE / "cookies.txt"))
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
opener.addheaders = [("User-Agent", "Mozilla/5.0 (local development sample capture)")]
requests = 0

def consent():
    # CookiePurposes=1 is the necessary-cookies-only choice of the cookie settings of the site.
    opener.open(BASE + "/cookiewall/Save", urlencode({"ReturnUrl": "/", "SettingsOpen": "true", "CookiePurposes": "1"}).encode(), timeout=60).read()

def fetch(path):
    global requests
    file = CACHE / (re.sub(r"[^A-Za-z0-9]+", "_", path) + ".html")
    if file.exists():
        return BeautifulSoup(file.read_text(encoding="utf-8"), "html.parser")
    if OFFLINE:
        return None
    if requests == 0:
        consent()
    time.sleep(DELAY)
    requests += 1
    html = opener.open(BASE + path, timeout=60).read().decode("utf-8", "replace")
    file.write_text(html, encoding="utf-8")
    if requests % 50 == 0:
        print(requests, "requests", flush=True)
    return BeautifulSoup(html, "html.parser")

def text(node):
    return node.get_text(" ", strip=True) if node else ""

def day(raw):
    m = re.search(r"(\d{1,2})-(\d{1,2})-(\d{4})", raw)
    return f"{m[3]}-{int(m[2]):02d}-{int(m[1]):02d}" if m else None

def side(cell):
    """Names with the level in brackets as published, and whether the source highlights this side as the winner."""
    names, levels = [], []
    for a in cell.select('a[href*="player="]'):
        raw = text(a)
        m = re.search(r"\((\d{1,2})\)\s*$", raw)
        levels.append(int(m[1]) if m else None)
        names.append(re.sub(r"\s*\(\d{1,2}\)\s*$", "", raw).strip())
    return names, levels, cell.select_one("strong") is not None

def game(disc, cells, date, source, event, stats):
    """cells: home, separator, away, score."""
    home, hl, hw = side(cells[0])
    away, al, aw = side(cells[2])
    score = " ".join(text(s) for s in cells[3].select(".score span")) or text(cells[3])
    flag = "R" if re.search(r"(?i)opgave|retired|ret\.", text(cells[3])) else "W" if re.search(r"(?i)walk|w\.o|geen speler", text(cells[3])) else ""
    if not home or not away or len(home) != len(away) or hw == aw:
        stats["skipped"] += 1
        return None
    win, wl, lose, ll = (home, hl, away, al) if hw else (away, al, home, hl)
    return dict(d=date, c=disc, w=win, l=lose, wl=wl, ll=ll, s=re.sub(r"\s+", " ", score)[:40], f=flag, x=source, e=event)

def badminton(ident, source, games, stats):
    draws = fetch(f"/sport/draws.aspx?id={ident}")
    links = [(a["href"].split("draw=")[1], text(a)) for a in draws.select('a[href*="draw.aspx"]') if "draw=" in a["href"]] if draws else []
    seen = set()
    for did, name in links:
        if not DIVISIONS.search(name):
            continue
        page = fetch(f"/sport/drawmatches.aspx?id={ident}&draw={did}")
        for row in page.select("table.matches tr") if page else []:
            link = row.select_one('a[href*="teammatch.aspx"]')
            planned = row.select_one(".plannedtime")
            if not link or not planned:
                continue
            mid = link["href"].split("match=")[1].split("&")[0]
            date = day(text(planned))
            if mid in seen or not date or date > CAPTURED:
                continue
            seen.add(mid)
            sheet = fetch(f"/sport/teammatch.aspx?id={ident}&match={mid}")
            table = sheet.select_one("table.matches") if sheet else None
            for tr in table.select(":scope > tbody > tr") if table else []:
                tds = tr.find_all("td", recursive=False)
                if len(tds) >= 5 and re.fullmatch(r"(?:HD|DD|GD|HE|DE)\d*", text(tds[0])):
                    item = game(re.sub(r"\d", "", text(tds[0])), tds[1:5], date, source, name, stats)
                    if item:
                        games.append(item)

def tennis_discipline(name):
    """Club tournaments name their events freely: HD1, DH35, Heren Dubbel, Enkel Heren. The result is one of the discipline codes of the sheets."""
    m = re.match(r"(HD|DD|GD|HE|DE|DH)(?![A-Za-z])", name)
    if m:
        return "HD" if m[1] == "DH" else m[1]
    low = name.lower()
    single = "enkel" in low
    if "gemengd" in low:
        return "GD"
    if "heren" in low:
        return "HE" if single else "HD" if "dubbel" in low else None
    if "dames" in low:
        return "DE" if single else "DD" if "dubbel" in low else None
    return None

def tennis(ident, source, games, stats):
    draws = fetch(f"/sport/draws.aspx?id={ident}")
    links = [(a["href"].split("draw=")[1], text(a)) for a in draws.select('a[href*="draw.aspx"]') if "draw=" in a["href"]] if draws else []
    for did, name in links:
        code = tennis_discipline(name)
        if not code or re.search(r"(?i)jeugd|-1\d|u1\d|junior", name):
            continue
        page = fetch(f"/sport/drawmatches.aspx?id={ident}&draw={did}")
        for tr in page.select("table.matches > tbody > tr") if page else []:
            tds = tr.find_all("td", recursive=False)
            planned = tr.select_one(".plannedtime")
            if len(tds) < 6 or not planned or not day(text(planned)):
                continue
            item = game(code, tds[2:6], day(text(planned)), source, name, stats)
            if item:
                games.append(item)

def compact(games, sources):
    """Players as a string table to keep the resource small."""
    names = {}
    def code(n):
        return names.setdefault(n, len(names))
    rows = []
    for g in sorted(games, key=lambda g: (g["d"], g["x"], g["e"])):
        rows.append([g["d"], g["c"], [code(n) for n in g["w"]], [code(n) for n in g["l"]], g["wl"] if any(v is not None for v in g["wl"]) else [], g["ll"] if any(v is not None for v in g["ll"]) else [], g["s"], g["f"], g["x"], g["e"]])
    return dict(sources=sources, players=list(names), games=rows)

def main():
    result = dict(schemaVersion=1, capturedOn=CAPTURED, note="Public results, local development copy. Adult events only.", datasets=[])
    for key, sport, title, level_text, entries, reader in [
        ("tennis-be", "Tennis", "Belgian club tennis tournaments, 2023 to 2026", "No levels on the sheets", TENNIS, tennis),
        ("badminton-be", "Badminton", "Antwerp provincial badminton league, 2024 to 2026", "Levels 1 to 12 as printed on the sheets", BADMINTON, badminton)]:
        games, stats, sources = [], collections.Counter(), []
        for i, (ident, name) in enumerate(entries):
            before = len(games)
            reader(ident, i, games, stats)
            sources.append(dict(name=name, url=f"{BASE}/sport/tournament?id={ident}", games=len(games) - before))
            print(key, name, len(games) - before, flush=True)
        data = compact(games, sources)
        data.update(key=key, sport=sport, title=title, levels=level_text, skipped=stats["skipped"])
        result["datasets"].append(data)
    payload = json.dumps(result, ensure_ascii=False, separators=(",", ":"))
    OUTPUT.write_text(payload, encoding="utf-8")
    print(json.dumps(dict(requests=requests, bytes=len(payload.encode()), sha256=hashlib.sha256(payload.encode()).hexdigest(), datasets=[dict(key=d["key"], games=len(d["games"]), players=len(d["players"]), skipped=d["skipped"]) for d in result["datasets"]])))

main()
