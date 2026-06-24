package game.manager.table.cards;

import game.manager.table.card.CardConst;
import org.junit.Test;
import static org.junit.Assert.*;

/** Card 基础模型测试 */
public class CardTest {

    @Test
    public void testGetCardVal() {
        Card card = new Card(105); // 花色1, 点数5
        assertEquals(5, card.getCardVal());
    }

    @Test
    public void testGetCardSuit() {
        Card card = new Card(105);
        assertNotNull(card.getCardSuit());
    }

    @Test
    public void testJokers() {
        Card smallJoker = new Card(CardConst.SMALL_JOKER_VAL);
        assertTrue(smallJoker.isSmallJoker());
        assertFalse(smallJoker.isBigJoker());

        Card bigJoker = new Card(CardConst.BIG_JOKER_VAL);
        assertTrue(bigJoker.isBigJoker());
        assertFalse(bigJoker.isSmallJoker());
    }

    @Test
    public void testCompareTo() {
        Card big = new Card(114);   // 点数14(A)
        Card small = new Card(103); // 点数3
        assertTrue(big.compareTo(small) < 0); // 大的排前面(降序)
    }

    @Test
    public void testEqualsAndHashCode() {
        Card a = new Card(105);
        Card b = new Card(105);
        Card c = new Card(106);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
