package api.dao;

import api.models.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDao {
    private Integer id;
    private Double amount;
    private TransactionType type;
    private Integer accountId;
    private Integer relatedAccountId;
}
