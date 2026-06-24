package hall.db.service;

import db.mysql.DBService;
import db.mysql.DBSourceFactory;
import hall.db.dao.UserDao;
import hall.db.entity.UserInfos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 用户数据库服务，封装用户表的增删改查 */
public class UserService extends DBService<UserDao> {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	public UserService() {
		super(DBSourceFactory.INSTANCE.getSqlSessionFactory(), UserDao.class);
	}

	/** 按用户ID查询 */
	public UserInfos queryUserInfo(long userId) {
		logger.debug("queryUserInfo, userId: {}", userId);
		return execute(o -> o.queryUserInfo(userId));
	}

	/** 按设备ID查询 */
	public UserInfos queryByDeviceId(String deviceId) {
		logger.debug("queryByDeviceId, deviceId: {}", deviceId);
		return execute(o -> o.queryByDeviceId(deviceId));
	}

	/** 按登录密钥查询 */
	public UserInfos queryByLoginKey(String loginKey) {
		logger.debug("queryByLoginKey");
		return execute(o -> o.queryByLoginKey(loginKey));
	}

	/** 插入新用户，返回生成的userId */
	public long insertUser(UserInfos user) {
		logger.info("insertUser, nickName: {}", user.getNickName());
		execute(o -> o.insertUser(user));
		return user.getUserId();
	}

	/** 更新登录信息(token/deviceId) */
	public int updateLoginInfo(UserInfos user) {
		logger.debug("updateLoginInfo, userId: {}", user.getUserId());
		return execute(o -> o.updateLoginInfo(user));
	}
}
