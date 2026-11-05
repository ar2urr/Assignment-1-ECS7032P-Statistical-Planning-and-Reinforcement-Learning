package sushigoagent;

import core.*;
import games.GameType;
import games.sushigo.SGForwardModel;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;
import games.sushigo.cards.SGCard.SGCardType;
import org.junit.Test;
import players.PlayerFactory;
import players.simple.RandomPlayer;

import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;
import static games.sushigo.cards.SGCard.SGCardType.*;

public class SushiGoHeuristicTest {
    private SGGameState state(int n) {
        Game game = GameType.SushiGo.createGameInstance(n, 7032L);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int i = 0; i < n; i++) players.add(new RandomPlayer(new Random(i)));
        game.reset(players, 7032L);
        game.getGameState().getCoreGameParameters().setParameterValue("competition mode", true);
        return (SGGameState) game.getGameState();
    }

    private SushiGoHeuristic only(String component) {
        SushiGoHeuristic heuristic = new SushiGoHeuristic();
        for (String key : List.of("setWeight", "wasabiWeight", "makiWeight", "puddingWeight"))
            heuristic.setParameterValue(key, key.equals(component) ? 1.0 : 0.0);
        return heuristic;
    }

    private void play(SGGameState state, int player, SGCard card) {
        state.getPlayedCards().get(player).add(card);
        state.getPlayedCardTypes(card.type, player).increment(card.count);
        card.type.onReveal(state, player);
    }

    @Test
    public void zeroWeightsReproduceFrameworkScoreEvaluation() {
        SGGameState state = state(3);
        play(state, 0, new SGCard(SquidNigiri));
        play(state, 0, new SGCard(Dumpling));
        play(state, 0, new SGCard(Tempura));
        SushiGoHeuristic heuristic = only("");
        assertEquals(state.getHeuristicScore(0), heuristic.evaluateState(state, 0), 0.0);
    }

    @Test
    public void incompleteSetsHavePotentialButCompletedSetsAreAlreadyScored() {
        SGGameState state = state(3);
        SushiGoHeuristic heuristic = only("setWeight");
        play(state, 0, new SGCard(Tempura));
        assertTrue(heuristic.evaluateState(state, 0) > 0);
        play(state, 0, new SGCard(Tempura));
        assertEquals(5.0 / 50, heuristic.evaluateState(state, 0), 1e-12);
        play(state, 0, new SGCard(Sashimi));
        double oneSashimi = heuristic.evaluateState(state, 0);
        play(state, 0, new SGCard(Sashimi));
        assertTrue(heuristic.evaluateState(state, 0) > oneSashimi);
        play(state, 0, new SGCard(Sashimi));
        assertEquals(15.0 / 50, heuristic.evaluateState(state, 0), 1e-12);
    }

    @Test
    public void impossibleSetCompletionsReceiveNoCredit() {
        SGGameState state = state(3);
        SushiGoHeuristic heuristic = only("setWeight");
        play(state, 0, new SGCard(Sashimi));
        state.getPlayerHands().get(0).clear();
        state.getPlayerHands().get(0).add(new SGCard(EggNigiri));
        assertEquals(0.0, heuristic.evaluateState(state, 0), 0.0);
        state.getPlayerHands().get(0).add(new SGCard(EggNigiri));
        ((SGParameters) state.getGameParameters()).nCardsPerType.replaceAll(
                (key, count) -> key.a == Sashimi ? 1 : count);
        assertEquals(0.0, heuristic.evaluateState(state, 0), 0.0);
    }

    @Test
    public void unusedWasabiAddsPotentialAndConsumedWasabiDoesNotDoubleCount() {
        SGGameState state = state(3);
        SushiGoHeuristic heuristic = only("wasabiWeight");
        play(state, 0, new SGCard(Wasabi));
        assertTrue(heuristic.evaluateState(state, 0) > 0);
        play(state, 0, new SGCard(SquidNigiri));
        assertEquals(0, state.getPlayedCardTypes(Wasabi, 0).getValue());
        assertEquals(9.0 / 50, heuristic.evaluateState(state, 0), 1e-12);
        play(state, 0, new SGCard(Dumpling));
        play(state, 0, new SGCard(SalmonNigiri));
        assertEquals(12.0 / 50, heuristic.evaluateState(state, 0), 1e-12);
    }

    @Test
    public void wasabiCannotUseMoreSlotsThanRemainingPicks() {
        SGGameState state = state(3);
        SushiGoHeuristic heuristic = only("wasabiWeight");
        state.getPlayerHands().get(0).clear();
        state.getPlayerHands().get(0).add(new SGCard(EggNigiri));
        play(state, 0, new SGCard(Wasabi));
        double oneSlot = heuristic.evaluateState(state, 0);
        state.getPlayedCardTypes(Wasabi, 0).setValue(5);
        assertEquals(oneSlot, heuristic.evaluateState(state, 0), 0.0);
        state.getPlayerHands().get(0).clear();
        assertEquals(0, heuristic.evaluateState(state, 0), 0.0);
    }

    @Test
    public void wasabiHasNoPotentialWhenNoNigiriRemain() {
        SGGameState state = state(3);
        play(state, 0, new SGCard(Wasabi));
        ((SGParameters) state.getGameParameters()).nCardsPerType.replaceAll((key, count) ->
                Set.of(SquidNigiri, SalmonNigiri, EggNigiri).contains(key.a) ? 0 : count);
        assertEquals(0, only("wasabiWeight").evaluateState(state, 0), 0.0);
    }

    @Test
    public void makiAndPuddingProjectionsMatchFrameworkAcrossAllSmallStandings() {
        // Check every combination of counts 0..3 for 2, 3 and 4 players, including ties.
        for (int n = 2; n <= 4; n++) {
            SGGameState state = state(n);
            for (int encoding = 0; encoding < (1 << (2 * n)); encoding++) {
                int value = encoding;
                for (int p = 0; p < n; p++) {
                    int count = value % 4;
                    state.getPlayedCardTypes(Maki, p).setValue(count);
                    state.getPlayedCardTypes(Pudding, p).setValue(count);
                    value /= 4;
                }
                for (SGCardType type : List.of(Maki, Pudding)) {
                    for (int p = 0; p < n; p++) state.getPlayerScore()[p].setValue(20);
                    int[] expected = new int[n];
                    for (int p = 0; p < n; p++) expected[p] = type == Maki
                            ? SushiGoHeuristic.projectedMaki(state, p)
                            : SushiGoHeuristic.projectedPudding(state, p);
                    if (type == Maki) type.onRoundEnd(state); else type.onGameEnd(state);
                    for (int p = 0; p < n; p++)
                        assertEquals(type + " n=" + n + " counts=" + encoding + " p=" + p,
                                state.getGameScore(p) - 20, expected[p], 0.0);
                }
            }
        }
    }

    @Test
    public void rankProjectionsAreDiscountedUntilTheirScoringBoundaries() {
        SGGameState state = state(3);
        play(state, 0, new SGCard(Maki, 3));
        play(state, 0, new SGCard(Pudding));
        assertEquals(0, only("makiWeight").evaluateState(state, 0), 0.0);
        assertEquals(0, only("puddingWeight").evaluateState(state, 0), 0.0);
        state.getPlayerHands().get(0).clear();
        assertEquals(6.0 / 50, only("makiWeight").evaluateState(state, 0), 1e-12);
        assertEquals(2.0 / 50, only("puddingWeight").evaluateState(state, 0), 1e-12);
    }

    @Test
    public void roundTransitionDoesNotCountAwardedMakiTwiceAndRetainsPudding() {
        SGGameState state = state(3);
        state.getPlayerScore()[0].setValue(20);
        play(state, 0, new SGCard(Maki, 3));
        play(state, 0, new SGCard(Pudding));
        play(state, 0, new SGCard(Tempura));
        state.getPlayerHands().forEach(hand -> hand.clear());
        SushiGoHeuristic heuristic = new SushiGoHeuristic();
        assertEquals(28.0 / 50, heuristic.evaluateState(state, 0), 1e-12);
        SGForwardModel model = new SGForwardModel();
        model._endRound(state);
        model._startRound(state);
        assertEquals(1, state.getRoundCounter());
        assertEquals(26, state.getGameScore(0), 0.0);
        assertEquals(0, state.getPlayedCardTypes(Maki, 0).getValue());
        assertEquals(1, state.getPlayedCardTypes(Pudding, 0).getValue());
        assertEquals(28.0 / 50, heuristic.evaluateState(state, 0), 1e-12);
    }

    @Test
    public void terminalEvaluationUsesOnlyTheActualResult() {
        SGGameState state = state(3);
        play(state, 0, new SGCard(Tempura));
        play(state, 0, new SGCard(Wasabi));
        state.getPlayerScore()[0].setValue(100);
        state.setGameStatus(CoreConstants.GameResult.GAME_END);
        for (CoreConstants.GameResult result : List.of(CoreConstants.GameResult.WIN_GAME,
                CoreConstants.GameResult.LOSE_GAME, CoreConstants.GameResult.DRAW_GAME)) {
            state.setPlayerResult(result, 0);
            assertEquals(result.value, new SushiGoHeuristic().evaluateState(state, 0), 0.0);
        }
    }

    @Test
    public void evaluationIgnoresHandContentsAndLeavesPublicFeaturesUnchanged() {
        SGGameState state = state(3);
        play(state, 0, new SGCard(Tempura));
        play(state, 0, new SGCard(Wasabi));
        SushiGoHeuristic heuristic = new SushiGoHeuristic();
        double before = heuristic.evaluateState(state, 0);
        String tableaus = state.getPlayedCards().toString();
        for (var hand : state.getPlayerHands()) {
            int count = hand.getSize();
            hand.clear();
            for (int i = 0; i < count; i++) hand.add(new SGCard(Pudding));
        }
        assertEquals(before, heuristic.evaluateState(state, 0), 0.0);
        assertEquals(before, heuristic.evaluateState(state, 0), 0.0);
        assertEquals(tableaus, state.getPlayedCards().toString());
        assertEquals(1, state.getPlayedCardTypes(Wasabi, 0).getValue());
        assertEquals(1, state.getPlayedCardTypes(Tempura, 0).getValue());
        assertEquals(0, state.getGameScore(0), 0.0);
    }

    @Test
    public void configuredHeuristicWeightsSurviveAnIndependentPlayerCopy() {
        String config = Path.of(System.getProperty("coursework.project"), "sushigoagent",
                "heuristic_iterations.json").toString();
        BasicMCTSPlayer player = (BasicMCTSPlayer) PlayerFactory.createPlayer(config);
        assertTrue(player.getParameters().heuristic instanceof SushiGoHeuristic);
        player.getParameters().setParameterValue("heuristic.setWeight", 0.25);
        BasicMCTSPlayer copy = player.copy();
        assertNotSame(player.getParameters().heuristic, copy.getParameters().heuristic);
        SushiGoHeuristic original = (SushiGoHeuristic) player.getParameters().heuristic;
        SushiGoHeuristic copied = (SushiGoHeuristic) copy.getParameters().heuristic;
        assertEquals(0.25, ((Number) copied.getParameterValue("setWeight")).doubleValue(), 0.0);
        copy.getParameters().setParameterValue("heuristic.setWeight", 0.0);
        SGGameState state = state(3);
        play(state, 0, new SGCard(Tempura));
        assertTrue(original.evaluateState(state, 0) > copied.evaluateState(state, 0));
        assertEquals(0.25, ((Number) original.getParameterValue("setWeight")).doubleValue(), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeWeightsAreRejected() {
        new SushiGoHeuristic().setParameterValue("setWeight", -1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonFiniteWeightsAreRejected() {
        new SushiGoHeuristic().setParameterValue("wasabiWeight", Double.NaN);
    }
}
