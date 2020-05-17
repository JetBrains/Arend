package org.arend.ext.dependency;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class ArendDependencyLoader {
  public static void load(@NotNull Object dependencyContainer, @NotNull ArendDependencyProvider dependencyProvider) {
    try {
      for (Field field : dependencyContainer.getClass().getDeclaredFields()) {
        Class<?> fieldType = field.getType();
        if (CoreDefinition.class.isAssignableFrom(fieldType)) {
          field.setAccessible(true);
          Dependency dependency = field.getAnnotation(Dependency.class);
          String name = dependency.name();
          CoreDefinition definition = dependencyProvider.getDefinition(ModulePath.fromString(dependency.module()), name.isEmpty() ? new LongName(field.getName()) : LongName.fromString(name), fieldType.asSubclass(CoreDefinition.class));
          field.set(dependencyContainer, definition);
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
