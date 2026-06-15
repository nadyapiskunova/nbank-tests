package requests;

import io.restassured.response.ValidatableResponse;
import models.BaseModel;

public interface PostRequest <T extends BaseModel> {
    ValidatableResponse post(T model);
}