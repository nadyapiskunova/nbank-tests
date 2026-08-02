package iteration_1.api;

import api.dao.AccountDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.models.AccountResponse;
import api.models.CreateUserRequest;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.requests.steps.DataBaseSteps;
import org.junit.jupiter.api.Test;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

public class CreateAccountTest extends BaseTest {
    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        AccountResponse accountResponse = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();

        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(accountResponse.getAccountNumber());

        DaoAndModelAssertions.assertThat(accountResponse, accountDao).match();
    }
}