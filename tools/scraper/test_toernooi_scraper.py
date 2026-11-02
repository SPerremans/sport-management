"""Parser and safety checks on small fictional pages; sends no requests. Usage: python -X utf8 -m unittest tools/scraper/test_toernooi_scraper.py"""
from datetime import datetime
from pathlib import Path
from unittest import mock
import json, sys, tempfile, threading, time, types, unittest, urllib.error

sys.path.insert(0, str(Path(__file__).parent))
import toernooi_scraper as ts

SEARCH = """<ul><li class="list__item"><a class="media__link" title="Testcompetitie 2026-2027" href="/sport/tournament?id=aaaaaaaa-0000-0000-0000-000000000001">x</a>
<small class="media__subheading">Fictieve Bond | Testdorp</small><time datetime="2026-09-01 00:00"></time><time datetime="2027-04-30 23:59"></time></li>
<li class="list__item"><span>Box Ladder</span></li></ul>"""
CLUBS = """<table class="ruler"><thead><tr><td>Naam</td></tr></thead><tbody><tr><td><a href="club.aspx?id=A&amp;club=23">Test BC</a></td>
<td>10012</td><td>5</td><td>28</td><td>6</td><td>22</td><td></td><td>Testdorp</td><td></td></tr></tbody></table>"""
SHEET = """<table class="matches"><tbody><tr><td>HD1</td><td><strong><a href="player.aspx?id=A&amp;player=1">Tom Test (7)</a><a href="player.aspx?id=A&amp;player=2">Bo Voorbeeld (9)</a></strong></td><td>-</td>
<td><a href="player.aspx?id=A&amp;player=3">An Fictief</a><a href="player.aspx?id=A&amp;player=4">Kim Proef (8)</a></td><td>21-15 21-18</td></tr>
<tr><td>HE1</td><td><a href="player.aspx?id=A&amp;player=1">Tom Test (7)</a></td><td>-</td><td></td><td>geen speler van Team 2</td></tr></tbody></table>
<table><tr><th>Wedstrijdleider:</th><td>Lea Leider</td></tr><tr><th>Einduur:</th><td></td></tr></table>"""
RANKING = """<select name="x$C4582CS"><option value="0">Alles</option><option selected="selected" value="9">U19</option></select><table class="ruler">
<tr><th colspan="9">HE/SM</th></tr><tr><th>Rank</th></tr>
<tr><td class="rank"><div>2</div></td><td class="rank_down" title="Vorige rank: 1">1</td><td></td><td><a href="player.aspx?id=9&amp;player=77">Tom Test</a></td>
<td><a class="icon profile" href="/player/AAAA-BBBB/12345678" title="Profiel"></a></td><td>12345678</td><td><a href="category.aspx">Test BC</a></td><td>3</td><td class="right">2613</td></tr></table>
<div>Pagina 1 van 4 - 301 resultaten</div>"""
OVERVIEW = """<select name="a$dlPublication"><option value="54452">44-2026</option><option value="54331">43-2026</option></select>
<table class="ruler"><tr><th><a href="category.aspx?id=54452&amp;category=4582">HE/SM</a></th><th><a href="category.aspx?id=54452&amp;category=4582">Meer</a></th></tr></table>"""


