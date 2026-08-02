package api.requests.skeleton;

import api.models.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Endpoint {
    ADMIN_USER(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    ),
    ACCOUNTS(
           "/accounts",
            BaseModel.class,
            AccountResponse.class
    ),
    LOGIN(
            "/auth/login",
            LoginUserRequest.class,
            LoginUserResponse.class
    ),
    DEPOSIT(
            "/accounts/deposit",
            DepositRequest.class,
            AccountResponse.class
    ),
    CUSTOMER_ACCOUNTS(
            "/customer/accounts",
            BaseModel.class,
            AccountResponse.class
    ),
    TRANSFER(
            "/accounts/transfer",
            TransferRequest.class,
            TransferResponse.class
    ),
    CUSTOMER_PROFILE(
            "/customer/profile",
            BaseModel.class,
            CustomerResponse.class
    ),
    UPDATE_CUSTOMER_PROFILE(
            "/customer/profile",
            UpdateProfileRequest.class,
            UpdateProfileResponse.class
    ),
    TRANSFER_WITH_FRAUD_CHECK(
            "/accounts/transfer-with-fraud-check",
            TransferRequest.class,
            FraudCheckTransferResponse.class
    );

    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends BaseModel> responseModel;
}
