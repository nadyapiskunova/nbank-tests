package api.database.mapper;

import api.dao.TransactionDao;
import api.models.TransactionType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionDaoMapper implements RowMapper<TransactionDao> {

    @Override
    public TransactionDao map(ResultSet resultSet) throws SQLException {
        return TransactionDao.builder()
                .id(resultSet.getInt("id"))
                .amount(resultSet.getDouble("amount"))
                .type(TransactionType.valueOf(resultSet.getString("type")))
                .accountId(resultSet.getInt("account_id"))
                .relatedAccountId(
                        resultSet.getObject("related_account_id") == null
                                ? null
                                : Math.toIntExact(resultSet.getLong("related_account_id"))
                )
                .build();
    }
}