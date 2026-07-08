package ui.steps;

import api.models.AccountResponse;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;

public class UiUserSteps {
    private String username;
    private String password;

    public UiUserSteps(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public List<AccountResponse> getAllAccounts() {
        return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()).getAll(AccountResponse[].class);
    }
}
