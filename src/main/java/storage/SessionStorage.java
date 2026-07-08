package storage;

import api.models.CreateUserRequest;
import ui.steps.UiUserSteps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SessionStorage {
    private static final SessionStorage INSTANCE = new SessionStorage();

    private final LinkedHashMap<CreateUserRequest, UiUserSteps> userStepsMap = new LinkedHashMap<>();

    private SessionStorage(){}

    public static void addUsers(List<CreateUserRequest> users) {
        for(CreateUserRequest user: users) {
            INSTANCE.userStepsMap.put(user, new UiUserSteps(user.getUsername(), user.getPassword()));
        }
    }
    /**
     * Возвращаем объект CreateUserRequest по его порядковому номеру в списке созданных пользователей
     * @param number Порядковый номер, начиная с 1 (а не с 0)
     * @return Объект CreateUserRequest, соответствующий указанному порядковому номеру
     */
    public static CreateUserRequest getUser(int number) {
        return new ArrayList<>(INSTANCE.userStepsMap.keySet()).get(number-1);
    }

    public static CreateUserRequest getUser() {
        return getUser(1);
    }

    public static UiUserSteps getSteps(int number) {
        return new ArrayList<>(INSTANCE.userStepsMap.values()).get(number-1);
    }

    public static UiUserSteps getSteps() {
        return getSteps(1);
    }

    public static void clear(){
        INSTANCE.userStepsMap.clear();
    }
}
