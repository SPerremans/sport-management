"""Normalize the public pages cached by fetch-competition-snapshot.ps1; never invent fixtures."""
from pathlib import Path
from bs4 import BeautifulSoup
from urllib.parse import parse_qs, urlparse
from datetime import datetime
from zoneinfo import ZoneInfo
import collections
import hashlib
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
CACHE = ROOT / "deployment/tmp"
OUTPUT = ROOT / "resources/competition-public-2026-2027.json"
CAPTURED = "2026-10-24"
IDS = ["0c62bb49-5d47-4217-864d-ec86875b1c00", "7c182df5-153d-443a-86db-c21a50e46372",
       "88774B10-ACF3-4F2F-B616-9A803C8B3880", "E0230E0A-A3D9-4E95-BA2B-6E8E8D252B6C"]

def page(path):
    return BeautifulSoup(Path(path).read_text(encoding="utf-8-sig"), "html.parser")

def text(node):
    return node.get_text(" ", strip=True) if node else ""

def key(href, param):
    return parse_qs(urlparse(href).query)[param][0]

def rows(table):
    return table.select(":scope > tbody > tr") or table.find_all("tr", recursive=False)

def iso_date(raw):
    m = re.search(r"(\d{1,2}-\d{1,2}-\d{4})(?:\s+(\d\d:\d\d))?", raw)
    assert m, raw
    local = datetime.strptime(m[1] + " " + (m[2] or "00:00"), "%d-%m-%Y %H:%M")
    # A date without a published time remains a date, never a fabricated midnight fixture.
    return local.date().isoformat(), (local.replace(tzinfo=ZoneInfo("Europe/Brussels")).isoformat() if m[2] else None)

def detail(path):
    p = page(path)
    games = []
    for row in rows(p.select_one("table.matches")):
        cells = row.find_all("td", recursive=False)
        labels = [i for i, c in enumerate(cells) if re.fullmatch(r"(?:HD|DD|GD|HE|DE)\d*", text(c))]
        if not labels:
            continue
        i = labels[0]
        raw = text(cells[i + 4])
        outcome = "Played" if re.search(r"\d+\s*-\s*\d+", raw) else "Pending"
        if "Opgave Team 1" in raw:
            outcome = "HomeRetired"
        elif "Opgave Team 2" in raw:
            outcome = "AwayRetired"
        elif "geen speler van Team 1" in raw:
            outcome = "HomeForfeit"
        elif "geen speler van Team 2" in raw:
            outcome = "AwayForfeit"
        elif "geen spelers" in raw:
            outcome = "NotPlayed"
        elif "Opgave" in raw:
            # Only infer the retiring side when the source explicitly highlights the winner.
            outcome = "AwayRetired" if cells[i + 1].select_one("strong") else "HomeRetired" if cells[i + 3].select_one("strong") else "Pending"
        sets = [[int(a), int(b)] for a, b in re.findall(r"(\d+)\s*-\s*(\d+)", raw)]
        names = lambda c: [text(a) for a in c.select('a[href*="player="]')]
        games.append(dict(name=text(cells[i]), home=names(cells[i + 1]), away=names(cells[i + 3]),
                          sets=sets, sourceScore=raw, outcome=outcome))
    assert len(games) == 8, (path, len(games))
    points = []
    totals = None
    for t in p.select("table.ruler"):
        if "Wedstrijdstatistiek" in text(t):
            for row in rows(t):
                c = row.find_all("td", recursive=False)
                if c and c[0].select_one('a[href*="team="]'):
                    points.append(int(text(c[1])))
                    if totals is None and len(c)>=14:
                        totals=dict(homeSets=int(text(c[8])),awaySets=int(text(c[10])),homeRallies=int(text(c[11])),awayRallies=int(text(c[13])),counted=sum(int(text(c[n])) for n in [2,3,4])>0)
    assert totals is not None, (path,"Published match-statistic totals missing")
    return games, points, totals

