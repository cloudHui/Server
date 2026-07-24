package game.manager.table.ddz;

import game.manager.table.DdzTable;
import game.manager.table.Table;
import game.manager.table.TableUser;
import model.tablemodel.RobotRoomTemplates;
import model.tablemodel.TableModel;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 地主连庄 / 农民胜下家优先叫牌。
 */
public class DdzLianZhuangTest {

	@Test
	public void landlordWin_keepsFirstCallSeat() throws Exception {
		DdzTable table = newTable();
		seatRobots(table, 3);
		table.getDdz().setLandlordSeat(1);
		table.setNextFirstCallSeat(1);
		table.resetGameContext();
		assertEquals(1, table.getBanner().getFirstRandomRobSeat());
		assertEquals(1, table.getOp().getCurrOpSeat());
		assertEquals(-1, table.getNextFirstCallSeat());
	}

	@Test
	public void farmerWin_nextSeatFirstCall() throws Exception {
		DdzTable table = newTable();
		seatRobots(table, 3);
		// 地主座位 2，农民胜 → 下家 0 优先叫
		table.setNextFirstCallSeat((2 + 1) % 3);
		table.resetGameContext();
		assertEquals(0, table.getBanner().getFirstRandomRobSeat());
		assertEquals(0, table.getOp().getCurrOpSeat());
	}

	private static DdzTable newTable() {
		TableModel model = RobotRoomTemplates.douDiZhu();
		return new DdzTable(90002L, model, null);
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
			TableUser bot = new TableUser(2000 + seat, "", "bot" + seat, 0);
			bot.setRobot(true);
			bot.setSeated(seat);
			seatUsers.put(seat, bot);
			users.put(bot.getUserId(), bot);
		}
	}
}
