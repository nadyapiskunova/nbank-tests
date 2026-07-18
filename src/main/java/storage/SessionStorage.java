package storage;

import api.models.CreateUserRequest;
import api.requests.steps.UserSteps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SessionStorage {
    /*
     ThreadLocal - способ сделать SessionStorage потокобезопасным

     Каждый поток обращая к INSTANCE.get() получают свою КОПИЮ

     Map<Thread, SessionStorage>

     Тест1 : создал юзеров, положил в SessionStorage (СВОЯ КОПИЯ), работает с ними
     Тест2 : создал юзеров, положил в SessionStorage (СВОЯ КОПИЯ), работает с ними
     Тест3 : создал юзеров, положил в SessionStorage (СВОЯ КОПИЯ), работает с ними
    */

    private static final ThreadLocal<SessionStorage> INSTANCE = ThreadLocal.withInitial(SessionStorage::new);
    private final LinkedHashMap<CreateUserRequest, UserSteps> userStepsMap = new LinkedHashMap<>();
    private final List<Integer> createdUserIds = new ArrayList<>();

    private SessionStorage(){}

    public static void addUser(CreateUserRequest user, Integer userId) {
        INSTANCE.get().userStepsMap.put(user, new UserSteps(user.getUsername(), user.getPassword()));
        INSTANCE.get().createdUserIds.add(userId);
    }
    /**
     * Возвращаем объект CreateUserRequest по его порядковому номеру в списке созданных пользователей
     * @param number Порядковый номер, начиная с 1 (а не с 0)
     * @return Объект CreateUserRequest, соответствующий указанному порядковому номеру
     */
    public static CreateUserRequest getUser(int number) {
        return new ArrayList<>(INSTANCE.get().userStepsMap.keySet()).get(number-1);
    }

    public static CreateUserRequest getUser() {
        return getUser(1);
    }

    public static UserSteps getSteps(int number) {
        return new ArrayList<>(INSTANCE.get().userStepsMap.values()).get(number-1);
    }

    public static UserSteps getSteps() {
        return getSteps(1);
    }

    public static List<Integer> getCreatedUserIds() {
        return new ArrayList<>(INSTANCE.get().createdUserIds);
    }

    public static void clear(){
        INSTANCE.remove();
    }
}
