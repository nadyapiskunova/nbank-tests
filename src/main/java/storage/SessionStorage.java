package storage;

import api.models.CreateUserRequest;
import api.requests.steps.UserSteps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SessionStorage {
    private static final SessionStorage INSTANCE = new SessionStorage();
    private final LinkedHashMap<CreateUserRequest, UserSteps> userStepsMap = new LinkedHashMap<>();
    private final List<Integer> createdUserIds = new ArrayList<>();

    private SessionStorage(){}

//    public static void addUsers(List<CreateUserRequest>users) {
//        for (CreateUserRequest user: users) {
//            INSTANCE.userStepsMap.put(user, new UserSteps(user.getUsername(), user.getPassword()));
//        }
//    }

    public static void addUser(CreateUserRequest user, Integer userId) {
        INSTANCE.userStepsMap.put(user, new UserSteps(user.getUsername(), user.getPassword()));
        INSTANCE.createdUserIds.add(userId);
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

    public static UserSteps getSteps(int number) {
        return new ArrayList<>(INSTANCE.userStepsMap.values()).get(number-1);
    }

    public static UserSteps getSteps() {
        return getSteps(1);
    }

    public static List<Integer> getCreatedUserIds() {
        return new ArrayList<>(INSTANCE.createdUserIds);
    }

    public static void clear(){
        INSTANCE.userStepsMap.clear();
        INSTANCE.createdUserIds.clear();
    }
}
