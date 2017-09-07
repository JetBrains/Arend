package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class LongUnresolvedReference extends NamedUnresolvedReference {
  private final List<String> myPath;

  public LongUnresolvedReference(Object data, @Nonnull String name, @Nonnull List<String> path) {
    super(data, name);
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
    if (scope != null) {
      resolved = scope.resolveName(super.textRepresentation());
    }

    resolvePath(nameResolver);
    return resolved;
  }

  @Nullable
  @Override
  public Referable resolve(GlobalReferable enclosingClass, NameResolver nameResolver) {
    if (resolved != null) {
      return resolved;
    }
    if (enclosingClass != null && nameResolver != null) {
      resolved = nameResolver.nsProviders.statics.forReferable(enclosingClass).resolveName(super.textRepresentation());
    }

    return resolvePath(nameResolver);
  }

  private Referable resolvePath(NameResolver nameResolver) {
    if (!(resolved instanceof GlobalReferable) || !myPath.isEmpty() && nameResolver == null) {
      resolved = this;
      return null;
    }

    for (String name : myPath) {
      resolved = nameResolver.nsProviders.statics.forReferable((GlobalReferable) resolved).resolveName(name);
      if (resolved == null) {
        resolved = this;
        return null;
      }
    }

    return resolved;
  }
}
