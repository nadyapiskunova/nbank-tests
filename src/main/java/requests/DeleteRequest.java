package requests;

import io.restassured.response.ValidatableResponse;

public interface DeleteRequest<T> {
    ValidatableResponse delete(T value);
}