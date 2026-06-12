package requests;

import io.restassured.response.ValidatableResponse;
import models.BaseModel;

public interface PutRequest <T extends BaseModel> {
    ValidatableResponse put(T model);
}