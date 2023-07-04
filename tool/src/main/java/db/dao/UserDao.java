package db.dao;


import db.entity.UserInfos;
import org.apache.ibatis.annotations.Param;

public interface UserDao {

	UserInfos queryUserInfo(@Param("userId") int userId);

	UserInfos insertUserInfo(@Param("plant") String plant);
}
