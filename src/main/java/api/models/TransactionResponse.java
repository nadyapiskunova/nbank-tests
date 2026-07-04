package api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {
    private Integer id;
    private Double amount;
    private TransactionType type;
    private String timestamp;
    private Integer relatedAccountId;

}
