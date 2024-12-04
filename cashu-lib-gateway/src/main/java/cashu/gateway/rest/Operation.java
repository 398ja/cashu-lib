package cashu.gateway.rest;

import cashu.gateway.model.Request;
import lombok.NonNull;

import java.lang.reflect.Field;

public interface Operation {
    Operation execute();

    String getResponseBody();

    String getRequestData();

    Operation addHeader(@NonNull String key, @NonNull String value);

    Operation removeHeader(@NonNull String key);

    String getHeader(@NonNull String key);

    default String replacePathVariables(String path, Request.Param param) throws IllegalAccessException {
        Field[] fields = param.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object fieldValue = field.get(param);
            if (fieldValue != null) {
                path = path.replace(":" + fieldName, fieldValue.toString());
            }
        }
        return path;
    }

}
