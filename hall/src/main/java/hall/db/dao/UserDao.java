package hall.db.dao;


import hall.db.entity.UserInfos;
import org.apache.ibatis.annotations.Param;

public interface UserDao {

	UserInfos queryUserInfo(@Param("userId") int userId);

	UserInfos queryUserInfoByPlant(@Param("plant") String plant);
}
