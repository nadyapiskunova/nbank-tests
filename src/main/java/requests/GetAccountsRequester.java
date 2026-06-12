package requests;

import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.AccountResponse;

import java.util.List;

import static io.restassured.RestAssured.given;

public class GetAccountsRequester extends BaseRequester implements GetRequest {
    public GetAccountsRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    public List<AccountResponse> getAsList() {
        return get()
                .extract()
                .body()
                .as(new TypeRef<List<AccountResponse>>() {});
    }
}