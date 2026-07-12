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
    private String username;
    private String password;

    public UserSteps(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public AccountResponse createAccount(){
        return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(username, password), Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
    }

    public  List<AccountResponse> getAllAccounts() {
        return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getAll(AccountResponse[].class);
    }

    public  AccountResponse deposit(int accountId, double amount){
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();
       return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(username,password),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositRequest);
    }

    public  UpdateProfileRequest updateName() {
        UpdateProfileRequest updateProfileRequest = UpdateProfileRequest.builder()
                .name(RandomData.getValidName())
                .build();
        new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .update(updateProfileRequest);
        return updateProfileRequest;
    }

    public TransferResponse transfer(int senderAccountId, int receiverAccountId, double amount){
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();
        return new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(transferRequest);
    }

    public CustomerResponse getCustomerProfile() {
        return new ValidatedCrudRequester<CustomerResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get();
    }
}
