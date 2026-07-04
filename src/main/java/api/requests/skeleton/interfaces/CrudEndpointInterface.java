package api.requests.skeleton.interfaces;

import api.models.BaseModel;

public interface CrudEndpointInterface {
    Object post(BaseModel model);
    Object post();
    Object get(int id);
    Object get();
    Object update(BaseModel model);
    Object delete(int id);
}
