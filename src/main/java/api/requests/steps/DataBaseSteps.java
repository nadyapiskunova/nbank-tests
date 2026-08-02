package api.requests.steps;

import api.dao.TransactionDao;
import api.database.Condition;
import api.database.DBRequest;
import api.dao.UserDao;
import api.dao.AccountDao;
import api.configs.Config;
import api.database.RequestType;
import api.database.TableName;
import api.database.mapper.AccountDaoMapper;
import api.database.mapper.TransactionDaoMapper;
import api.database.mapper.UserDaoMapper;
import api.models.TransactionType;
import common.helpers.StepLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBaseSteps {

    public static UserDao getUserByUsername(String username) {
        return StepLogger.log("Get user from database by username: " + username, () -> {
            return DBRequest.builder()
                    .requestType(RequestType.SELECT)
                    .table(TableName.CUSTOMERS)
                    .where(Condition.equalTo("username", username))
                    .extractAs(new UserDaoMapper());
        });
    }

    public static UserDao getUserById(Integer id) {
        return StepLogger.log("Get user from database by ID: " + id, () -> {
            return DBRequest.builder()
                    .requestType(RequestType.SELECT)
                    .table(TableName.CUSTOMERS)
                    .where(Condition.equalTo("id", id))
                    .extractAs(new UserDaoMapper());
        });
    }

    public static UserDao getUserByRole(String role) {
        return StepLogger.log("Get user from database by role: " + role, () -> {
            return DBRequest.builder()
                    .requestType(RequestType.SELECT)
                    .table(TableName.CUSTOMERS)
                    .where(Condition.equalTo("role", role))
                    .extractAs(new UserDaoMapper());
        });
    }

    public static AccountDao getAccountByAccountNumber(String accountNumber) {
        return StepLogger.log("Get account from database by account number: " + accountNumber, () -> {
            return DBRequest.builder()
                    .requestType(RequestType.SELECT)
                    .table(TableName.ACCOUNTS)
                    .where(Condition.equalTo("account_number", accountNumber))
                    .extractAs(new AccountDaoMapper());
        });
    }

    public static AccountDao getAccountById(Integer id) {
        return StepLogger.log("Get account from database by ID: " + id, () -> {
            return DBRequest.builder()
                    .requestType(RequestType.SELECT)
                    .table(TableName.ACCOUNTS)
                    .where(Condition.equalTo("id", id))
                    .extractAs(new AccountDaoMapper());
        });
    }

    public static AccountDao getAccountByCustomerId(Integer customerId) {
        return StepLogger.log("Get account from database by customer ID: " + customerId, () -> {
            return DBRequest.builder()
                    .requestType(RequestType.SELECT)
                    .table(TableName.ACCOUNTS)
                    .where(Condition.equalTo("customer_id", customerId))
                    .extractAs(new AccountDaoMapper());
        });
    }

    public static void updateAccountBalance(Integer accountId, Double newBalance) {
        StepLogger.log("Update account balance in database for account ID: " + accountId + " to: " + newBalance, () -> {
            try (Connection connection = DriverManager.getConnection(
                    Config.getProperty("db.url"),
                    Config.getProperty("db.username"),
                    Config.getProperty("db.password"))) {

                String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setDouble(1, newBalance);
                    statement.setLong(2, accountId);
                    int rowsAffected = statement.executeUpdate();

                    if (rowsAffected == 0) {
                        throw new RuntimeException("No account found with ID: " + accountId);
                    }

                    return rowsAffected;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update account balance", e);
            }
        });
    }

    public static TransactionDao getTransactionByAccountId(Integer accountId){
        return DBRequest.builder()
                .requestType(RequestType.SELECT)
                .table(TableName.TRANSACTIONS)
                .where(Condition.equalTo("account_id", accountId))
                .extractAs(new TransactionDaoMapper());
    }

    public static TransactionDao getTransactionByAccountIdAndType(
            Integer accountId,
            TransactionType type
    ) {
        return DBRequest.builder()
                .requestType(RequestType.SELECT)
                .table(TableName.TRANSACTIONS)
                .where(Condition.equalTo("account_id", accountId))
                .where(Condition.equalTo("type", type.name()))
                .extractAs(new TransactionDaoMapper());
    }
}