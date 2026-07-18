package api.requests.skeleton.requesters;

import api.models.BaseModel;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.HttpRequest;
import api.requests.skeleton.interfaces.CrudEndpointInterface;
import api.requests.skeleton.interfaces.GetAllEndpointInterface;
import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class ValidatedCrudRequester<M extends BaseModel> extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
    private final CrudRequester crudRequester;

    public ValidatedCrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(requestSpecification, endpoint, responseSpecification);
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

    @Override
    public List<M> getAll(Class<?> clazz) {
        M[] array = (M[]) crudRequester.getAll(clazz).extract().as(clazz);
        return Arrays.asList(array);
    }
}
