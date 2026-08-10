package api.database.mapper;

import api.dao.UserDao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDaoMapper implements RowMapper<UserDao> {

    @Override
    public UserDao map(ResultSet resultSet) throws SQLException {
        return UserDao.builder()
                .id(resultSet.getInt("id"))
                .username(resultSet.getString("username"))
                .password(resultSet.getString("password"))
                .role(resultSet.getString("role"))
                .name(resultSet.getString("name"))
                .build();
    }
}
