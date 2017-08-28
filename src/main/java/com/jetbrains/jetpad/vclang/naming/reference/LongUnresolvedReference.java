package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import java.util.List;

public class LongUnresolvedReference extends NamedUnresolvedReference {
  private final List<String> myPath;

  public LongUnresolvedReference(@Nonnull String name, @Nonnull List<String> path) {
    super(name);
    myPath = path;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    StringBuilder builder = new StringBuilder();
    builder.append(super.textRepresentation());
    for (String name : myPath) {
      builder.append('.').append(name);
    }
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
  public Referable resolve(Scope scope, NameResolver nameResolver) {
    if (resolved != null) {
      return resolved;
    }
    super.resolve(scope, nameResolver);

    for (String name : myPath) {
      if (!(resolved instanceof GlobalReferable)) {
        resolved = this;
        return this;
      }
      resolved = nameResolver.nsProviders.statics.forReferable((GlobalReferable) resolved).resolveName(name);
    }

    return resolved;
  }
}
