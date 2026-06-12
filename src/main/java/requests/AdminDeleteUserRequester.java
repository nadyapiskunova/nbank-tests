package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class AdminDeleteUserRequester extends BaseRequester implements DeleteRequest<Integer> {

    public AdminDeleteUserRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }
    @Override
    public ValidatableResponse delete(Integer userId) {
        return given()
                .spec(requestSpecification)
                .delete("/api/v1/admin/users/" + userId)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}