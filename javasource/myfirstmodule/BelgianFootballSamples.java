package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Belgian football divisions without a published schedule, plus fictional team-sheet examples. Called only inside DevelopmentSamples' authorized transaction. */
final class BelgianFootballSamples {
    static final String FEATURE = "features:football-team-sheets-v1";
    static final String PREFIX = "DEMO-RBFA-2026:";
    private BelgianFootballSamples() {}

    /** Public team lists for 2026-2027 (Wikipedia/Voetbal Vlaanderen listings consulted 27 October 2026); one league per division, one poule per series. */
    private static final String[][] LEAGUES = {
        {"Nationale 1 2026-2027", "Men", "VV", "Belisia|Dessel|Harelbeke|Heist|Hoogstraten|Houtvenne|Jong Cercle=Cercle Brugge|Knokke|Lyra-Lierse|Mandel United|Merelbeke|OH Leuven U23=OH Leuven|Roeselare|Thes Sport|Tienen|Zelzate",
            "FFA", "Flenu|Habay-la-Neuve|Meux|RAEC Mons|Olympic Charleroi|Onhaye|Rochefort|RWDM Brussels|SL16 FC U23=Standard de Liège|Tubize-Braine|Union SG B U23=Royale Union Saint-Gilloise|Zebra Elites U23=Sporting Charleroi"},
        {"Super League Vrouwenvoetbal 2026-2027", "Women", "Super League", "RSC Anderlecht=RSC Anderlecht|KRC Genk Ladies=KRC Genk|AA Gent Ladies=KAA Gent|Les Louviéroises|Club YLA=Club Brugge|OH Leuven=OH Leuven|Standard Femina=Standard de Liège|SV Zulte Waregem=SV Zulte Waregem"},
        {"Eerste Nationale Vrouwen 2026-2027", "Women", "Eerste Nationale", "Stade Mouscronnois|KFC Damme|OH Leuven B=OH Leuven|KEWS Schoonbeek-Beverst|K. Olsa Brakel|RSC Anderlecht B=RSC Anderlecht|Famkes Diksmuide Oostende|KRC Genk B=KRC Genk|Herk Sport Hasselt|Club Brugge B=Club Brugge|KV Mechelen=KV Mechelen|Standard Liège B=Standard de Liège|SV Zulte Waregem B=SV Zulte Waregem|FC Femina White Star Woluwe|KVC Westerlo=KVC Westerlo|RAEC Mons Ladies=RAEC Mons"},
        {"Interprovinciale Vrouwen 2026-2027", "Women", "Interprovinciale A", "KSK Beveren|FC Bosdam Beveren|KVE Drongen|Dames VK Egem|KAA Gent B=KAA Gent|KdNS Heule|KVV Kester-Gooik|KVK Svelta Melsele|KFC Merelbeke Ladies=Merelbeke|VK Ninove|RWDM Girls=RWDM Brussels|HO Wolvertem Merchtem",
            "Interprovinciale B", "Bocholter VV|KFC Diepenbeek|Sporting Hasselt Ladies=Sporting Hasselt|KFC Herent|Kontich FC|Loenhout SK|Excelsior Mariaburg|KV Mechelen B=KV Mechelen|VC Moldavo|Noorse SV|WS Schoonbeek Bilzen B=KEWS Schoonbeek-Beverst|KVK Tienen Ladies=Tienen",
            "Interprovinciale FFA", "US Assenois|FC Irlande Auderghem|BX Brussels|Sporting Charleroi Ladies=Sporting Charleroi|EJ Fléron|RAAL La Louvière B=RAAL La Louvière|RUS Loyers|RFC Liège Ladies=RFC Liège|Ostiches|CS Sart-Tilman|Union SG Ladies=Royale Union Saint-Gilloise|FC Lions Leopold Uccle|FC Femina White Star Woluwe B=FC Femina White Star Woluwe"}
    };
    private static final String[] GIVEN = {"Lena","Noor","Jules","Mila","Arne","Lotte","Bram","Elise","Wout","Fien","Senne","Hanne","Lars","Ines","Tibo","Jade","Milan","Sara"};

