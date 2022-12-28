package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class ListScope extends DelegateScope {
  private final List<? extends Referable> myContext;
  private final List<? extends Referable> myPLevels;
  private final List<? extends Referable> myHLevels;

  public ListScope(Scope parent, List<? extends Referable> context, List<? extends Referable> pLevels, List<? extends Referable> hLevels) {
    super(parent);
    myContext = context;
    myPLevels = pLevels;
    myHLevels = hLevels;
  }

  public ListScope(List<? extends Referable> context) {
    super(EmptyScope.INSTANCE);
    myContext = context;
    myPLevels = Collections.emptyList();
    myHLevels = Collections.emptyList();
  }

  public ListScope(Referable... context) {
    this(Arrays.asList(context));
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getElements(Referable.RefKind kind) {
    if (parent == EmptyScope.INSTANCE && kind != null) {
      return kind == Referable.RefKind.EXPR ? myContext : kind == Referable.RefKind.PLEVEL ? myPLevels : myHLevels;
    }
    List<Referable> result = new ArrayList<>();
    Set<String> names = new HashSet<>();
    if (kind == Referable.RefKind.EXPR || kind == null) {
      result.addAll(myContext);
      for (Referable referable : myContext) {
        names.add(referable.getRefName());
      }
    }
    if (kind == Referable.RefKind.PLEVEL || kind == null) {
      result.addAll(myPLevels);
      for (Referable referable : myPLevels) {
        names.add(referable.getRefName());
      }
    }
    if (kind == Referable.RefKind.HLEVEL || kind == null) {
      result.addAll(myHLevels);
      for (Referable referable : myHLevels) {
        names.add(referable.getRefName());
      }
    }

    parent.find(ref -> {
      if (!names.contains(ref.getRefName())) result.add(ref);
      return false;
    });
    return result;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (int i = myContext.size() - 1; i >= 0; i--) {
      if (pred.test(myContext.get(i))) {
        return myContext.get(i);
      }
    }
    for (int i = myPLevels.size() - 1; i >= 0; i--) {
      if (pred.test(myPLevels.get(i))) {
        return myPLevels.get(i);
      }
    }
    for (int i = myHLevels.size() - 1; i >= 0; i--) {
      if (pred.test(myHLevels.get(i))) {
        return myHLevels.get(i);
      }
    }
    return parent.find(pred);
  }

  private Referable resolveNameLocal(@NotNull String name, Referable.RefKind kind) {
    if (kind == null) {
      for (Referable.RefKind refKind : Referable.RefKind.values()) {
        Referable ref = resolveName(name, refKind);
        if (ref != null) return ref;
      }
    } else {
      List<? extends Referable> list = kind == Referable.RefKind.EXPR ? myContext : kind == Referable.RefKind.PLEVEL ? myPLevels : myHLevels;
      for (int i = list.size() - 1; i >= 0; i--) {
        if (list.get(i).getRefName().equals(name)) {
          return list.get(i);
        }
      }
    }
    return null;
  }

  @Override
  public @Nullable Referable resolveName(@NotNull String name, Referable.RefKind kind) {
    Referable ref = resolveNameLocal(name, kind);
    return ref != null ? ref : parent.resolveName(name, kind);
  }

  @Override
  public @Nullable Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    return resolveNameLocal(name, Referable.RefKind.EXPR) != null ? EmptyScope.INSTANCE : parent.resolveNamespace(name, onlyInternal);
  }
}
