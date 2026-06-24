package game.manager.table.mj;

import org.junit.Test;
import static org.junit.Assert.*;

/** MjExposedSet 副露牌组测试 */
public class MjExposedSetTest {

    @Test
    public void testTypeName() {
        assertEquals("peng", MjExposedSet.Type.PENG.toName());
        assertEquals("mingGang", MjExposedSet.Type.MING_GANG.toName());
        assertEquals("anGang", MjExposedSet.Type.AN_GANG.toName());
        assertEquals("buGang", MjExposedSet.Type.BU_GANG.toName());
        assertEquals("chi", MjExposedSet.Type.CHI.toName());
    }

    @Test
    public void testIsGang() {
        MjExposedSet mingGang = new MjExposedSet(MjExposedSet.Type.MING_GANG,
                java.util.Arrays.asList(1, 1, 1, 1), 0);
        assertTrue(mingGang.isGang());

        MjExposedSet peng = new MjExposedSet(MjExposedSet.Type.PENG,
                java.util.Arrays.asList(1, 1, 1), 0);
        assertFalse(peng.isGang());

        MjExposedSet chi = new MjExposedSet(MjExposedSet.Type.CHI,
                java.util.Arrays.asList(1, 2, 3), 0);
        assertFalse(chi.isGang());
    }

    @Test
    public void testTileIdsImmutable() {
        MjExposedSet set = new MjExposedSet(MjExposedSet.Type.PENG,
                java.util.Arrays.asList(5, 5, 5), 1);
        try {
            set.getTileIds().add(6);
            fail("应该抛出UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
