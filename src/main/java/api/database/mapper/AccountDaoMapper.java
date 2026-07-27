package api.database.mapper;

import api.dao.AccountDao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDaoMapper implements RowMapper<AccountDao> {
    @Override
    public AccountDao map(ResultSet resultSet) throws SQLException {
        return AccountDao.builder()
                .id(resultSet.getInt("id"))
                .accountNumber(resultSet.getString("account_number"))
                .balance(resultSet.getDouble("balance"))
                .customerId(resultSet.getInt("customer_id"))
                .build();
    }
}
