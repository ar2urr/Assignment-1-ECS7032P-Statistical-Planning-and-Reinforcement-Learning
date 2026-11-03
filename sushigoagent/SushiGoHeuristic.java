package sushigoagent;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;
import games.sushigo.cards.SGCard.SGCardType;

import java.util.EnumMap;

/**
 * Leaf evaluation based on awarded points and discounted public scoring prospects.
 * Set and Wasabi estimates use a coarse independent-draw model, not calibrated
 * draft probabilities. See docs/STAGE_3.md for assumptions and ablations.
 */
public class SushiGoHeuristic extends TunableParameters<SushiGoHeuristic> implements IStateHeuristic {
    private double setWeight = 1.0;
    private double wasabiWeight = 1.0;
    private double makiWeight = 1.0;
    private double puddingWeight = 1.0;

    public SushiGoHeuristic() {
        addTunableParameter("setWeight", 1.0);
        addTunableParameter("wasabiWeight", 1.0);
        addTunableParameter("makiWeight", 1.0);
        addTunableParameter("puddingWeight", 1.0);
        _reset();
    }

    @Override
    public void _reset() {
        setWeight = weight("setWeight");
        wasabiWeight = weight("wasabiWeight");
        makiWeight = weight("makiWeight");
        puddingWeight = weight("puddingWeight");
    }

    private double weight(String name) {
        double value = ((Number) getParameterValue(name)).doubleValue();
        if (!Double.isFinite(value) || value < 0)
            throw new IllegalArgumentException(name + " must be finite and nonnegative");
        return value;
    }

