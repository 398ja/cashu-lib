package cashu.common.protocol;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class BaseAbility<T> implements Ability<T> {

    private final Task<T> task;

    @Override
    public T apply() throws CashuErrorException {
        return task.execute();
    }

    public interface Task<T> {

        T execute() throws CashuErrorException;
    }
}
