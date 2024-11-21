package org.arend.naming.scope;

import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.group.AccessModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

public class PrivateFilteredScope extends DelegateScope {
  private final boolean myDiscardPrivate;

  public PrivateFilteredScope(Scope parent, boolean discardPrivate) {
    super(parent);
    myDiscardPrivate = discardPrivate;
  }

  public PrivateFilteredScope(Scope parent) {
    super(parent);
    myDiscardPrivate = false;
  }

  @Override
  public @NotNull Collection<? extends Referable> getElements(Referable.@Nullable RefKind kind) {
    return myDiscardPrivate ? parent.getElements(kind).stream().filter(ref -> !(ref instanceof GlobalReferable) || ((GlobalReferable) ref).getAccessModifier() != AccessModifier.PRIVATE).toList() : parent.getElements(kind);
  }

  @Override
  public @Nullable Referable find(Predicate<Referable> pred) {
    return myDiscardPrivate ? parent.find(ref -> (!(ref instanceof GlobalReferable) || ((GlobalReferable) ref).getAccessModifier() != AccessModifier.PRIVATE) && pred.test(ref)) : parent.find(pred);
  }

  @Override
  public @Nullable Referable resolveName(@NotNull String name, Referable.@Nullable RefKind kind) {
    Referable ref = parent.resolveName(name, kind);
    return myDiscardPrivate && ref instanceof GlobalReferable && ((GlobalReferable) ref).getAccessModifier() == AccessModifier.PRIVATE ? null : ref;
  }

  @Override
  public @Nullable Scope resolveNamespace(@NotNull String name) {
    Scope result = parent.resolveNamespace(name);
    return result == null ? null : new PrivateFilteredScope(result, true);
  }
}
