package api.models;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FraudCheckTransferResponse extends BaseModel {

    private String status;
    private String message;

    private Double amount;
    private Integer senderAccountId;
    private Integer receiverAccountId;

    private Double fraudRiskScore;
    private String fraudReason;

    private Boolean requiresManualReview;
    private Boolean requiresVerification;

    private Integer transactionId;
}






