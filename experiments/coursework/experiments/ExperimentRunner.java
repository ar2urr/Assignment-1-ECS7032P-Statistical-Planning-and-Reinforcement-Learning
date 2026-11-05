package coursework.experiments;

import core.*;
import core.actions.AbstractAction;
import games.GameType;
import games.sushigo.SGGameState;
import org.json.simple.JSONValue;
import players.PlayerFactory;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import sushigoagent.BasicMCTSParams;
import sushigoagent.BasicMCTSPlayer;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Runs experiment games outside the agent package submitted for marking. */
public final class ExperimentRunner {
    public static final List<String> VARIANTS = List.of("baseline", "heuristic", "multiplayer", "combined",
            "combined_no_sets", "combined_no_wasabi", "combined_no_maki", "combined_no_pudding");
    public static final List<String> OPPONENTS = List.of("random", "osla", "baseline");

    public static int seatFor(int index, int players) { return index % players; }

    public static double winShare(AbstractGameState state, int seat) {
        if (state.getOrdinalPosition(seat) != 1) return 0;
        int winners = 0;
        for (int p = 0; p < state.getNPlayers(); p++) if (state.getOrdinalPosition(p) == 1) winners++;
        return 1.0 / winners;
    }

    public static long gameSeed(long master, int cell, int index) {
        // Give each cell its own seed stream for the unpaired comparisons.
        return new SplittableRandom(master + 1_000_003L * cell + 104_729L * index).nextLong();
    }

    private static Path config(Path project, String variant, String mode) {
        return variant.startsWith("combined_no_")
                ? project.resolve("experiments/configs/" + variant + ".json")
                : project.resolve("sushigoagent/" + variant + "_" + mode + ".json");
    }

