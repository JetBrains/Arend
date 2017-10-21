package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class LongUnresolvedReference extends NamedUnresolvedReference {
  private final List<String> myPath;

  private LongUnresolvedReference(Object data, @Nonnull List<String> path, @Nonnull String name) {
    super(data, name);
    myPath = path;
  }

  public static NamedUnresolvedReference make(Object data, @Nonnull String name, @Nonnull List<String> path) {
    if (path.isEmpty()) {
      return new NamedUnresolvedReference(data, name);
    }

    List<String> path2 = new ArrayList<>(path.size());
    path2.add(name);
    for (int i = 0; i < path.size() - 1; i++) {
      path2.add(path.get(i));
    }
    return new LongUnresolvedReference(data, path2, path.get(path.size() - 1));
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    StringBuilder builder = new StringBuilder();
    for (String name : myPath) {
      builder.append(name).append('.');
    }
    builder.append(super.textRepresentation());
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    LongUnresolvedReference that = (LongUnresolvedReference) o;

    return myPath.equals(that.myPath);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myPath.hashCode();
    return result;
  }

  @Nonnull
  @Override
  public Referable resolve(Scope scope) {
    if (resolved != null) {
      return resolved;
    }

    for (String name : myPath) {
      scope = scope.resolveNamespace(name);
      if (scope == null) {
        resolved = new ErrorReference(getData(), null, textRepresentation());
        return resolved;
      }
    }

    resolved = scope.resolveName(super.textRepresentation());
    if (resolved == null) {
      resolved = new ErrorReference(getData(), null, textRepresentation());
    }

    return resolved;
  }
}
