package myfirstmodule;

import com.mendix.thirdparty.org.json.JSONObject;
import java.nio.file.*;
import java.util.*;

/** Offline: run listing, folder safety and SHA-256 verification of ScrapedRunImport on the fictional example. Needs no runtime; argument: the repository root. */
public final class ScrapedRunFilesCheck {
    private static int checks;
    private static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); checks++; }

    public static void main(String[] args) throws Exception {
        Path example = Path.of(args[0], "resources", ScrapedRunImport.EXAMPLE_RESOURCE);
        ScrapedRunImport.Run run = new ScrapedRunImport.Run(ScrapedRunImport.EXAMPLE_FOLDER, true, example);
        check(run.verified() && !run.partial(), "the example verifies: " + run.problems);
        check(run.rows("matches").length() == 2 && run.rows("rankings").length() == 5, "the example rows read");
        JSONObject match = run.rows("matches").getJSONObject(0);
        check(ScrapedRunImport.sheet(match).getJSONArray("games").getJSONObject(0).getJSONArray("home").getString(0).equals("An Fictief (7)"), "lineup names carry the printed level");

        Path root = Files.createTempDirectory("scraped-runs");
        Files.createDirectory(root.resolve("league-a"));
        Files.createDirectory(root.resolve("rankings"));
        Path good = copy(example, root.resolve("league-a/badminton-bel-20261101-120000"));
        Path older = copy(example, root.resolve("rankings/badminton-bel-20261001-120000-partial"));
        Files.createDirectory(root.resolve("_cache"));
        copy(example, root.resolve("_cache/badminton-bel-20261201-120000"));
        Files.createDirectory(root.resolve("..evil"));
        copy(example, root.resolve("badminton-bel-20261201-120000"));
        check(ScrapedRunImport.folders(root).equals(List.of("league-a/badminton-bel-20261101-120000", "rankings/badminton-bel-20261001-120000-partial")), "only run folders per league, newest first, cache skipped");
        check(ScrapeJob.currentSeason(java.time.LocalDate.of(2026, 7, 1)).equals("2026-2027") && ScrapeJob.currentSeason(java.time.LocalDate.of(2026, 6, 30)).equals("2025-2026"), "the season starts on 1 July");
        check(ScrapeJob.validFilter("badminton", "BEL", "2026-2027") && !ScrapeJob.validFilter("badminton", "BEL", "--all-leagues") && !ScrapeJob.validFilter("a b", "BEL", "2026-2027"), "filters cannot become script options");
        for (String bad : new String[]{"_cache", "..", "../secret", "a/b", "badminton-bel-20261101-120000", "../badminton-bel-20261101-120000", "_cache/badminton-bel-20261101-120000", "league-a/../badminton-bel-20261101-120000", "league-a/badminton-bel-1-2", null}) check(!ScrapedRunImport.validFolder(bad), "rejects folder " + bad);

        Files.writeString(good.resolve("clubs.json"), "[]");
        ScrapedRunImport.Run tampered = new ScrapedRunImport.Run("league-a/badminton-bel-20261101-120000", false, good);
        check(!tampered.verified() && tampered.problems.get(0).startsWith("clubs.json"), "a changed file fails");
        Files.delete(older.resolve("teams.json"));
        check(new ScrapedRunImport.Run("rankings/badminton-bel-20261001-120000-partial", false, older).problems.get(0).startsWith("teams.json"), "a missing file fails");
        System.out.println("PASS: " + checks + " offline checks. This does not exercise Mendix workflows.");
    }

    private static Path copy(Path from, Path to) throws Exception {
        Files.createDirectory(to);
        try (var files = Files.list(from)) { for (Path f : (Iterable<Path>)files::iterator) Files.copy(f, to.resolve(f.getFileName())); }
        return to;
    }
}