    private static BasicMCTSParams parameters(Path project, String variant, String mode, int seed) {
        BasicMCTSPlayer player = (BasicMCTSPlayer) PlayerFactory.createPlayer(config(project, variant, mode).toString());
        player.getParameters().setParameterValue("randomSeed", seed);
        return player.getParameters();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 8) throw new IllegalArgumentException(
                "project outputDirectory variant opponent players games masterSeed iterations|500ms");
        Path project = Path.of(args[0]).toAbsolutePath();
        Path output = Path.of(args[1]).toAbsolutePath();
        String variant = args[2], opponent = args[3], mode = args[7];
        int n = Integer.parseInt(args[4]), games = Integer.parseInt(args[5]);
        long master = Long.parseLong(args[6]);
        if (!VARIANTS.contains(variant) || !OPPONENTS.contains(opponent) || !List.of(3, 4).contains(n)
                || games <= 0 || games % n != 0 || !List.of("iterations", "500ms").contains(mode)
                || (mode.equals("500ms") && variant.startsWith("combined_no_")))
            throw new IllegalArgumentException("Invalid or unbalanced experiment cell");
        Files.createDirectories(output);
        String id = mode + "_" + n + "p_" + variant + "_vs_" + opponent;
        Path csv = output.resolve(id + ".csv"), partial = output.resolve(id + ".partial.csv");
        if (Files.exists(csv) || Files.exists(partial)) throw new IOException("Cell output already exists: " + id);
        int cell = (mode.equals("500ms") ? 100 : 0) + n * 24 + VARIANTS.indexOf(variant) * 3 + OPPONENTS.indexOf(opponent);
        long started = System.nanoTime();
        String startTime = Instant.now().toString();
        // Warm up with iteration budgets and keep these games out of the results.
        for (int i = 0; i < 8; i++) play(project, variant, opponent, n, i, gameSeed(master, cell, -100 - i),
                "iterations", null, null);
        Path decisionPath = output.resolve(id + ".decisions.partial.csv");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(partial, StandardOpenOption.CREATE_NEW));
             PrintWriter decisions = mode.equals("500ms")
                     ? new PrintWriter(Files.newBufferedWriter(decisionPath, StandardOpenOption.CREATE_NEW)) : null) {
            writer.println("variant,opponent,mode,players,game_index,game_seed,agent_seed,seat,result,rank,score,win_share,score_gap,decisions,iterations,fm_calls,copy_calls,total_decision_ms,max_decision_ms,over_500ms,game_wall_ms,player_seeds,all_scores,all_ranks");
            if (decisions != null) decisions.println("variant,players,game_index,seat,decision,round,hand_size,actions,elapsed_ms,iterations,fm_calls,copy_calls");
            for (int i = 0; i < games; i++) {
                play(project, variant, opponent, n, i, gameSeed(master, cell, i), mode, writer, decisions);
                writer.flush();
                if (writer.checkError() || decisions != null && decisions.checkError())
                    throw new IOException("Result write failed for " + id);
                if ((i + 1) % 50 == 0 || i + 1 == games)
                    System.out.printf(Locale.ROOT, "%s %d/%d elapsed_s=%.1f%n", id, i + 1, games,
                            (System.nanoTime() - started) / 1e9);
            }
        }
        Files.move(partial, csv);
        if (mode.equals("500ms")) Files.move(decisionPath, output.resolve(id + ".decisions.csv"));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("variant", variant); metadata.put("opponent", opponent); metadata.put("players", n);
        metadata.put("games", games); metadata.put("master_seed", master); metadata.put("mode", mode);
        metadata.put("started_utc", startTime); metadata.put("finished_utc", Instant.now().toString());
        metadata.put("wall_seconds", (System.nanoTime() - started) / 1e9);
        metadata.put("warmup_games", 8); metadata.put("max_heap_bytes", Runtime.getRuntime().maxMemory());
        Files.writeString(output.resolve(id + ".json"), JSONValue.toJSONString(metadata) + "\n", StandardOpenOption.CREATE_NEW);
    }

    private static void play(Path project, String variant, String opponent, int n, int index, long seed,
                             String mode, PrintWriter writer, PrintWriter decisionWriter) {
        int seat = seatFor(index, n);
        SplittableRandom seedStream = new SplittableRandom(seed ^ 0x5DEECE66DL);
        int[] seeds = new int[n];
        for (int p = 0; p < n; p++) seeds[p] = seedStream.nextInt();
        MeasuredPlayer focal = new MeasuredPlayer(parameters(project, variant, mode, seeds[seat]),
                variant, n, index, seat, decisionWriter);
        List<AbstractPlayer> agents = new ArrayList<>();
        for (int p = 0; p < n; p++) {
            if (p == seat) agents.add(focal);
            else agents.add(switch (opponent) {
                case "random" -> new RandomPlayer(new Random(seeds[p]));
                case "osla" -> new OSLAPlayer(new Random(seeds[p]));
                case "baseline" -> parameters(project, "baseline", mode, seeds[p]).instantiate();
                default -> throw new IllegalArgumentException(opponent);
            });
        }
        Game game = GameType.SushiGo.createGameInstance(n, seed);
        var core = game.getGameState().getCoreGameParameters();
        core.setParameterValue("competition mode", true);
        core.setParameterValue("recordEventHistory", false);
        core.setParameterValue("partial observable", true);
        core.setParameterValue("disqualify player on illegal action played", true);
        core.setParameterValue("disqualify player on timeout", true);
        game.reset(agents, seed);
        long started = System.nanoTime();
        game.run();
        double gameMillis = (System.nanoTime() - started) / 1e6;
        AbstractGameState finalState = game.getGameState();
        if (finalState.isNotTerminal()) throw new IllegalStateException("Incomplete game");
        double[] scores = new double[n];
        int[] ranks = new int[n];
        double strongestOther = Double.NEGATIVE_INFINITY;
        for (int p = 0; p < n; p++) {
            var result = finalState.getPlayerResults()[p];
            if (!List.of(CoreConstants.GameResult.WIN_GAME, CoreConstants.GameResult.LOSE_GAME,
                    CoreConstants.GameResult.DRAW_GAME).contains(result))
                throw new IllegalStateException("Invalid game result " + result);
            scores[p] = finalState.getGameScore(p);
            ranks[p] = finalState.getOrdinalPosition(p);
            if (!Double.isFinite(scores[p])) throw new IllegalStateException("Nonfinite score");
            if (p != seat) strongestOther = Math.max(strongestOther, scores[p]);
        }
        if (writer != null) row(writer, variant, opponent, mode, n, index, seed, seeds[seat], seat,
                finalState.getPlayerResults()[seat], ranks[seat], scores[seat], winShare(finalState, seat),
                scores[seat] - strongestOther, focal.decisions, focal.iterations, focal.fmCalls, focal.copyCalls,
                focal.totalNanos / 1e6, focal.maxNanos / 1e6, focal.over500, gameMillis,
                Arrays.toString(seeds), Arrays.toString(scores), Arrays.toString(ranks));
    }

    private static void row(PrintWriter writer, Object... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) writer.print(',');
            String value = String.valueOf(fields[i]);
            writer.print('"'); writer.print(value.replace("\"", "\"\"")); writer.print('"');
        }
        writer.println();
    }

    private static final class MeasuredPlayer extends BasicMCTSPlayer {
        final String variant;
        final int n, gameIndex, seat;
        final PrintWriter writer;
        long decisions, iterations, fmCalls, copyCalls, totalNanos, maxNanos, over500;
        MeasuredPlayer(BasicMCTSParams params, String variant, int n, int gameIndex, int seat, PrintWriter writer) {
            super(params); this.variant = variant; this.n = n; this.gameIndex = gameIndex;
            this.seat = seat; this.writer = writer;
        }
        @Override public AbstractAction _getAction(AbstractGameState state, List<AbstractAction> actions) {
            long start = System.nanoTime();
            AbstractAction selected = super._getAction(state, actions);
            long duration = System.nanoTime() - start;
            if (!actions.contains(selected)) throw new IllegalStateException("Illegal focal action");
            SearchStats stats = getLastSearchStats();
            decisions++; iterations += stats.iterations(); fmCalls += stats.fmCalls(); copyCalls += stats.copyCalls();
            totalNanos += duration; maxNanos = Math.max(maxNanos, duration);
            if (duration > 500_000_000L) over500++;
            if (writer != null) row(writer, variant, n, gameIndex, seat, decisions, state.getRoundCounter(),
                    ((SGGameState) state).getPlayerHands().get(seat).getSize(), actions.size(), duration / 1e6,
                    stats.iterations(), stats.fmCalls(), stats.copyCalls());
            return selected;
        }
    }
}
