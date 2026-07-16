package api.models;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferResponse extends BaseModel {
    private String message;
    private Double amount;
    private Integer receiverAccountId;
    private Integer senderAccountId;
}
