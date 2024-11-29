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
  public @NotNull Collection<? extends Referable> getElements(@Nullable ScopeContext context) {
    return myDiscardPrivate ? parent.getElements(context).stream().filter(ref -> !(ref instanceof GlobalReferable) || ((GlobalReferable) ref).getAccessModifier() != AccessModifier.PRIVATE).toList() : parent.getElements(context);
  }

  @Override
  public @Nullable Referable find(Predicate<Referable> pred, @Nullable ScopeContext context) {
    return myDiscardPrivate ? parent.find(ref -> (!(ref instanceof GlobalReferable) || ((GlobalReferable) ref).getAccessModifier() != AccessModifier.PRIVATE) && pred.test(ref), context) : parent.find(pred, context);
  }

  @Override
  public @Nullable Referable resolveName(@NotNull String name, @Nullable ScopeContext context) {
    Referable ref = parent.resolveName(name, context);
    return myDiscardPrivate && ref instanceof GlobalReferable && ((GlobalReferable) ref).getAccessModifier() == AccessModifier.PRIVATE ? null : ref;
  }

  @Override
  public @Nullable Scope resolveNamespace(@NotNull String name) {
    Scope result = parent.resolveNamespace(name);
    return result == null ? null : new PrivateFilteredScope(result, true);
  }
}
