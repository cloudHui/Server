package game.manager.table.ddz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.google.protobuf.CodedOutputStream;

import proto.GameProto;

/**
 * 生成带扩展字段的 {@link proto.GameProto.NotResult} 二进制（字段 3-9），与 game.proto 保持一致。
 * 旧客户端会忽略未知字段；新客户端按更新后的 .proto 解析即可。
 *
 * WARNING: This class uses manual protobuf encoding via {@link CodedOutputStream}.
 * The field numbers (1-9) used in {@link #encodeNotResultExtended} MUST match the
 * field numbers defined in the corresponding .proto file (game.proto / NotResult message).
 * If the .proto definition changes, this encoder must be updated accordingly to avoid
 * data corruption or silent parsing failures on the client side.
 *
 * @author cloud
 * @date 2026-05-03
 * @version 1.0
 * @since 1.0
 */
public final class DdzResultEncoder {

	private DdzResultEncoder() {
	}

	/**
	 * 编码不结果扩展
	 * 
	 * @param winnerUserId   赢家用户ID
	 * @param rPlayers       地主玩家列表
	 * @param landlordUserId 地主用户ID
	 * @param winTeam        赢家队伍
	 * @param baseScore      基础分数
	 * @param robMultiplier  抢地主倍数
	 * @param spring         春天
	 * @param antiSpring     反春天
	 * @param settleFactor   结算因子
	 * @return 编码后的字节数组
	 * @throws IOException 输入输出异常
	 */
	public static byte[] encodeNotResultExtended(
			int winnerUserId,
			List<GameProto.RPlayer> rPlayers,
			int landlordUserId,
			int winTeam,
			int baseScore,
			int robMultiplier,
			boolean spring,
			boolean antiSpring,
			int settleFactor) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CodedOutputStream cos = CodedOutputStream.newInstance(baos);
		cos.writeInt32(1, winnerUserId);
		for (GameProto.RPlayer rp : rPlayers) {
			cos.writeMessage(2, rp);
		}
		cos.writeInt32(3, landlordUserId);
		cos.writeInt32(4, winTeam);
		cos.writeInt32(5, baseScore);
		cos.writeInt32(6, robMultiplier);
		cos.writeBool(7, spring);
		cos.writeBool(8, antiSpring);
		cos.writeInt32(9, settleFactor);
		cos.flush();
		return baos.toByteArray();
	}
}
