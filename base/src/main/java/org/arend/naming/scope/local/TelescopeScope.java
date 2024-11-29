package org.arend.naming.scope.local;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.EmptyScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.DelegateScope;
import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class TelescopeScope extends DelegateScope {
  private final List<? extends Abstract.Parameter> myParameters;
  private final Collection<? extends Referable> myExcluded;

  private TelescopeScope(Scope parent, List<? extends Abstract.Parameter> parameters, Collection<? extends Referable> excluded) {
    super(parent);
    myParameters = parameters;
    myExcluded = excluded;
  }

  public static Scope make(Scope parent, List<? extends Abstract.Parameter> parameters) {
    return make(parent, parameters, Collections.emptyList());
  }

  public static Scope make(Scope parent, List<? extends Abstract.Parameter> parameters, Collection<? extends Referable> excluded) {
    return parameters.isEmpty() ? parent : new TelescopeScope(parent, parameters, excluded);
  }

  @Override
  public @NotNull Collection<? extends Referable> getElements(@Nullable ScopeContext context) {
    if (!(context == null || context == ScopeContext.STATIC)) {
      return parent.getElements();
    }

    List<Referable> result = new ArrayList<>();
    Set<String> names = new HashSet<>();
    for (Abstract.Parameter parameter : myParameters) {
      List<? extends Referable> refs = parameter.getReferableList();
      for (Referable ref : refs) if (ref != null) {
        result.add(ref);
        names.add(ref.getRefName());
      }
    }

    parent.find(ref -> {
      if (!names.contains(ref.getRefName())) result.add(ref);
      return false;
    }, context);
    return result;
  }

  private Referable findHere(Predicate<Referable> pred) {
    for (int i = myParameters.size() - 1; i >= 0; i--) {
      List<? extends Referable> referables = myParameters.get(i).getReferableList();
      for (int j = referables.size() - 1; j >= 0; j--) {
        if (referables.get(j) != null && !myExcluded.contains(referables.get(j)) && pred.test(referables.get(j))) {
          return referables.get(j);
        }
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred, @Nullable ScopeContext context) {
    Referable ref = context == null || context == ScopeContext.STATIC ? findHere(pred) : null;
    return ref != null ? ref : parent.find(pred, context);
  }

  @Override
  public Referable resolveName(@NotNull String name, @Nullable ScopeContext context) {
    Referable ref = context == null || context == ScopeContext.STATIC ? findHere(ref2 -> ref2.textRepresentation().equals(name)) : null;
    return ref != null ? ref : parent.resolveName(name, context);
  }

  @Override
  public @Nullable Scope resolveNamespace(@NotNull String name) {
    return findHere(ref -> ref.textRepresentation().equals(name)) != null ? EmptyScope.INSTANCE : parent.resolveNamespace(name);
  }
}
