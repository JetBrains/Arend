package org.arend.ext.serialization;

import org.arend.ext.userData.Key;
import org.jetbrains.annotations.NotNull;

public abstract class SerializableKey<T> extends Key<T> {
  protected SerializableKey(@NotNull String name) {
    super(name);
  }

  public String getName() {
    return name;
  }

  public abstract @NotNull byte[] serialize(@NotNull ArendSerializer serializer, T object);

  public abstract @NotNull T deserialize(@NotNull ArendDeserializer deserializer, @NotNull byte[] data) throws DeserializationException;
}
