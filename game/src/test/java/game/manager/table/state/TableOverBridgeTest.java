package game.manager.table.state;

import game.manager.table.DdzTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
import model.tablemodel.RobotRoomTemplates;
import model.tablemodel.TableModel;
import msg.registor.enums.TableState;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 小结算阶段：有超时的 TABLE_OVER 仍需下发准备提示；自动房不在 tick 内立即全员准备。
 */
public class TableOverBridgeTest {

	@Test
	public void settleTick_sendsPrepareOnce_withoutImmediateAutoReady() throws Exception {
		DdzTable table = newTable();
		seatRobots(table, 3);
		// 模拟已完成第 1 局，仍有后续局
		setCurrentRound(table, 1);
		table.upNextStateWithTime(TableState.TABLE_OVER, System.currentTimeMillis());

		TableOverBridge bridge = new TableOverBridge();
		bridge.handle(table);

		assertEquals(TableState.TABLE_OVER, table.getTableState());
		assertEquals("快速房应等满 15 秒再自动准备，tick 内不应已全员准备开下一局",
				0, table.getReadyCount());
		assertEquals(-1, table.getOp().getCurrOpSeat());
	}

	@Test
	public void overTime_autoStartsNextRoundForRobotRoom() throws Exception {
		DdzTable table = newTable();
		seatRobots(table, 3);
		setCurrentRound(table, 1);
		table.setNextFirstCallSeat(2);
		// 已超过 15 秒：同一次 handle 内会下发准备提示并走超时自动开下一局
		table.upNextStateWithTime(TableState.TABLE_OVER, System.currentTimeMillis() - 16000L);

		new TableOverBridge().handle(table);

		assertEquals(TableState.WAITING, table.getTableState());
		assertEquals(2, table.getCurrentRound());
		assertEquals("连庄优先叫座位应带到下一局", 2, table.getBanner().getFirstRandomRobSeat());
	}

	private static DdzTable newTable() {
		TableModel model = RobotRoomTemplates.douDiZhu();
		assertTrue(model.getTotalRounds() > 1);
		assertEquals(1, model.getAutoNextRound());
		return new DdzTable(90003L, model, null);
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
			TableUser bot = new TableUser(3000 + seat, "", "bot" + seat, 0);
			bot.setRobot(true);
			bot.setSeated(seat);
			seatUsers.put(seat, bot);
			users.put(bot.getUserId(), bot);
		}
	}

	private static void setCurrentRound(Table table, int round) throws Exception {
		Field f = Table.class.getDeclaredField("currentRound");
		f.setAccessible(true);
		f.setInt(table, round);
	}
}
