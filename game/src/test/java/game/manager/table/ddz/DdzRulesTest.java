package game.manager.table.ddz;

import game.manager.table.cards.Card;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/** DdzRules 牌型识别和比大小测试 */
public class DdzRulesTest {

    // ======================== 牌型识别 ========================

    @Test
    public void testSingle() {
        List<Card> cards = Arrays.asList(new Card(103));
        Optional<DdzHand> hand = DdzRules.analyze(cards);
        assertTrue(hand.isPresent());
        assertEquals(proto.ConstProto.CardType.SINGLE, hand.get().getType());
    }

    @Test
    public void testPair() {
        List<Card> cards = Arrays.asList(new Card(105), new Card(205));
        Optional<DdzHand> hand = DdzRules.analyze(cards);
        assertTrue(hand.isPresent());
        assertEquals(proto.ConstProto.CardType.DOUBLE, hand.get().getType());
    }

    @Test
    public void testTriple() {
        List<Card> cards = Arrays.asList(new Card(107), new Card(207), new Card(307));
        Optional<DdzHand> hand = DdzRules.analyze(cards);
        assertTrue(hand.isPresent());
        assertEquals(proto.ConstProto.CardType.TRIPLE, hand.get().getType());
    }

    @Test
    public void testBomb() {
        List<Card> cards = Arrays.asList(new Card(108), new Card(208), new Card(308), new Card(408));
        Optional<DdzHand> hand = DdzRules.analyze(cards);
        assertTrue(hand.isPresent());
        assertEquals(proto.ConstProto.CardType.BOOM, hand.get().getType());
        assertTrue(hand.get().isBomb());
    }

    @Test
    public void testRocket() {
        // 小王(516) + 大王(517)
        List<Card> cards = Arrays.asList(new Card(516), new Card(517));
        Optional<DdzHand> hand = DdzRules.analyze(cards);
        assertTrue(hand.isPresent());
        assertTrue(hand.get().isRocket());
    }

    @Test
    public void testStraight() {
        // 顺子: 3-4-5-6-7
        List<Card> cards = Arrays.asList(
                new Card(103), new Card(104), new Card(105), new Card(106), new Card(107));
        Optional<DdzHand> hand = DdzRules.analyze(cards);
        assertTrue(hand.isPresent());
        assertEquals(proto.ConstProto.CardType.STRAIGHT, hand.get().getType());
    }

    @Test
    public void testEmptyCards() {
        Optional<DdzHand> hand = DdzRules.analyze(Arrays.asList());
        assertFalse(hand.isPresent());
    }

    @Test
    public void testNullCards() {
        Optional<DdzHand> hand = DdzRules.analyze(null);
        assertFalse(hand.isPresent());
    }

    // ======================== 比大小 ========================

    @Test
    public void testBeatsSingle() {
        DdzHand big = DdzRules.analyze(Arrays.asList(new Card(114))).get();   // A
        DdzHand small = DdzRules.analyze(Arrays.asList(new Card(103))).get();  // 3
        assertTrue(DdzRules.beats(big, small));
        assertFalse(DdzRules.beats(small, big));
    }

    @Test
    public void testBeatsBombOverSingle() {
        DdzHand bomb = DdzRules.analyze(Arrays.asList(
                new Card(105), new Card(205), new Card(305), new Card(405))).get();
        DdzHand single = DdzRules.analyze(Arrays.asList(new Card(114))).get();
        assertTrue(DdzRules.beats(bomb, single));
    }

    @Test
    public void testBeatsNullLast() {
        DdzHand single = DdzRules.analyze(Arrays.asList(new Card(103))).get();
        assertTrue(DdzRules.beats(single, null));
    }

    @Test
    public void testBeatsSameTypeCompareStrength() {
        DdzHand pair1 = DdzRules.analyze(Arrays.asList(new Card(110), new Card(210))).get();
        DdzHand pair2 = DdzRules.analyze(Arrays.asList(new Card(105), new Card(205))).get();
        assertTrue(DdzRules.beats(pair1, pair2));
        assertFalse(DdzRules.beats(pair2, pair1));
    }
}
