package game.manager.table;

import org.junit.Test;
import static org.junit.Assert.*;

/** GameResult 多局结算测试 */
public class GameResultTest {

    @Test
    public void testAddRound() {
        GameResult result = new GameResult();
        result.setTotalRounds(3);

        int[] scores = {10, -5, -5};
        result.addRound(1, 0, 10, scores, "normal");

        assertEquals(1, result.getCompletedRounds());
        assertEquals(10, result.getTotalScore(0));
        assertEquals(-5, result.getTotalScore(1));
        assertEquals(-5, result.getTotalScore(2));
    }

    @Test
    public void testIsComplete() {
        GameResult result = new GameResult();
        result.setTotalRounds(2);
        assertFalse(result.isComplete());

        result.addRound(1, 0, 10, new int[]{10, -5, -5}, "normal");
        assertFalse(result.isComplete());

        result.addRound(2, 1, 10, new int[]{-5, 10, -5}, "normal");
        assertTrue(result.isComplete());
    }

    @Test
    public void testGetRanking() {
        GameResult result = new GameResult();
        result.setTotalRounds(1);
        result.addRound(1, 0, 10, new int[]{20, -10, -10}, "normal");

        java.util.List<java.util.Map.Entry<Integer, Integer>> ranking = result.getRanking();
        assertEquals(Integer.valueOf(0), ranking.get(0).getKey());
        assertEquals(Integer.valueOf(20), ranking.get(0).getValue());
    }

    @Test
    public void testGetTotalScoreDefaultZero() {
        GameResult result = new GameResult();
        assertEquals(0, result.getTotalScore(99));
    }
}
