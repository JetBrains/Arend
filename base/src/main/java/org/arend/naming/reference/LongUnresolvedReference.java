package org.arend.naming.reference;

import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.naming.scope.EmptyScope;
import org.arend.naming.scope.MergeScope;
import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LongUnresolvedReference implements UnresolvedReference {
  private final Object myData;
  private final List<String> myPath;
  private Referable resolved;

  public LongUnresolvedReference(Object data, @NotNull List<String> path) {
    assert !path.isEmpty();
    myData = data;
    myPath = path;
  }

  public static UnresolvedReference make(Object data, @NotNull List<String> path) {
    return path.isEmpty() ? null : path.size() == 1 ? new NamedUnresolvedReference(data, path.get(0)) : new LongUnresolvedReference(data, path);
  }

  public List<String> getPath() {
    return myPath;
  }

  @Nullable
  @Override
  public Object getData() {
    return myData;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String name : myPath) {
      if (first) {
        first = false;
      } else {
        builder.append(".");
      }
      builder.append(name);
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

  private Referable resolve(Scope scope, List<Referable> resolvedRefs, boolean onlyTry, RefKind kind) {
    if (resolved != null) {
      return resolved;
    }

    for (int i = 0; i < myPath.size() - 1; i++) {
      if (resolvedRefs != null) {
        resolvedRefs.add(scope.resolveName(myPath.get(i)));
      }
      scope = scope.resolveNamespace(myPath.get(i));
      if (scope == null) {
        if (!onlyTry) {
          Object data = getData();
          resolved = new ErrorReference(data, make(data, myPath.subList(0, i)), i, myPath.get(i));
          if (resolvedRefs != null) {
            resolvedRefs.set(i, resolved);
          }
        } else {
          resolved = null;
        }
        return resolved;
      }
    }

    String name = myPath.get(myPath.size() - 1);
    resolved = scope.resolveName(name, kind);
    if (resolved == null && !onlyTry) {
      Object data = getData();
      resolved = new ErrorReference(data, make(data, myPath.subList(0, myPath.size() - 1)), myPath.size() - 1, name);
    }
    if (resolvedRefs != null && resolved != null) {
      resolvedRefs.add(resolved);
    }

    return resolved;
  }

  @NotNull
  @Override
  public Referable resolve(Scope scope, @Nullable List<Referable> resolvedRefs, RefKind kind) {
    return resolve(scope, resolvedRefs, false, kind);
  }

  @Nullable
  @Override
  public Referable tryResolve(Scope scope, List<Referable> resolvedRefs) {
    return resolve(scope, resolvedRefs, true, RefKind.EXPR);
  }

  private Concrete.Expression resolveArgument(Scope scope, boolean onlyTry, List<Referable> resolvedRefs) {
    if (resolved != null) {
      return null;
    }

    Scope prevScope = scope;
    for (int i = 0; i < myPath.size() - 1; i++) {
      Scope nextScope = scope.resolveNamespace(myPath.get(i));
      if (nextScope == null) {
        return resolveField(prevScope, i - 1, onlyTry, resolvedRefs);
      }

      if (resolvedRefs != null) {
        resolvedRefs.add(scope.resolveName(myPath.get(i)));
      }

      prevScope = scope;
      scope = nextScope;
    }

    String name = myPath.get(myPath.size() - 1);
    resolved = scope.resolveName(name);
    if (resolved == null) {
      if (myPath.size() == 1) {
        if (onlyTry) return null;
        resolved = new ErrorReference(getData(), name);
      } else {
        return resolveField(prevScope, myPath.size() - 2, onlyTry, resolvedRefs);
      }
    }
    if (resolvedRefs != null) {
      resolvedRefs.add(resolved);
    }

    return null;
  }

  @Nullable
  @Override
  public Concrete.Expression resolveArgument(Scope scope, List<Referable> resolvedRefs) {
    return resolveArgument(scope, false, resolvedRefs);
  }

  @Override
  public @Nullable Concrete.Expression tryResolveArgument(Scope scope, List<Referable> resolvedRefs) {
    return resolveArgument(scope, true, resolvedRefs);
  }

  @Override
  public void reset() {
    resolved = null;
  }

  @Override
  public boolean isResolved() {
    return resolved != null;
  }

  private Concrete.Expression resolveField(Scope scope, int i, boolean onlyTry, List<Referable> resolvedRefs) {
    if (i == -1) {
      resolved = scope.resolveName(myPath.get(0));
      if (resolvedRefs != null) {
        resolvedRefs.add(resolved);
      }
      i = 0;
    } else {
      resolved = resolvedRefs != null ? resolvedRefs.get(i) : scope.resolveName(myPath.get(i));
    }

    ClassReferable classRef = resolved instanceof TypedReferable ? ((TypedReferable) resolved).getTypeClassReference() : null;
    boolean withArg = classRef != null;
    if (classRef == null && resolved != null && resolved.getUnderlyingReferable() instanceof ClassReferable classRef2) {
      classRef = classRef2;
    }
    if (classRef == null) {
      if (onlyTry) {
        resolved = null;
        return null;
      }
      Object data = getData();
      boolean wasResolved = resolved != null;
      if (wasResolved) {
        i++;
      }
      resolved = new ErrorReference(data, make(data, myPath.subList(0, i)), i, myPath.get(i));

      if (resolvedRefs != null) {
        if (wasResolved) {
          resolvedRefs.add(resolved);
        } else {
          resolvedRefs.set(i, resolved);
        }
      }
      return null;
    }

    Object data = getData();
    Concrete.Expression result = withArg ? new Concrete.ReferenceExpression(data, getReferable()) : null;
    for (i++; i < myPath.size(); i++) {
      resolved = new ClassFieldImplScope(classRef, ClassFieldImplScope.Extent.WITH_DYNAMIC).resolveName(myPath.get(i));
      if (resolved == null) {
        if (onlyTry) return null;
        resolved = new ErrorReference(data, classRef, i, myPath.get(i));
        if (resolvedRefs != null) {
          resolvedRefs.add(resolved);
        }
        return result;
      }
      if (resolvedRefs != null) {
        resolvedRefs.add(resolved);
      }
      if (i == myPath.size() - 1) {
        return result;
      }
      Concrete.Expression refExpr = new Concrete.ReferenceExpression(data, getReferable());
      result = result == null ? refExpr : Concrete.AppExpression.make(data, refExpr, result, false);

      classRef = resolved instanceof TypedReferable ? ((TypedReferable) resolved).getTypeClassReference() : null;
      if (classRef == null) {
        if (onlyTry) {
          resolved = null;
          return null;
        }
        resolved = new ErrorReference(data, make(data, myPath.subList(0, i + 1)), i + 1, myPath.get(i + 1));
        if (resolvedRefs != null) {
          resolvedRefs.add(resolved);
        }
        return null;
      }
    }

    return result;
  }

  private Referable getReferable() {
    Referable ref = resolved;
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    return ref;
  }

  public Scope resolveNamespace(Scope scope) {
    if (resolved instanceof ErrorReference) {
      return null;
    }

    for (int i = 0; i < myPath.size(); i++) {
      scope = scope.resolveNamespace(myPath.get(i));
      if (scope == null) {
        Object data = getData();
        resolved = new ErrorReference(data, make(data, myPath.subList(0, i)), i, myPath.get(i));
        return null;
      }
    }

    return scope;
  }

  private Scope resolveFieldNamespace(Scope scope, int i) {
    Referable ref = scope.resolveName(myPath.get(i));
    ClassReferable classRef = ref instanceof TypedReferable ? ((TypedReferable) ref).getTypeClassReference() : null;
    if (classRef == null) {
      return EmptyScope.INSTANCE;
    }

    for (i++; i < myPath.size(); i++) {
      ref = new ClassFieldImplScope(classRef, ClassFieldImplScope.Extent.WITH_DYNAMIC).resolveName(myPath.get(i));
      classRef = ref instanceof TypedReferable ? ((TypedReferable) ref).getTypeClassReference() : null;
      if (classRef == null) {
        return EmptyScope.INSTANCE;
      }
    }

    return new ClassFieldImplScope(classRef, ClassFieldImplScope.Extent.WITH_DYNAMIC);
  }

  public Scope resolveNamespaceWithArgument(Scope scope) {
    Scope prevScope = scope;
    for (int i = 0; i < myPath.size(); i++) {
      Scope nextScope = scope.resolveNamespace(myPath.get(i));
      if (nextScope == null) {
        return resolveFieldNamespace(prevScope, i == 0 ? 0 : i - 1);
      }
      prevScope = scope;
      scope = nextScope;
    }

    Referable ref = prevScope.resolveName(myPath.get(myPath.size() - 1));
    if (ref instanceof TypedReferable) {
      ClassReferable classRef = ((TypedReferable) ref).getTypeClassReference();
      if (classRef != null) {
        scope = new MergeScope(scope, new ClassFieldImplScope(classRef, ClassFieldImplScope.Extent.WITH_DYNAMIC));
      }
    }

    return scope;
  }

  public ErrorReference getErrorReference() {
    return resolved instanceof ErrorReference ? (ErrorReference) resolved : null;
  }
}
