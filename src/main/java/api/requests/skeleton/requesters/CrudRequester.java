package api.requests.skeleton.requesters;

import api.configs.Config;
import api.models.BaseModel;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.HttpRequest;
import api.requests.skeleton.interfaces.CrudEndpointInterface;
import api.requests.skeleton.interfaces.GetAllEndpointInterface;
import common.helpers.StepLogger;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
    private final static String API_VERSION = Config.getProperty("apiVersion");
    public CrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        return StepLogger.log("POST запрос на " + endpoint.getUrl(), () -> {
             return given()
                    .spec(requestSpecification)
                    .body(model)
                    .post(API_VERSION + endpoint.getUrl())
                    .then()
                    .assertThat()
                    .spec(responseSpecification);
        });
    }

    @Override
    @Step("POST запрос на {endpoint} без тела")
    public ValidatableResponse post() {
        return given()
                .spec(requestSpecification)
                .post(API_VERSION + endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    @Step("GET запрос на {endpoint} с id {id}")
    public Object get(int id) {
        return null;
    }

    @Override
    @Step("GET запрос на {endpoint} без параметров")
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get(API_VERSION + endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    @Step("PUT запрос на {endpoint} c телом {model}")
    public Object update(BaseModel model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .put(API_VERSION + endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    @Step("DELETE запрос на {endpoint} с id {id}")
    public Object delete(int id) {
        return given()
                .spec(requestSpecification)
                .delete(API_VERSION + endpoint.getUrl() + "/" + id)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    @Step("GET запрос на {endpoint}")
    public ValidatableResponse getAll(Class<?> clazz) {
        return given()
                .spec(requestSpecification)
                .get(API_VERSION + endpoint.getUrl())
                .then().assertThat()
                .spec(responseSpecification);
    }
}
