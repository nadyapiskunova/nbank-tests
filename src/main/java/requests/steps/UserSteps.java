package requests.steps;

import io.restassured.common.mapper.TypeRef;
import models.AccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

public class UserSteps {

    public static AccountResponse createAccount(CreateUserRequest userRequest){
        return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated(), Endpoint.ACCOUNTS)
                .post();
    }

    public static List<AccountResponse> getAccounts(CreateUserRequest userRequest) {
        return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(
                        userRequest.getUsername(),
                        userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(),
                Endpoint.CUSTOMER_ACCOUNTS)
                .getAsList(new TypeRef<List<AccountResponse>>() {});
    }

    public static AccountResponse deposit(CreateUserRequest userRequest, int accountId, double amount){
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();
       return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(),userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(), Endpoint.DEPOSIT)
                .post(depositRequest);
    }
}