def badminton(ident):
    main = page(CACHE / f"public-{ident}.html")
    name = text(main.select_one("title")).split(" - ")[1]
    clubs, teams = {}, {}
    queue = json.loads((CACHE / "club-queue.json").read_text(encoding="utf-8-sig"))
    for club in queue:
        if not club["id"].startswith(ident + ":"):
            continue
        cid = club["id"].split(":")[1]
        clubs[cid] = dict(id=cid, name=club["name"])
        for a in page(CACHE / Path(club["path"]).name.replace("-club-", "-clubteams-")).select('a[href*="team.aspx?"]'):
            tid = key(a["href"], "team")
            teams[tid] = dict(id=tid, name=text(a), club=cid)
    draws = []
    for a in page(CACHE / f"draws-{ident}.html").select('a[href^="draw.aspx?"]'):
        did, label = key(a["href"], "draw"), text(a)
        p = page(CACHE / f"{ident}-draw-{did}.html")
        members = {}
        for t in p.select("table.ruler:not(.matches)"):
            for team in t.select('a[href*="team.aspx?"]'):
                tid = key(team["href"], "team")
                assert tid in teams, (ident, tid)
                members[text(team)] = tid
        fixtures = []
        matches_page = page(CACHE / f"{ident}-drawmatches-{did}.html")
        for table in matches_page.select("table.matches"):
            for row in rows(table):
                sides = row.select("a.teamname")
                if not sides:
                    continue
                assert len(sides) == 2
                mid = key(sides[0]["href"], "match")
                cells = row.find_all("td", recursive=False)
                date, starts = iso_date(text(row.select_one(".plannedtime")))
                home, away = text(sides[0]), text(sides[1])
                assert home in members and away in members, (label, home, away)
                scorecell = row.select_one(".score")
                source_score = text(scorecell)
                source_status = "; ".join(img.get("title", "") for img in row.select("img[title]"))
                scoreindex = next((n for n,c in enumerate(cells) if c.select_one(".score")), None)
                # Empty-score rows still have the score column immediately after the away team.
                awayindex = next(n for n,c in enumerate(cells) if sides[1] in c.descendants)
                venue = text(cells[(scoreindex if scoreindex is not None else awayindex + 1) + 1])
                beforehome = cells[2:next(n for n,c in enumerate(cells) if sides[0] in c.descendants)]
                roundname = " / ".join(text(c) for c in beforehome if text(c))
                fixture = dict(id=mid, home=members[home], away=members[away], date=date, startsAt=starts,
                               round=roundname, venue=venue, sourceScore=source_score, sourceStatus=source_status,
                               url=f"https://www.toernooi.nl/sport/teammatch.aspx?id={ident}&match={mid}")
                if source_score:
                    fixture["games"], fixture["points"], fixture["totals"] = detail(CACHE / f"{ident}-match-{mid}.html")
                    if "Niet gespeeld" in source_score:
                        for game in fixture["games"]:
                            if not game["sourceScore"]:
                                game["outcome"] = "NotPlayed"
                fixtures.append(fixture)
        national = ident == IDS[0]
        category = "Men" if label.startswith("Heren") else "Women" if label.startswith("Dames") else "Mixed"
        division = " – ".join(label.split(" – ")[:-1])
        draws.append(dict(id=did, name=label, division=division, category=category,
                          format="SingleRoundRobin" if national else "DoubleRoundRobin",
                          teams=list(members.values()), fixtures=fixtures))
    dates = re.search(r"(\d{1,2}-\d{1,2}-\d{4})\s+t/m\s+(\d{1,2}-\d{1,2}-\d{4})", text(main))
    assert dates, name
    organizer = re.search(r"Laatste wijziging:.*?\d{2}:\d{2}\s+(.+?)\s+\|", text(main))[1]
    return dict(id=ident, name=name, organizer=organizer, sport="Badminton", level=["National","Flemish","Provincial","Provincial"][IDS.index(ident)],
                url=f"https://www.toernooi.nl/sport/league?id={ident}", season="2026-2027", startsOn=iso_date(dates[1])[0], endsOn=iso_date(dates[2])[0],
                clubs=list(clubs.values()), teams=list(teams.values()), draws=draws)

def football(kind):
    nextdata = json.loads((CACHE / f"{kind}-next.json").read_text(encoding="utf-8-sig"))
    config = nextdata["props"]["pageProps"]["data"]["page"]["grids"][0]["areas"][0]["modules"][0]["data"]
    teams, fixtures = {}, []
    for week in config["gameweeks"]:
        data = json.loads((CACHE / f'{kind}-{week["week"]}.json').read_text(encoding="utf-8-sig"))
        assert data["gameweek"]["id"] == week["id"]
        for m in data["matches"]:
            for side in ["homeTeam", "awayTeam"]:
                t = m[side]
                teams[t["id"]] = dict(id=t["id"], name=t["name"], club=t["id"])
            fulltime = m["period"]["type"] == "FullTime"
            fixtures.append(dict(id=m["id"], home=m["homeTeam"]["id"], away=m["awayTeam"]["id"], date=m["date"],
                                 startsAt=m["time"], round=week["name"], venue=(m.get("venue") or {}).get("name", ""),
                                 sourceScore=f'{m["homeScore"]}-{m["awayScore"]}' if m["homeScore"] is not None and m["awayScore"] is not None else "",
                                 sourceStatus=m["period"]["name"], complete=fulltime,
                                 url="https://www.proleague.be/wedstrijden/" + m["slug"]))
    name = "Jupiler Pro League" if kind == "jpl" else "Challenger Pro League"
    return dict(id=kind, name=name, organizer="Pro League", sport="Football", level="First class" if kind == "jpl" else "Second class",
                url=f"https://www.proleague.be/{kind}-kalender", season="2026-2027",
                startsOn=min(f["date"] for f in fixtures if f["date"]), endsOn=max(f["date"] for f in fixtures if f["date"]),
                clubs=[dict(id=t["id"],name=t["name"]) for t in teams.values()], teams=list(teams.values()),
                draws=[dict(id="regular",name="Regulier Seizoen",division=name,category="Open",format="DoubleRoundRobin",teams=list(teams),fixtures=fixtures)])