    @Override
    public double evaluateState(AbstractGameState gameState, int playerId) {
        if (!(gameState instanceof SGGameState state))
            throw new IllegalArgumentException("SushiGoHeuristic requires a Sushi Go state");
        if (playerId < 0 || playerId >= state.getNPlayers())
            throw new IllegalArgumentException("Invalid player index: " + playerId);
        // End-of-round/game awards are already included before TAG declares a result.
        if (!state.isNotTerminal()) return state.getPlayerResults()[playerId].value;

        SGParameters params = (SGParameters) state.getGameParameters();
        int horizon = state.getPlayerHands().get(playerId).getSize();
        int initialHand = Math.max(1, params.nCards - state.getNPlayers() + 2);
        double roundProgress = clamp(1.0 - (double) horizon / initialHand);
        double gameProgress = clamp((state.getRoundCounter() + roundProgress) / Math.max(1, params.nRounds));
        EnumMap<SGCardType, Integer> remaining = publicRemainingCards(state, params);
        int pool = remaining.values().stream().mapToInt(Integer::intValue).sum();

        double sets = setPotential(state, playerId, SGCardType.Tempura, 2,
                params.valueTempuraPair, horizon, remaining, pool)
                + setPotential(state, playerId, SGCardType.Sashimi, 3,
                params.valueSashimiTriple, horizon, remaining, pool);
        double wasabi = wasabiPotential(state, playerId, params, horizon, remaining, pool);
        // Nigiri, Dumplings and completed sets have already contributed to gameScore.
        return (state.getGameScore(playerId) + setWeight * sets + wasabiWeight * wasabi
                + makiWeight * roundProgress * projectedMaki(state, playerId)
                + puddingWeight * gameProgress * projectedPudding(state, playerId)) / 50.0;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static EnumMap<SGCardType, Integer> publicRemainingCards(SGGameState state, SGParameters params) {
        EnumMap<SGCardType, Integer> remaining = new EnumMap<>(SGCardType.class);
        for (SGCardType type : SGCardType.values()) remaining.put(type, 0);
        params.nCardsPerType.forEach((key, count) -> remaining.merge(key.a, count, Integer::sum));
        // Count physical cards here; a three-icon Maki card removes one card, not three.
        // Discards from earlier rounds and hidden hands are deliberately not inspected.
        for (var tableau : state.getPlayedCards()) {
            for (SGCard card : tableau.getComponents()) {
                remaining.compute(card.type, (type, count) -> Math.max(0, count - 1));
            }
        }
        return remaining;
    }

    private static double setPotential(SGGameState state, int playerId, SGCardType type,
                                       int size, int award, int horizon,
                                       EnumMap<SGCardType, Integer> remaining, int pool) {
        int progress = state.getPlayedCardTypes(type, playerId).getValue() % size;
        int needed = size - progress;
        if (progress == 0 || horizon < needed || remaining.get(type) < needed || pool == 0) return 0;
        double chance = binomialTail(horizon, (double) remaining.get(type) / pool, needed);
        // Credit only the fraction represented by cards already committed to this set.
        return award * ((double) progress / size) * chance;
    }

    private static double wasabiPotential(SGGameState state, int playerId, SGParameters params,
                                          int horizon, EnumMap<SGCardType, Integer> remaining, int pool) {
        int slots = Math.min(horizon, state.getPlayedCardTypes(SGCardType.Wasabi, playerId).getValue());
        int squid = remaining.get(SGCardType.SquidNigiri);
        int salmon = remaining.get(SGCardType.SalmonNigiri);
        int egg = remaining.get(SGCardType.EggNigiri);
        int nigiri = squid + salmon + egg;
        if (slots == 0 || nigiri == 0 || pool == 0) return 0;
        slots = Math.min(slots, nigiri);
        double average = (double) (squid * params.valueSquidNigiri + salmon * params.valueSalmonNigiri
                + egg * params.valueEggNigiri) / nigiri;
        double expectedFilled = 0;
        for (int i = 1; i <= slots; i++) expectedFilled += binomialTail(horizon, (double) nigiri / pool, i);
        // Only the multiplier's extra points are credited; the base Nigiri score is excluded.
        return (params.multiplierWasabi - 1) * average * expectedFilled;
    }

    /** Independent-draw approximation P(X >= needed); final bucket is absorbing. */
    private static double binomialTail(int trials, double probability, int needed) {
        if (trials < needed) return 0;
        double[] mass = new double[needed + 1];
        mass[0] = 1;
        for (int i = 0; i < trials; i++) {
            mass[needed] += mass[needed - 1] * probability;
            for (int j = needed - 1; j > 0; j--)
                mass[j] = mass[j] * (1 - probability) + mass[j - 1] * probability;
            mass[0] *= 1 - probability;
        }
        return clamp(mass[needed]);
    }

    /** Current Maki award if the round ended now, matching TAG's integer tie splits. */
    static int projectedMaki(SGGameState state, int playerId) {
        SGParameters params = (SGParameters) state.getGameParameters();
        int most = 0, second = 0, firstTies = 0, secondTies = 0;
        for (int p = 0; p < state.getNPlayers(); p++) {
            int count = state.getPlayedCardTypes(SGCardType.Maki, p).getValue();
            if (count > most) { second = most; most = count; }
            else if (count < most && count > second) second = count;
        }
        for (int p = 0; p < state.getNPlayers(); p++) {
            int count = state.getPlayedCardTypes(SGCardType.Maki, p).getValue();
            if (count == most) firstTies++;
            if (count == second) secondTies++;
        }
        int own = state.getPlayedCardTypes(SGCardType.Maki, playerId).getValue();
        if (most > 0 && own == most) return params.valueMakiMost / firstTies;
        if (firstTies == 1 && second > 0 && own == second) return params.valueMakiSecond / secondTies;
        return 0;
    }

    /** Current Pudding award if the game ended now; two-player games have no penalty. */
    static int projectedPudding(SGGameState state, int playerId) {
        SGParameters params = (SGParameters) state.getGameParameters();
        int most = Integer.MIN_VALUE, least = Integer.MAX_VALUE, firstTies = 0, lastTies = 0;
        for (int p = 0; p < state.getNPlayers(); p++) {
            int count = state.getPlayedCardTypes(SGCardType.Pudding, p).getValue();
            most = Math.max(most, count);
            least = Math.min(least, count);
        }
        if (most == least) return 0;
        for (int p = 0; p < state.getNPlayers(); p++) {
            int count = state.getPlayedCardTypes(SGCardType.Pudding, p).getValue();
            if (count == most) firstTies++;
            if (count == least) lastTies++;
        }
        int own = state.getPlayedCardTypes(SGCardType.Pudding, playerId).getValue();
        if (own == most) return params.valuePuddingMost / firstTies;
        if (own == least && state.getNPlayers() > 2) return params.valuePuddingLeast / lastTies;
        return 0;
    }

    // Keep TAG's score/50 scale for the ablation; it is not bounded to [-1, 1].
    @Override public double minValue() { return Double.NEGATIVE_INFINITY; }
    @Override public double maxValue() { return Double.POSITIVE_INFINITY; }
    @Override public SushiGoHeuristic instantiate() { return (SushiGoHeuristic) copy(); }
    @Override protected SushiGoHeuristic _copy() { return new SushiGoHeuristic(); }
    @Override protected boolean _equals(Object other) {
        return other instanceof SushiGoHeuristic h && setWeight == h.setWeight
                && wasabiWeight == h.wasabiWeight && makiWeight == h.makiWeight
                && puddingWeight == h.puddingWeight;
    }
}