class Parsers(unittest.TestCase):
    def test_pick_leagues_by_id_is_exact_and_by_pattern_searches(self):
        leagues = [dict(id="AB", name="PBA competitie"), dict(id="CD", name="PBA seniorencompetitie")]
        self.assertEqual(ts.pick_leagues(leagues, None, "ab"), leagues[:1])
        self.assertEqual(ts.pick_leagues(leagues, "pba", None), leagues)
        self.assertEqual(ts.pick_leagues(leagues, "pba", "XX"), [])

    def test_league_search_skips_tag_items(self):
        league = ts.parse_league_search(SEARCH)
        self.assertEqual(len(league), 1)
        self.assertEqual((league[0]["organizer"], league[0]["place"], league[0]["startsOn"], league[0]["endsOn"]), ("Fictieve Bond", "Testdorp", "2026-09-01", "2027-04-30"))

    def test_clubs(self):
        self.assertEqual(ts.parse_clubs(CLUBS), [dict(id="23", name="Test BC", number=10012, teamCount=5, playerCount=28, menCount=6, womenCount=22, place="Testdorp")])

    def test_match_sheet(self):
        games, info = ts.parse_match_sheet(SHEET, ts.SPORTS["badminton"]["games"])
        self.assertEqual(games[0]["sets"], [[21, 15], [21, 18]])
        self.assertEqual(games[0]["winner"], "home")
        self.assertEqual(games[0]["home"][0], dict(sourceId="1", name="Tom Test", level=7))
        self.assertIsNone(games[0]["away"][0]["level"])
        self.assertEqual(games[1]["outcome"], "AwayForfeit")
        self.assertEqual(info, dict(leader="Lea Leider"))

    def test_ranking_row_never_holds_the_member_number(self):
        parsed = ts.parse_ranking_page(RANKING)
        self.assertEqual((parsed["pages"], parsed["total"]), (4, 301))
        self.assertEqual(parsed["entries"], [dict(rank=2, previousRank=1, sourceId="77", name="Tom Test", club="Test BC", level=3, points=2613)])
        self.assertNotIn("12345678", json.dumps(parsed))
        self.assertEqual(ts.selected_age_group(RANKING), 9)

    def test_ranking_overview_uses_the_first_publication_and_one_category(self):
        self.assertEqual(ts.parse_ranking_overview(OVERVIEW), dict(publicationId="54452", week="44-2026", categories=[dict(id="4582", name="HE/SM")]))

    def test_names_dates_and_youth(self):
        self.assertEqual(ts.display_name("Test, Tom"), "Tom Test")
        self.assertEqual(ts.planned("za 12-9-2026"), ("2026-09-12", None))
        self.assertEqual(ts.planned("za 12-9-2026 19:15"), ("2026-09-12", "2026-09-12T19:15:00+02:00"))
        for name in ["PBA jeugdcompetitie 2026-2027", "Coupe des écoles des jeunes 2026", "Heren U17", "Victor IPJO 2026"]:
            self.assertTrue(ts.YOUTH.search(name), name)
        for name in ["PBA seniorencompetitie 2026-2027", "Heren 1e Provinciale", "Dubbel 2026"]:
            self.assertFalse(ts.YOUTH.search(name), name)


