package game.manager.table.ddz;

import game.manager.table.DdzTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
import game.manager.table.state.Rob;
import model.tablemodel.RobotRoomTemplates;
import model.tablemodel.TableModel;
import msg.registor.enums.TableState;
import org.junit.Test;
import proto.ConstProto;
import proto.GameProto;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 叫分阶段座位轮转：Rob 不得把操作位重置回首叫座位。
 */
public class DdzBidSeatRotationTest {

	@Test
	public void robCallPhase_preservesAdvancedOpSeat() {
		DdzTable table = newTable();
		table.getBanner().setFirstRandomRobSeat(0);
		table.getOp().setCurrOpSeat(1);
		table.getBanner().setRobPhase(false);
		table.getBanner().setRobBroadcastDone(false);

		new Rob().onTiming(table);

		assertEquals("再次进入 ROB 时须保留已推进的操作位，不能回到首叫座位",
				1, table.getOp().getCurrOpSeat());
		assertEquals(0, table.getBanner().getFirstRandomRobSeat());
	}

	@Test
	public void robCallPhase_initializesFirstSeatOnce() {
		DdzTable table = newTable();
		assertEquals(-1, table.getBanner().getFirstRandomRobSeat());
		assertEquals(-1, table.getOp().getCurrOpSeat());

		new Rob().onTiming(table);

		int first = table.getBanner().getFirstRandomRobSeat();
		assertTrue(first >= 0 && first < 3);
		assertEquals(first, table.getOp().getCurrOpSeat());

		table.getBanner().setRobBroadcastDone(false);
		table.getOp().moveToNextOp();
		int advanced = table.getOp().getCurrOpSeat();
		assertNotEquals(first, advanced);

		new Rob().onTiming(table);
		assertEquals(advanced, table.getOp().getCurrOpSeat());
		assertEquals(first, table.getBanner().getFirstRandomRobSeat());
	}

	@Test
	public void threeNotCalls_rotateThroughAllSeats() throws Exception {
		DdzTable table = newTable();
		seatRobots(table, 3);
		table.upNextStateWithTime(TableState.IDLE_ROB, System.currentTimeMillis());

		table.getBanner().setFirstRandomRobSeat(0);
		table.getOp().setCurrOpSeat(0);
		table.getBanner().setRobPhase(false);

		int[] expectedSeats = {0, 1, 2};
		for (int i = 0; i < expectedSeats.length; i++) {
			assertEquals("第 " + (i + 1) + " 次不叫前操作位", expectedSeats[i], table.getOp().getCurrOpSeat());
			assertEquals(TableState.IDLE_ROB, table.getTableState());

			int userId = table.getSeatUser(expectedSeats[i]).getUserId();
			int result = DdzBidService.apply(table, userId,
					GameProto.OpInfo.newBuilder().setChoice(ConstProto.Operation.NOT_CALL).build());
			assertEquals(ConstProto.Result.SUCCESS_VALUE, result);

			if (i < expectedSeats.length - 1) {
				assertEquals(TableState.ROB, table.getTableState());
				assertFalse(table.getBanner().isRobPhase());
				table.getBanner().setRobBroadcastDone(false);
				new Rob().onTiming(table);
				assertEquals("第 " + (i + 1) + " 次不叫后应轮到下一座位",
						expectedSeats[i + 1], table.getOp().getCurrOpSeat());
				assertEquals(TableState.IDLE_ROB, table.getTableState());
			}
		}
		assertTrue("三人都不叫后应进入抢地主/定地主后续流程",
				table.getBanner().isRobPhase() || table.getTableState() == TableState.CARD
						|| table.getTableState() == TableState.ROB);
	}

	private static DdzTable newTable() {
		TableModel model = RobotRoomTemplates.douDiZhu();
		return new DdzTable(90001L, model, null);
	}

	@SuppressWarnings("unchecked")
	private static void seatRobots(DdzTable table, int seats) throws Exception {
		Field seatUsersField = Table.class.getDeclaredField("seatUsers");
		seatUsersField.setAccessible(true);
		Field usersField = Table.class.getDeclaredField("users");
		usersField.setAccessible(true);
		Map<Integer, TableUser> seatUsers = (Map<Integer, TableUser>) seatUsersField.get(table);
		Map<Integer, TableUser> users = (Map<Integer, TableUser>) usersField.get(table);
		for (int seat = 0; seat < seats; seat++) {
			TableUser bot = new TableUser(1000 + seat, "", "bot" + seat, 0);
			bot.setRobot(true);
			bot.setSeated(seat);
			seatUsers.put(seat, bot);
			users.put(bot.getUserId(), bot);
		}
	}
}
