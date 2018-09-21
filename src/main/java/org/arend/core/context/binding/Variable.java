package org.arend.core.context.binding;

public interface Variable {
  default String getName() {
    return toString();
  }
}
