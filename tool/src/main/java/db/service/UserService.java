package db.service;

import db.dao.UserDao;
import db.model.UserInfos;
import db.mysql.DBService;
import org.apache.ibatis.session.SqlSessionFactory;

public class UserService extends DBService<UserDao> {

	public UserService(SqlSessionFactory sqlSessionFactory, Class<UserDao> clazz) {
		super(sqlSessionFactory, clazz);
	}

	public UserInfos queryUserInfo(int userId) {
		return execute(o -> o.queryUserInfo(userId));
	}

	public UserInfos insertUserInfo(String plant) {
		return execute(o -> o.insertUserInfo(plant));
	}
}
