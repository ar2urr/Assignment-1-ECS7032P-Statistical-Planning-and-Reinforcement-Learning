package coursework.validation;

import core.*;
import core.actions.AbstractAction;
import games.GameType;
import games.sushigo.SGGameState;
import org.json.simple.JSONValue;
import players.PlayerFactory;
import players.simple.RandomPlayer;

import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;

/** Tests the exported archive without loading agent classes from the development project. */
public final class SubmissionProbe {
    private static final List<Double> latency = new ArrayList<>();
    private static int games;

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("configuration package outputDirectory");
        Path config = Path.of(args[0]), output = Path.of(args[2]);
        String packageName = args[1];
        Class<?> entry = Class.forName(packageName + ".SubmissionPlayer");
        try (PrintWriter decisions = new PrintWriter(Files.newBufferedWriter(output.resolve("decisions.csv")));
             PrintWriter outcomes = new PrintWriter(Files.newBufferedWriter(output.resolve("games.csv")))) {
            decisions.println("construction,players,seat,decision,round,actions,elapsed_ms");
            outcomes.println("construction,players,seat,game_seed,result,rank,score,decisions");
            for (String mode : List.of("json", "constructor", "copy")) {
                for (int n : List.of(3, 4)) {
                    for (int seat = 0; seat < n; seat++) {
                        long seed = 7032600L + 1000L * List.of("json", "constructor", "copy").indexOf(mode) + n * 100L + seat;
                        AbstractPlayer agent = mode.equals("json") ? PlayerFactory.createPlayer(config.toString())
                                : (AbstractPlayer) entry.getConstructor(long.class).newInstance(seed);
                        if (mode.equals("copy")) agent = agent.copy();
                        if (agent.getClass() != entry) throw new AssertionError("Wrong entry point or copy type");
                        if (agent.getParameters().budget != 400 || agent.getParameters().breakMS != 25)
                            throw new AssertionError("Deployment budget differs from the frozen policy");
                        CheckedPlayer checked = new CheckedPlayer(agent, mode, n, seat, decisions);
                        List<AbstractPlayer> players = new ArrayList<>();
                        for (int p = 0; p < n; p++)
                            players.add(p == seat ? checked : new RandomPlayer(new Random(seed + p)));
                        Game game = GameType.SushiGo.createGameInstance(n, seed);
                        var core = game.getGameState().getCoreGameParameters();
                        core.setParameterValue("competition mode", true);
                        core.setParameterValue("partial observable", true);
                        core.setParameterValue("recordEventHistory", false);
                        core.setParameterValue("disqualify player on illegal action played", true);
                        core.setParameterValue("disqualify player on timeout", true);
                        game.reset(players, seed); game.run();
                        AbstractGameState end = game.getGameState();
                        if (end.isNotTerminal() || checked.count == 0) throw new AssertionError("Incomplete game or no search");
                        for (int p = 0; p < n; p++) {
                            if (!Double.isFinite(end.getGameScore(p)) || !List.of(CoreConstants.GameResult.WIN_GAME,
                                    CoreConstants.GameResult.LOSE_GAME, CoreConstants.GameResult.DRAW_GAME)
                                    .contains(end.getPlayerResults()[p])) throw new AssertionError("Invalid terminal result");
                        }
                        outcomes.printf(Locale.ROOT, "%s,%d,%d,%d,%s,%d,%.3f,%d%n", mode, n, seat, seed,
                                end.getPlayerResults()[seat], end.getOrdinalPosition(seat), end.getGameScore(seat), checked.count);
                        games++;
                        System.out.printf("PASS construction=%s players=%d seat=%d decisions=%d%n", mode, n, seat, checked.count);
                    }
                }
            }
            if (decisions.checkError() || outcomes.checkError()) throw new AssertionError("CSV write failed");
        }
        Collections.sort(latency);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("passed", true); result.put("games", games); result.put("searched_decisions", latency.size());
        result.put("median_ms", percentile(0.5)); result.put("p95_ms", percentile(0.95));
        result.put("max_ms", latency.get(latency.size()-1));
        result.put("over_500ms", latency.stream().filter(x -> x > 500).count());
        result.put("scope", "21 functionality/timing games versus Random, one per seat per construction mode; no strength inference or hard deadline guarantee");
        result.put("measurement", "delegate getAction call; forced singleton moves and external observation/action-list generation excluded; cold first search retained; no warmup games");
        Files.writeString(output.resolve("probe.json"), JSONValue.toJSONString(result) + "\n");
        System.out.println(JSONValue.toJSONString(result));
    }

    private static double percentile(double q) {
        double position = q * (latency.size()-1); int lo = (int) position, hi = (int) Math.ceil(position);
        return latency.get(lo) + (position-lo) * (latency.get(hi)-latency.get(lo));
    }

    private static final class CheckedPlayer extends AbstractPlayer {
        final AbstractPlayer delegate;
        final String mode;
        final int n, seat;
        final PrintWriter decisions;
        int count;
        CheckedPlayer(AbstractPlayer delegate, String mode, int n, int seat, PrintWriter decisions) {
            super(delegate.getParameters(), "Archive validation");
            this.delegate=delegate; this.mode=mode; this.n=n; this.seat=seat; this.decisions=decisions;
        }
        @Override public void initializePlayer(AbstractGameState state) {
            super.initializePlayer(state);
            delegate.setPlayerID(getPlayerID());
            delegate.setForwardModel(getForwardModel());
            delegate.initializePlayer(state);
        }
        @Override public AbstractAction _getAction(AbstractGameState state, List<AbstractAction> actions) {
            long started = System.nanoTime();
            AbstractAction chosen = delegate.getAction(state, actions);
            double ms = (System.nanoTime()-started)/1e6;
            if (!actions.contains(chosen)) throw new AssertionError("Exported agent returned illegal action");
            latency.add(ms); count++;
            decisions.printf(Locale.ROOT,"%s,%d,%d,%d,%d,%d,%.6f%n",mode,n,seat,count,state.getRoundCounter(),actions.size(),ms);
            return chosen;
        }
        @Override public AbstractPlayer copy() { return new CheckedPlayer(delegate.copy(),mode,n,seat,decisions); }
    }
}
