package common.models;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FraudCheckMockResponse {
    private String status;
    private String decision;
    private Double riskScore;
    private String reason;
    private Boolean requiresManualReview;
    private Boolean additionalVerificationRequired;

}
