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

  public TelescopeScope(Scope parent, List<? extends Abstract.Parameter> parameters) {
    super(parent);
    myParameters = parameters;
    myExcluded = Collections.emptyList();
  }

  public TelescopeScope(Scope parent, List<? extends Abstract.Parameter> parameters, Collection<? extends Referable> excluded) {
    super(parent);
    myParameters = parameters;
    myExcluded = excluded;
  }

  @Override
  public @NotNull Collection<? extends Referable> getElements(Referable.@Nullable RefKind kind) {
    if (!(kind == Referable.RefKind.EXPR || kind == null)) {
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
    });
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
  public Referable find(Predicate<Referable> pred) {
    Referable ref = findHere(pred);
    return ref != null ? ref : parent.find(pred);
  }

  @Override
  public Referable resolveName(@NotNull String name, @Nullable Referable.RefKind kind) {
    Referable ref = findHere(ref2 -> (kind == null || ref2.getRefKind() == kind) && ref2.textRepresentation().equals(name));
    return ref != null ? ref : parent.resolveName(name, kind);
  }

  @Override
  public @Nullable Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    return findHere(ref -> ref.textRepresentation().equals(name)) != null ? EmptyScope.INSTANCE : parent.resolveNamespace(name, onlyInternal);
  }
}