def build():
    sources = [badminton(i) for i in IDS] + [football(k) for k in ["jpl", "cpl"]]
    for s in sources:
        fixture_ids = [f["id"] for d in s["draws"] for f in d["fixtures"]]
        assert len(fixture_ids) == len(set(fixture_ids)), (s["name"], "duplicate source fixture")
    snapshot = dict(schemaVersion=1,capturedOn=CAPTURED,timezone="Europe/Brussels",sources=sources)
    OUTPUT.write_text(json.dumps(snapshot,ensure_ascii=False,separators=(",", ":")),encoding="utf-8")
    report = dict(sha256=hashlib.sha256(OUTPUT.read_bytes()).hexdigest(), capturedOn=CAPTURED,
                  sources=[dict(name=s["name"],clubs=len(s["clubs"]),teams=len(s["teams"]),draws=len(s["draws"]),
                                fixtures=sum(len(d["fixtures"]) for d in s["draws"]),
                                detailedResults=sum(bool(f.get("games")) for d in s["draws"] for f in d["fixtures"])) for s in sources])
    (ROOT / "docs/development/competition-snapshot-counts.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(report,ensure_ascii=False,indent=2))

def plan(phase):
    """Emit only observed public links and first-party calendar API requests for PowerShell."""
    CACHE.mkdir(parents=True, exist_ok=True)
    queue = []
    def add(url, name):
        queue.append(dict(url=url,path=str(CACHE / name)))
    if phase == "initial":
        for ident in IDS:
            add(f"https://www.toernooi.nl/sport/league?id={ident}",f"public-{ident}.html")
            for kind in ["draws","clubs"]:
                add(f"https://www.toernooi.nl/sport/{kind}.aspx?id={ident}",f"{kind}-{ident}.html")
        for kind in ["jpl","cpl"]:
            add(f"https://www.proleague.be/{kind}-kalender",f"{kind}.html")
    elif phase == "groups":
        clubs = []
        for ident in IDS:
            for a in page(CACHE / f"draws-{ident}.html").select('a[href^="draw.aspx?"]'):
                did=key(a["href"],"draw")
                for kind in ["draw","drawmatches"]:
                    add(f"https://www.toernooi.nl/sport/{kind}.aspx?id={ident}&draw={did}",f"{ident}-{kind}-{did}.html")
            for a in page(CACHE / f"clubs-{ident}.html").select('a[href^="club.aspx?"]'):
                cid=key(a["href"],"club")
                clubs.append(dict(id=ident+":"+cid,name=text(a),path=f"deployment/tmp/{ident}-club-{cid}.html"))
                add(f"https://www.toernooi.nl/sport/clubteams.aspx?id={ident}&cid={cid}",f"{ident}-clubteams-{cid}.html")
        (CACHE / "club-queue.json").write_text(json.dumps(clubs,ensure_ascii=False),encoding="utf-8")
        for kind in ["jpl","cpl"]:
            raw=page(CACHE / f"{kind}.html").select_one("#__NEXT_DATA__").string
            (CACHE / f"{kind}-next.json").write_text(raw,encoding="utf-8")
            data=json.loads(raw)["props"]["pageProps"]["data"]["page"]["grids"][0]["areas"][0]["modules"][0]["data"]
            for week in data["gameweeks"]:
                add("https://www.proleague.be/api/football_list/football_competition_match/variant_a?locale=nl&editionId="+data["editionId"]+"&roundId="+data["roundId"]+"&gameweekId="+week["id"],f'{kind}-{week["week"]}.json')
    elif phase == "results":
        for ident in IDS:
            for p in CACHE.glob(f"{ident}-drawmatches-*.html"):
                for table in page(p).select("table.matches"):
                    for row in rows(table):
                        a=row.select_one("a.teamname")
                        if a and text(row.select_one(".score")):
                            mid=key(a["href"],"match")
                            add(f"https://www.toernooi.nl/sport/teammatch.aspx?id={ident}&match={mid}",f"{ident}-match-{mid}.html")
    else:
        raise ValueError(phase)
    (CACHE / "competition-fetch-queue.json").write_text(json.dumps(queue),encoding="utf-8")
    print(f"{phase}: {len(queue)} public requests")

if __name__ == "__main__":
    if len(sys.argv)>1 and sys.argv[1]=="--plan":
        plan(sys.argv[2])
    else:
        build()
