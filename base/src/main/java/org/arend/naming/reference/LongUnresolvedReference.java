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

  private Referable resolve(Scope scope, List<Referable> resolvedRefs, boolean onlyTry, Scope.ScopeContext context) {
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
    resolved = scope.resolveName(name, context);
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
  public Referable resolve(Scope scope, @Nullable List<Referable> resolvedRefs, @Nullable Scope.ScopeContext context) {
    return resolve(scope, resolvedRefs, false, context);
  }

  @Nullable
  @Override
  public Referable tryResolve(Scope scope, List<Referable> resolvedRefs) {
    return resolve(scope, resolvedRefs, true, Scope.ScopeContext.STATIC);
  }

  private Concrete.Expression resolveArgument(Scope scope, boolean onlyTry, List<Referable> resolvedRefs) {
    if (resolved != null) {
      return null;
    }

    Scope initialScope = scope;
    Scope prevScope = scope;
    for (int i = 0; i < myPath.size() - 1; i++) {
      Scope nextScope = scope.resolveNamespace(myPath.get(i));
      if (nextScope == null) {
        return resolveField(prevScope, initialScope, i - 1, onlyTry, resolvedRefs);
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
        return resolveField(prevScope, initialScope, myPath.size() - 2, onlyTry, resolvedRefs);
      }
    }
    if (resolvedRefs != null) {
      resolvedRefs.add(resolved);
    }

    return null;
  }

  @Nullable
  @Override
  public Concrete.Expression resolveExpression(Scope scope, List<Referable> resolvedRefs) {
    return resolveArgument(scope, false, resolvedRefs);
  }

  @Override
  public @Nullable Concrete.Expression tryResolveExpression(Scope scope, List<Referable> resolvedRefs) {
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

  private Concrete.Expression resolveField(Scope scope, Scope initialScope, int i, boolean onlyTry, List<Referable> resolvedRefs) {
    if (i == -1) {
      resolved = scope.resolveName(myPath.get(0));
      if (resolvedRefs != null) {
        resolvedRefs.add(resolved);
      }
      i = 0;
    } else {
      resolved = resolvedRefs != null ? resolvedRefs.get(i) : scope.resolveName(myPath.get(i));
    }

    if (resolved == null) {
      if (!onlyTry) {
        Object data = getData();
        resolved = new ErrorReference(data, make(data, myPath.subList(0, i)), i, myPath.get(i));
        if (resolvedRefs != null) {
          resolvedRefs.set(i, resolved);
        }
      }
      return null;
    }

    ClassReferable classRef = resolved.getUnderlyingReferable() instanceof ClassReferable classRef2 ? classRef2 : null;
    Object data = getData();
    if (classRef == null && i + 1 < myPath.size() && RedirectingReferable.getOriginalReferable(resolved) instanceof GlobalReferable globalRef && globalRef.getKind() == GlobalReferable.Kind.OTHER) {
      resolved = new ErrorReference(data, resolved, i + 1, myPath.get(i + 1));
      return null;
    }

    Concrete.Expression result = classRef == null ? new Concrete.ReferenceExpression(data, resolved) : null;
    for (i++; i < myPath.size(); i++) {
      Referable newResolved = classRef == null ? initialScope.resolveName(myPath.get(i), Scope.ScopeContext.DYNAMIC) : new ClassFieldImplScope(classRef, ClassFieldImplScope.Extent.WITH_DYNAMIC).resolveName(myPath.get(i));
      if (newResolved == null) {
        if (classRef != null) {
          if (onlyTry) return null;
          resolved = new ErrorReference(data, classRef, i, myPath.get(i));
          if (resolvedRefs != null) {
            resolvedRefs.add(resolved);
          }
          return null;
        } else {
          for (; i < myPath.size(); i++) {
            result = new Concrete.FieldCallExpression(data, myPath.get(i), result);
          }
          return result;
        }
      }
      resolved = newResolved;
      if (resolvedRefs != null) {
        resolvedRefs.add(resolved);
      }
      Concrete.Expression refExpr = new Concrete.ReferenceExpression(data, resolved);
      result = result == null ? refExpr : Concrete.AppExpression.make(data, refExpr, result, false);
      classRef = null;
    }

    return result;
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

  public Scope resolveNamespaceWithArgument(Scope scope) {
    Scope prevScope = scope;
    for (String name : myPath) {
      Scope nextScope = scope.resolveNamespace(name);
      if (nextScope == null) {
        return EmptyScope.INSTANCE;
      }
      prevScope = scope;
      scope = nextScope;
    }

    Referable ref = prevScope.resolveName(myPath.get(myPath.size() - 1));
    if (ref instanceof ClassReferable classRef) {
      scope = new MergeScope(scope, new ClassFieldImplScope(classRef, ClassFieldImplScope.Extent.WITH_DYNAMIC));
    }

    return scope;
  }

  public ErrorReference getErrorReference() {
    return resolved instanceof ErrorReference ? (ErrorReference) resolved : null;
  }
}
