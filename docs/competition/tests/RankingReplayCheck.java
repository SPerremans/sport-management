package myfirstmodule;

import com.mendix.thirdparty.org.json.*;
import java.nio.file.*;
import java.util.*;

/**
 * Offline check of the ranking simulation on the frozen public data: every starting point on both data sets, the JSON round trip of a design, and a repeat for determinism.
 * It proves the calculation on real games, not the pages. Usage: java myfirstmodule.RankingReplayCheck resources/ranking-history-public.json
 */
public final class RankingReplayCheck {
    public static void main(String[] args) throws Exception {
        JSONObject resource = new JSONObject(Files.readString(Path.of(args[0]))); JSONArray sets = resource.getJSONArray("datasets"); int failures = 0;
        for (int i = 0; i < sets.length(); i++) {
            JSONObject data = sets.getJSONObject(i); List<RankingEngine.Game> games = RankingWorkbench.readGames(data);
            System.out.println("== " + data.getString("key") + ": " + games.size() + " games, " + data.getJSONArray("players").length() + " people, skipped on capture " + data.getInt("skipped"));
            for (String[] preset : RankingEngine.PRESETS) {
                RankingEngine.Config c = RankingEngine.preset(preset[0], twelveWhenSheetsHaveLevels(data, preset[0])); long start = System.currentTimeMillis();
                RankingEngine.Result r = RankingEngine.replay(games, c); long ms = System.currentTimeMillis() - start;
                JSONObject stored = new JSONObject(); RankingWorkbench.writeEngine(stored, c);
                RankingEngine.Result again = RankingEngine.replay(games, RankingWorkbench.readEngine(new JSONObject(stored.toString())));
                boolean same = r.moves == again.moves && r.favouriteWon == again.favouriteWon && r.players == again.players;
                int atStart = 0, atEnd = 0; for (int l = 1; l <= c.levels(); l++) { atStart += r.startCount[l]; atEnd += r.endCount[l]; }
                boolean ok = same && atStart == r.players && atEnd == r.players && r.players > 0;
                if (!ok) failures++;
                System.out.printf(Locale.ROOT, "%-17s %s players=%d reviews=%d up=%d down=%d better-level-won=%.1f%% (%d decided) brier=%.3f equal=%.1f%% moves/player/yr=%.2f flips=%.1f%% sheet exact=%.1f%% within1=%.1f%% dist=%.2f %dms%n",
                    preset[0], ok ? "ok  " : "FAIL", r.players, r.reviews, r.promoted, r.demoted, r.favouriteRate(), r.decided, r.brierScore(), r.games == 0 ? 0 : 100.0 * r.equalGames / r.games, r.movesPerPlayerYear(), r.flipRate(), r.exactRate(), r.withinOneRate(), r.meanDistance(), ms);
                StringBuilder levels = new StringBuilder("   levels:"); for (int l = 1; l <= c.levels(); l++) levels.append(' ').append(c.name(l)).append('=').append(r.endCount[l]); System.out.println(levels);
                for (String w : r.warnings) System.out.println("   warning: " + w);
            }
        }
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        if (failures > 0) System.exit(1);
    }

    private static String[] twelveWhenSheetsHaveLevels(JSONObject data, String preset) {
        if (!data.getString("key").startsWith("badminton")) return null;
        String[] names = new String[12]; for (int i = 0; i < 12; i++) names[i] = String.valueOf(i + 1); return names;
    }
}