    static void populate(IContext ctx, DevelopmentSamples.Seed s, Set<Long> owned, Set<Long> publicOrganizations) throws Exception {
        configureExistingFormats(ctx, owned);
        IMendixObject fed = s.make("Federation", "Name", "[DEMO] Royal Belgian Football Association", "Sport", "Football", "IsActive", true, "Currency", "EUR",
            "CensusMonth", 1L, "CensusDay", 1L, "BillingMethod", "ACTIVE_AT_CENSUS",
            "Description", "Local development copy of public 2026-2027 division names. No published schedules or results are imported; the team-sheet fixtures are fictional.");
        publicOrganizations.add(fed.getId().toLong());
        IMendixObject format = s.make("MatchFormat"); MatchRegistrationService.defaults(ctx, format);
        format.setValue(ctx, "Name", "[DEMO] Football RBFA national 2026-2027"); format.setValue(ctx, "Sport", "Football"); format.setValue(ctx, "Scoring", "Total");
        format.setValue(ctx, "WinPoints", BigDecimal.valueOf(3)); format.setValue(ctx, "StopWhenDecided", false);
        format.setValue(ctx, "MaxStarters", 11L); format.setValue(ctx, "MaxReserves", 9L);
        format.setValue(ctx, "AvailableFrom", day("2026-07-01")); format.setValue(ctx, "AvailableUntil", day("2027-06-30"));
        for (String check : Arrays.asList("CheckMembership","CheckRegistration","CheckSuspension","CheckDiscipline","CheckAppearances","CheckIndex")) format.setValue(ctx, check, false);
        Core.commitWithoutEvents(ctx, format); s.link(format, "MatchFormat_Federation", fed);
        s.link(s.make("MatchSlot", "Name", "Match", "Sequence", 1, "PlayersPerSide", 0, "Discipline", "Open"), "MatchSlot_Format", format);

        Map<String,IMendixObject> clubs = new HashMap<>();
        // Reuse only owned clubs already playing football, so a same-named badminton club is never matched.
        for (IMendixObject club : Core.createXPathQuery("//MyFirstModule.Club[starts-with(Name,'[DEMO] ')][MyFirstModule.CompetitionTeam_Club/MyFirstModule.CompetitionTeam/MyFirstModule.LeagueEntry_Team/MyFirstModule.LeagueEntry/MyFirstModule.LeagueEntry_League/MyFirstModule.CompetitionLeague/Sport='Football']").execute(ctx))
            if (owned.contains(club.getId().toLong())) clubs.putIfAbsent(club.getValue(ctx, "Name"), club);
        Set<Long> affiliated = new HashSet<>();
        for (String[] spec : LEAGUES) {
            IMendixObject league = s.make("CompetitionLeague", "Name", "[DEMO] " + spec[0], "Sport", "Football", "SeasonName", "2026-2027",
                "StartsOn", day("2026-08-15"), "EndsOn", day("2027-05-31"), "RegistrationOpens", day("2026-05-01"), "RegistrationCloses", day("2026-06-30"),
                "ChangesOpen", day("2026-08-01"), "ChangesClose", day("2027-05-15"), "RequestedPoules", (spec.length - 2) / 2, "MaxTeamsPerPoule", 16L, "DurationMinutes", 120L,
                "Format", "DoubleRoundRobin", "Stage", "Published", "ResultSettingsConfigured", true, "UseGameBreakdown", false, "PointsPerGame", false,
                "WinPoints", BigDecimal.valueOf(3), "DrawPoints", BigDecimal.ONE, "LossPoints", BigDecimal.ZERO,
                "Info", "[DEMO] " + spec[1] + "'s division with the public 2026-2027 team list. The schedule is not imported: use Generate fixtures. Dates are demo defaults. The single team-sheet fixture and all player names are fictional.");
            s.link(league, "CompetitionLeague_Federation", fed); s.link(league, "CompetitionLeague_MatchFormat", format);
            List<IMendixObject> firstPoule = null; int number = 0;
            for (int g = 2; g < spec.length; g += 2) {
                String[] teams = spec[g + 1].split("\\|");
                IMendixObject poule = s.make("CompetitionPoule", "Name", spec[g], "Number", ++number, "TeamCount", teams.length);
                s.link(poule, "CompetitionPoule_League", league);
                List<IMendixObject> entries = new ArrayList<>(); int order = 0;
                for (String item : teams) {
                    String teamName = item.contains("=") ? item.substring(0, item.indexOf('=')) : item, clubName = "[DEMO] " + (item.contains("=") ? item.substring(item.indexOf('=') + 1) : item);
                    IMendixObject club = clubs.get(clubName);
                    if (club == null) {
                        club = s.make("Club", "Name", clubName, "Description", "Public club name; local development copy. No real accounts or contact details are imported.",
                            "IsPublished", true, "LocationVerified", false, "RequiresApproval", true, "RequiresPayment", false);
                        clubs.put(clubName, club); publicOrganizations.add(club.getId().toLong());
                    }
                    if (affiliated.add(club.getId().toLong())) {
                        IMendixObject affiliation = s.make("FederationAffiliation", "ClubName", club.getValue(ctx, "Name"), "StartsOn", day("2026-07-01"), "EndsOn", day("2027-06-30"), "IsActive", true, "Status", "Approved");
                        s.link(affiliation, "FederationAffiliation_Club", club); s.link(affiliation, "FederationAffiliation_Federation", fed);
                    }
                    IMendixObject team = s.make("CompetitionTeam", "Name", teamName, "ClubName", club.getValue(ctx, "Name"), "SeasonName", "2026-2027", "IsActive", true);
                    s.link(team, "CompetitionTeam_Club", club);
                    IMendixObject entry = s.make("LeagueEntry", "TeamName", teamName, "ClubName", club.getValue(ctx, "Name"), "ClubKey", s.id(club),
                        "EntryKey", PREFIX + s.marker + ":" + spec[0] + ":" + spec[g] + ":" + order, "Order", ++order);
                    s.link(entry, "LeagueEntry_Team", team); s.link(entry, "LeagueEntry_Club", club); s.link(entry, "LeagueEntry_League", league); s.link(entry, "LeagueEntry_Poule", poule);
                    entries.add(entry);
                }
                if (firstPoule == null) { firstPoule = entries; teamSheetFixture(ctx, s, league, poule, format, entries.get(0), entries.get(1), spec[1].equals("Men")); }
            }
        }
        badmintonReserve(ctx, s, owned);
    }

