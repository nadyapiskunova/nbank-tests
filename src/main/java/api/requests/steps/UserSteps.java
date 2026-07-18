package api.requests.steps;

import api.generators.RandomData;
import api.models.*;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import io.restassured.common.mapper.TypeRef;

import java.util.List;

public class UserSteps {
    private UserSteps(){};

    public static AccountResponse createAccount(CreateUserRequest userRequest){
        return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()), Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
    }

    public static List<AccountResponse> getAccounts(CreateUserRequest userRequest) {
        return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getAll(AccountResponse[].class);
    }

    public static AccountResponse deposit(CreateUserRequest userRequest, int accountId, double amount){
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();
       return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);
    }

    public static UpdateProfileRequest updateName(CreateUserRequest userRequest) {
        UpdateProfileRequest updateProfileRequest = UpdateProfileRequest.builder()
                .name(RandomData.getValidName())
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .update(updateProfileRequest);
        return updateProfileRequest;
    }

    public static TransferResponse transfer(CreateUserRequest userRequest, int senderAccountId, int receiverAccountId, double amount){
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();
        return new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(transferRequest);
    }

    public static CustomerResponse getCustomerProfile(CreateUserRequest userRequest) {
        return new ValidatedCrudRequester<CustomerResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get();
    }
}
