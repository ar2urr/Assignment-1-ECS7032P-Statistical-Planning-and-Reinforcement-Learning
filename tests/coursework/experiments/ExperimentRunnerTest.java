package coursework.experiments;

import core.*;
import games.GameType;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import org.junit.Test;
import players.simple.RandomPlayer;
import java.util.*;
import static org.junit.Assert.*;

public class ExperimentRunnerTest {
    @Test public void principalStudyHasEqualSeatCounts() {
        for (int n : List.of(3, 4)) {
            int[] count = new int[n];
            for (int i = 0; i < 504; i++) count[ExperimentRunner.seatFor(i, n)]++;
            for (int value : count) assertEquals(504 / n, value);
        }
    }
    @Test public void seedScheduleSeparatesGamesCellsAndWarmup() {
        Set<Long> seeds = new HashSet<>();
        for (int cell = 72; cell < 220; cell++) {
            for (int game = -108; game < 504; game++) {
                long seed = ExperimentRunner.gameSeed(703205, cell, game);
                assertEquals(seed, ExperimentRunner.gameSeed(703205, cell, game));
                assertTrue(seeds.add(seed));
            }
        }
    }
    @Test public void winCreditSplitsTrueTiesAndUsesPuddingTiebreak() {
        Game game = GameType.SushiGo.createGameInstance(3, 7032L);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()), 7032L);
        SGGameState state = (SGGameState) game.getGameState();
        state.getPlayerScore()[0].setValue(10);
        state.getPlayerScore()[1].setValue(10);
        state.getPlayerScore()[2].setValue(3);
        assertEquals(0.5, ExperimentRunner.winShare(state, 0), 0.0);
        assertEquals(0.5, ExperimentRunner.winShare(state, 1), 0.0);
        assertEquals(0.0, ExperimentRunner.winShare(state, 2), 0.0);
        state.getPlayedCardTypes(SGCard.SGCardType.Pudding, 0).setValue(1);
        assertEquals(1.0, ExperimentRunner.winShare(state, 0), 0.0);
        assertEquals(0.0, ExperimentRunner.winShare(state, 1), 0.0);
    }
}
