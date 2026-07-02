package requests.skeleton.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skeleton.Endpoint;
import requests.skeleton.HttpRequest;
import requests.skeleton.interfaces.CrudEndpointInterface;
import io.restassured.common.mapper.TypeRef;

import java.util.List;

@SuppressWarnings("unchecked")
public class ValidatedCrudRequester<M extends BaseModel> extends HttpRequest implements CrudEndpointInterface {
    private final CrudRequester crudRequester;

    public ValidatedCrudRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification, Endpoint endpoint) {
        super(requestSpecification, responseSpecification, endpoint);
        this.crudRequester = new CrudRequester(requestSpecification, responseSpecification, endpoint);
    }

    @Override
    public M post(BaseModel model) {
        return (M) crudRequester.post(model).extract().as(endpoint.getResponseModel());
    }
    @Override
    public M post() {
        return (M) crudRequester.post().extract().as(endpoint.getResponseModel());
    }

    @Override
    public Object get(int id) {
        return null;
    }

    @Override
    public M get() {
        return (M) crudRequester.get().extract().as(endpoint.getResponseModel());
    }

    public List<M> getAsList(TypeRef<List<M>> typeRef) {
        return crudRequester.get()
                .extract()
                .body()
                .as(typeRef);
    }

    @Override
    public Object update(BaseModel model) {
        return null;
    }

    @Override
    public Object delete(int id) {
        return null;
    }
}
