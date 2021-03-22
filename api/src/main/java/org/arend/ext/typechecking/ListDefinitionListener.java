package org.arend.ext.typechecking;

import org.arend.ext.core.definition.CoreDefinition;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ListDefinitionListener implements DefinitionListener {
  public final List<DefinitionListener> listeners;

  public ListDefinitionListener(List<DefinitionListener> listeners) {
    this.listeners = listeners;
  }

  public ListDefinitionListener() {
    this(new ArrayList<>());
  }

  @Override
  public void typechecked(@NotNull CoreDefinition definition) {
    for (DefinitionListener listener : listeners) {
      listener.typechecked(definition);
    }
  }

  @Override
  public void loaded(@NotNull CoreDefinition definition) {
    for (DefinitionListener listener : listeners) {
      listener.loaded(definition);
    }
  }

  public ListDefinitionListener addListener(DefinitionListener listener) {
    listeners.add(listener);
    return this;
  }

  public ListDefinitionListener addDeclaredListeners(Object listenersContainer) {
    try {
      for (Field field : listenersContainer.getClass().getDeclaredFields()) {
        Class<?> fieldType = field.getType();
        if (DefinitionListener.class.isAssignableFrom(fieldType)) {
          field.setAccessible(true);
          DefinitionListener listener = (DefinitionListener) field.get(listenersContainer);
          if (listener != this && listener != null) {
            addListener(listener);
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
    return this;
  }

  public static DefinitionListener join(DefinitionListener... listeners) {
    List<DefinitionListener> result = new ArrayList<>(listeners.length);
    for (DefinitionListener listener : listeners) {
      if (listener != null) result.add(listener);
    }
    return result.isEmpty() ? null : result.size() == 1 ? result.get(0) : new ListDefinitionListener(result);
  }
}
