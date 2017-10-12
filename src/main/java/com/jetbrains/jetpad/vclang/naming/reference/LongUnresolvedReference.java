package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class LongUnresolvedReference extends NamedUnresolvedReference {
  private final List<String> myPath;

  private LongUnresolvedReference(Object data, @Nonnull String name, @Nonnull List<String> path) {
    super(data, name);
    myPath = path;
  }

  public static NamedUnresolvedReference make(Object data, @Nonnull String name, @Nonnull List<String> path) {
    return path.isEmpty() ? new NamedUnresolvedReference(data, name) : new LongUnresolvedReference(data, name, path);
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
    resolved = scope.resolveName(super.textRepresentation());

    resolvePath(nameResolver);
    return resolved;
  }

  @Nullable
  @Override
  public Referable resolveStatic(GlobalReferable enclosingClass, NameResolver nameResolver) {
    if (resolved != null) {
      return resolved;
    }
    if (enclosingClass != null && nameResolver != null) {
      resolved = nameResolver.nsProviders.statics.forReferable(enclosingClass).resolveName(super.textRepresentation());
    }

    return resolvePath(nameResolver);
  }

  @Nullable
  @Override
  public Referable resolveDynamic(GlobalReferable enclosingClass, NameResolver nameResolver) {
    if (resolved != null) {
      return resolved;
    }
    if (enclosingClass != null && nameResolver != null) {
      resolved = nameResolver.nsProviders.dynamics.forReferable(enclosingClass).resolveName(super.textRepresentation());
    }

    return resolvePath(nameResolver);
  }

  private Referable resolvePath(NameResolver nameResolver) {
    if (!(resolved instanceof GlobalReferable) || !myPath.isEmpty() && nameResolver == null) {
      resolved = new ErrorReference(getData(), null, resolved.textRepresentation());
      return resolved;
    }

    for (String name : myPath) {
      Referable newResolved = nameResolver.nsProviders.statics.forReferable((GlobalReferable) resolved).resolveName(name);
      if (newResolved == null) {
        resolved = new ErrorReference(getData(), resolved, name);
        return resolved;
      } else {
        resolved = newResolved;
      }
    }

    return resolved;
  }
}
