package api.database;

import api.database.mapper.RowMapper;
import lombok.Builder;
import lombok.Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class DBRequest {
    private RequestType requestType;
    private TableName table;
    private List<Condition> conditions;
    private final ConnectionProvider connectionProvider = new ConnectionProvider();

    public <T> T extractAs(RowMapper<T> mapper) {
        return executeQuery(mapper);
    }

    private <T> T executeQuery(RowMapper<T> mapper) {
        String sql = buildSQL();

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (conditions != null) {
                for (int i = 0; i < conditions.size(); i++) {
                    statement.setObject(
                            i + 1,
                            conditions.get(i).getValue()
                    );
                }
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapper.map(resultSet);
                }

                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database query failed. SQL: " + sql, e
            );
        }
    }

    private String buildSQL() {
        StringBuilder sql = new StringBuilder();
        
        switch (requestType) {
            case SELECT:
                sql.append("SELECT * FROM ").append(table.getValue());
                if (conditions != null && !conditions.isEmpty()) {
                    sql.append(" WHERE ");
                    for (int i = 0; i < conditions.size(); i++) {
                        if (i > 0) sql.append(" AND ");
                        sql.append(conditions.get(i).getColumn()).append(" ").append(conditions.get(i).getOperator()).append(" ?");
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Request type " + requestType + " not implemented");
        }
        
        return sql.toString();
    }

    public static DBRequestBuilder builder() {
        return new DBRequestBuilder();
    }

    public static class DBRequestBuilder {
        private RequestType requestType;
        private TableName table;
        private List<Condition> conditions = new ArrayList<>();

        public DBRequestBuilder requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public DBRequestBuilder where(Condition condition) {
            this.conditions.add(condition);
            return this;
        }

        public DBRequestBuilder table(TableName table) {
            this.table = table;
            return this;
        }

        public <T> T extractAs(RowMapper<T> mapper) {
            DBRequest request = DBRequest.builder()
                    .requestType(requestType)
                    .table(table)
                    .conditions(conditions)
                    .build();

            return request.extractAs(mapper);
        }
    }
}
