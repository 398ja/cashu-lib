package cashu.common.protocol;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class BaseAbility<T> implements Ability {

    private final Task<T> task;

    @Override
    public T apply() {
        return task.execute();
    }

    public static interface Task<T> {

        T execute();
    }
}
