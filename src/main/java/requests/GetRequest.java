package requests;

import io.restassured.response.ValidatableResponse;

public interface GetRequest {
    ValidatableResponse get();
}