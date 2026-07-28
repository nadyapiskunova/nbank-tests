package api.models;

import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponse extends BaseModel {
    private Integer id;
    private String accountNumber;
    private Double balance;
    private List<TransactionResponse> transactions;

}
