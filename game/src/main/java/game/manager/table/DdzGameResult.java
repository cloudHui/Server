package game.manager.table;

import java.util.ArrayList;
import java.util.List;

/**
 * 斗地主整场结算
 */
public class DdzGameResult extends GameResult {

    /**
     * 每局的settleFactor
     */
    private final List<Integer> settleFactorList = new ArrayList<>();

    /**
     * 每局是否春天
     */
    private final List<Boolean> springList = new ArrayList<>();

    @Override
    public void addRound(int round, int winnerSeat, int score, int[] scores, String winType) {
        super.addRound(round, winnerSeat, score, scores, winType);
        settleFactorList.add(score);
        springList.add("spring".equals(winType) || "antiSpring".equals(winType));
    }

    public List<Integer> getSettleFactorList() {
        return settleFactorList;
    }

    public List<Boolean> getSpringList() {
        return springList;
    }
}