class Safety(unittest.TestCase):
    def fetcher(self, **kw):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        for name in ("CACHE", "SCRAPES"):
            patch = mock.patch.object(ts, name, Path(folder.name))
            patch.start()
            self.addCleanup(patch.stop)
        fetcher = ts.Fetcher(kw.pop("delay", 3), kw.pop("max_requests", 10), **kw)
        fetcher.consented = True
        fetcher.delay = 0
        return fetcher

    def test_delay_has_a_floor(self):
        self.fetcher()
        self.assertEqual(ts.Fetcher(0.1, 1).delay, ts.MIN_DELAY)

    def test_refusal_stops_without_a_retry(self):
        for code in (403, 429, 503):
            fetcher = self.fetcher()
            fetcher.opener = mock.Mock()
            fetcher.opener.open.side_effect = urllib.error.HTTPError("u", code, "no", {}, None)
            with self.assertRaises(ts.StopScrape):
                fetcher.get("/sport/x")
            self.assertEqual(fetcher.opener.open.call_count, 1)

    def test_a_refusal_stops_the_other_runs_too(self):
        refused = self.fetcher()
        refused.opener = mock.Mock()
        refused.opener.open.side_effect = urllib.error.HTTPError("u", 429, "no", {}, None)
        with self.assertRaises(ts.StopScrape):
            refused.get("/sport/x")
        other = ts.Fetcher(3, 10)
        other.consented = True
        other.opener = mock.Mock()
        with self.assertRaises(ts.StopScrape):
            other.get("/sport/y")
        other.opener.open.assert_not_called()

    def test_runs_share_one_pace(self):
        self.fetcher()
        pace, turns = ts.SharedPace(ts.SCRAPES), []
        def worker():
            for _ in range(3):
                pace.turn(0.2)
                turns.append(time.monotonic())
        threads = [threading.Thread(target=worker) for _ in range(3)]
        [t.start() for t in threads]
        [t.join() for t in threads]
        ordered = sorted(turns)
        self.assertEqual(len(ordered), 9)
        self.assertGreater(min(b - a for a, b in zip(ordered, ordered[1:])), 0.15)

    def test_another_run_fetching_the_page_saves_the_request(self):
        fetcher = self.fetcher()
        fetcher.opener = mock.Mock()
        def other_run_fetches_meanwhile(delay):
            other = ts.Fetcher(3, 10)
            other.consented = True
            other.pace.turn = lambda d: None
            other.opener = mock.Mock()
            other.opener.open.return_value.__enter__ = lambda s: types.SimpleNamespace(read=lambda: b"<title>page</title>", geturl=lambda: "u")
            other.opener.open.return_value.__exit__ = lambda *a: False
            other.get("/sport/x")
        fetcher.pace.turn = other_run_fetches_meanwhile
        self.assertEqual(fetcher.get("/sport/x"), "<title>page</title>")
        fetcher.opener.open.assert_not_called()

    def test_folder_per_league_and_free_timestamp(self):
        self.fetcher()
        self.assertEqual(ts.league_folder("VVBBC interclubcompetitie 2026-2027"), "vvbbc-interclubcompetitie-2026-2027")
        self.assertLessEqual(len(ts.league_folder("x" * 90)), 40)
        args = types.SimpleNamespace(sport="badminton", country="BEL")
        moment = datetime(2026, 11, 1, 12, 0, 0)
        first, second = ts.new_folder(ts.SCRAPES / "a", args, moment, False), ts.new_folder(ts.SCRAPES / "a", args, moment, False)
        self.assertEqual((first.name, second.name), ("badminton-bel-20261101-120000", "badminton-bel-20261101-120001"))

    def test_request_cap_stops(self):
        fetcher = self.fetcher(max_requests=0)
        with self.assertRaises(ts.StopScrape):
            fetcher.get("/sport/x")

    def test_offline_sends_nothing(self):
        fetcher = self.fetcher(offline=True)
        fetcher.opener = mock.Mock()
        with self.assertRaises(ts.StopScrape):
            fetcher.get("/sport/x")
        fetcher.opener.open.assert_not_called()

    def run_for(self, pages, **kw):
        args = types.SimpleNamespace(division=None, include=ts.ENTITIES, sport="badminton", country="BEL", ranking_categories=None, ranking_top=kw.get("top", 0), since=None, until=None)
        fetcher = mock.Mock(search_sources=[])
        fetcher.get.side_effect = lambda path, **_: pages(path)
        return ts.Run(args, fetcher)

    def test_youth_entries_are_left_out(self):
        row = lambda rank, pid: f'<tr><td class="rank"><div>{rank}</div></td><td class="rank_equal"></td><td></td><td><a href="player.aspx?id=9&amp;player={pid}">P{pid}</a></td><td>n</td><td>m</td><td>Club</td><td>1</td><td>10</td></tr>'
        def pages(path):
            age = 9 if "CS=9" in path else 0
            body = row(1, 2) if age == 9 else row(1, 1) + row(2, 2) + row(3, 3)
            return f'<select name="a$C1CS"><option selected="selected" value="{age}">x</option></select><table class="ruler">{body}</table>'
        run = self.run_for(pages)
        run.read_ranking(334, dict(publicationId="9", week="44-2026"), dict(id="1", name="HE/SM"))
        self.assertEqual([e["sourceId"] for e in run.rankings], ["1", "3"])
        self.assertEqual(run.excluded_youth, 1)

    def test_category_is_skipped_when_the_youth_filter_did_not_apply(self):
        page = '<select name="a$C1CS"><option selected="selected" value="0">Alles</option></select><table class="ruler"></table>'
        run = self.run_for(lambda path: page)
        run.read_ranking(334, dict(publicationId="9", week="44-2026"), dict(id="1", name="HE/SM"))
        self.assertEqual(run.rankings, [])
        self.assertEqual(len(run.warnings), 1)


if __name__ == "__main__":
    unittest.main()