    /** Owned demo formats keep any configured limits; only untouched zero limits receive a sport default. */
    private static void configureExistingFormats(IContext ctx, Set<Long> owned) throws Exception {
        for (IMendixObject format : Core.createXPathQuery("//MyFirstModule.MatchFormat[MaxStarters=0][MaxReserves=0]").execute(ctx)) {
            if (!owned.contains(format.getId().toLong())) continue;
            String sport = Objects.toString(format.getValue(ctx, "Sport"), "");
            if (sport.equals("Football")) { format.setValue(ctx, "MaxStarters", 11L); format.setValue(ctx, "MaxReserves", 9L); }
            else if (sport.equals("Badminton")) format.setValue(ctx, "MaxReserves", 2L);
            else continue;
            Core.commitWithoutEvents(ctx, format);
        }
    }

    /** A kicked-off, unscored fixture: full sheets per side and logged substitutions. It never counts in standings. */
    private static void teamSheetFixture(IContext ctx, DevelopmentSamples.Seed s, IMendixObject league, IMendixObject poule, IMendixObject format, IMendixObject home, IMendixObject away, boolean men) throws Exception {
        IMendixObject hc = Core.retrieveId(ctx, home.getValue(ctx, "MyFirstModule.LeagueEntry_Club")), ac = Core.retrieveId(ctx, away.getValue(ctx, "MyFirstModule.LeagueEntry_Club"));
        IMendixObject fixture = s.make("CompetitionFixture", "HomeName", home.getValue(ctx, "TeamName"), "AwayName", away.getValue(ctx, "TeamName"),
            "HomeKey", s.id(home), "AwayKey", s.id(away), "HomeClubKey", s.id(hc), "AwayClubKey", s.id(ac), "StartsAt", s.hour(-1, 19), "EndsAt", s.hour(-1, 21),
            "Status", "Published", "Version", 1L, "Leg", 1L, "PairKey", PREFIX + s.marker + ":" + s.id(league), "Issue", "[DEMO] Fictional team-sheet example; not a published result");
        s.link(fixture, "CompetitionFixture_League", league); s.link(fixture, "CompetitionFixture_Poule", poule);
        s.link(fixture, "CompetitionFixture_Home", home); s.link(fixture, "CompetitionFixture_Away", away);
        s.link(fixture, "CompetitionFixture_HomeClub", hc); s.link(fixture, "CompetitionFixture_AwayClub", ac);
        IMendixObject reg = s.make("MatchRegistration", "State", "Started", "Revision", 1L, "StartedAt", s.hour(-1, 19), "WarningsAcknowledged", true,
            "Reason", "[DEMO] Fictional team sheets and substitutions; no score entered");
        s.link(reg, "MatchRegistration_Fixture", fixture); s.link(reg, "MatchRegistration_Format", format);
        int[][] subs = {{58, 72}, {64}};
        for (int side = 0; side < 2; side++) {
            IMendixObject entry = side == 0 ? home : away, team = Core.retrieveId(ctx, entry.getValue(ctx, "MyFirstModule.LeagueEntry_Team"));
            String sideName = side == 0 ? "Home" : "Away";
            List<IMendixObject> sheet = new ArrayList<>();
            for (int n = 0; n < 18; n++) {
                String name = "[DEMO] " + GIVEN[(n + side * 7) % GIVEN.length] + " " + (men ? "Speler" : "Speelster") + " " + (n + 1) + " (" + entry.getValue(ctx, "TeamName") + ")";
                IMendixObject player = s.make("CompetitionPlayer", "MemberName", name, "MemberKey", PREFIX + s.marker + ":" + s.id(team) + ":" + n, "PersonKey", PREFIX + s.marker + ":" + s.id(team) + ":" + n);
                s.link(player, "CompetitionPlayer_Team", team);
                IMendixObject member = s.make("MatchSquadPlayer", "Side", sideName, "Position", n < 11 ? "Starter" : "Reserve", "PlayerName", name, "Sequence", n < 11 ? n + 1 : n - 10);
                s.link(member, "MatchSquadPlayer_Registration", reg); s.link(member, "MatchSquadPlayer_Player", player); sheet.add(player);
            }
            for (int i = 0; i < subs[side].length; i++) {
                IMendixObject off = sheet.get(10 - i), on = sheet.get(11 + i);
                IMendixObject sub = s.make("MatchSubstitution", "Side", sideName, "Minute", subs[side][i], "OffName", off.getValue(ctx, "MemberName"), "OnName", on.getValue(ctx, "MemberName"),
                    "Reason", "[DEMO] Tactical substitution", "RecordedBy", "Local development generator", "RecordedAt", s.hour(-1, 20));
                s.link(sub, "MatchSubstitution_Registration", reg); s.link(sub, "MatchSubstitution_Off", off); s.link(sub, "MatchSubstitution_On", on);
            }
        }
        IMendixObject audit = s.make("MatchAudit", "Action", "Development example", "Actor", "Local development generator", "At", new Date(),
            "Reason", "Fictional team sheets (11 starters, 7 reserves) and substitutions. Seeded data is not runtime test evidence.", "AfterValue", "Team sheets and substitution log");
        s.link(audit, "MatchAudit_Registration", reg); s.link(audit, "MatchAudit_League", league); s.link(audit, "MatchAudit_Federation", Core.retrieveId(ctx, league.getValue(ctx, "MyFirstModule.CompetitionLeague_Federation")));
    }

