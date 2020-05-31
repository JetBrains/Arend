package org.arend.extImpl;

import org.arend.ext.serialization.SerializableKey;
import org.arend.ext.serialization.SerializableKeyRegistry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SerializableKeyRegistryImpl implements SerializableKeyRegistry {
  private final Map<String, SerializableKey<?>> myKeyMap = new HashMap<>();

  @Override
  public void registerKey(@NotNull SerializableKey<?> key) {
    if (myKeyMap.putIfAbsent(key.getName(), key) != null) {
      throw new IllegalArgumentException("Key '" + key.getName() + "' is already registered");
    }
  }

  @Override
  public void registerAllKeys(@NotNull Object keyContainer) {
    try {
      for (Field field : keyContainer.getClass().getDeclaredFields()) {
        Class<?> fieldType = field.getType();
        if (SerializableKey.class.isAssignableFrom(fieldType)) {
          field.setAccessible(true);
          registerKey((SerializableKey<?>) field.get(keyContainer));
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  public SerializableKey<?> getKey(String name) {
    return myKeyMap.get(name);
  }
}
