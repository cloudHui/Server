package game.manager.table.ddz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.google.protobuf.CodedOutputStream;

import proto.GameProto;

/**
 * 生成带扩展字段的 {@link proto.GameProto.NotResult} 二进制（字段 3-9），与 game.proto 保持一致。
 * 旧客户端会忽略未知字段；新客户端按更新后的 .proto 解析即可。
 */
public final class DdzResultEncoder {

	private DdzResultEncoder() {
	}

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