    /** Badminton reserve lists are rarely filled; one demo reserve shows the case on the owned match-registration example. */
    private static void badmintonReserve(IContext ctx, DevelopmentSamples.Seed s, Set<Long> owned) throws Exception {
        for (IMendixObject reg : Core.createXPathQuery("//MyFirstModule.MatchRegistration[MyFirstModule.MatchRegistration_Format/MyFirstModule.MatchFormat/Sport='Badminton'][State='Draft']").execute(ctx)) {
            if (!owned.contains(reg.getId().toLong())) continue;
            IMendixObject fixture = Core.retrieveId(ctx, reg.getValue(ctx, "MyFirstModule.MatchRegistration_Fixture"));
            IMendixObject entry = Core.retrieveId(ctx, fixture.getValue(ctx, "MyFirstModule.CompetitionFixture_Home"));
            IMendixObject team = Core.retrieveId(ctx, entry.getValue(ctx, "MyFirstModule.LeagueEntry_Team"));
            IMendixObject player = s.make("CompetitionPlayer", "MemberName", "[DEMO] Reserve player", "MemberKey", PREFIX + s.marker + ":reserve:" + s.id(team), "PersonKey", PREFIX + s.marker + ":reserve:" + s.id(team));
            s.link(player, "CompetitionPlayer_Team", team);
            IMendixObject member = s.make("MatchSquadPlayer", "Side", "Home", "Position", "Reserve", "PlayerName", "[DEMO] Reserve player", "Sequence", 1);
            s.link(member, "MatchSquadPlayer_Registration", reg); s.link(member, "MatchSquadPlayer_Player", player);
            return;
        }
    }

    private static Date day(String value) { return Date.from(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant()); }
}
