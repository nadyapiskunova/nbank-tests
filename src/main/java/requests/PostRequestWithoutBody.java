package requests;

import io.restassured.response.ValidatableResponse;

public interface PostRequestWithoutBody {
    ValidatableResponse post();
}