package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class ReferableExtractVisitor extends BaseAbstractExpressionVisitor<Void, ClassReferable> {
  private final Scope myScope;

  public ReferableExtractVisitor(Scope scope) {
    super(null);
    myScope = scope;
  }

  @Override
  public ClassReferable visitApp(@Nullable Object data, @Nonnull Abstract.Expression expr, @Nonnull Collection<? extends Abstract.Argument> arguments, Void params) {
    return expr.accept(this, null);
  }

  @Override
  public ClassReferable visitReference(@Nullable Object data, @Nonnull Referable referent, @Nullable Abstract.LevelExpression level1, @Nullable Abstract.LevelExpression level2, Void params) {
    if (referent instanceof UnresolvedReference && myScope != null) {
      referent = ((UnresolvedReference) referent).resolve(myScope);
    }
    return referent instanceof ClassReferable ? (ClassReferable) referent : null;
  }

  @Override
  public ClassReferable visitReference(@Nullable Object data, @Nonnull Referable referent, int lp, int lh, Void params) {
    if (referent instanceof UnresolvedReference && myScope != null) {
      referent = ((UnresolvedReference) referent).resolve(myScope);
    }
    return referent instanceof ClassReferable ? (ClassReferable) referent : null;
  }

  @Override
  public ClassReferable visitClassExt(@Nullable Object data, boolean isNew, @Nullable Abstract.Expression baseClass, @Nullable Collection<? extends Abstract.ClassFieldImpl> implementations, @Nonnull Collection<? extends Abstract.BinOpSequenceElem> sequence, Void params) {
    return isNew || !sequence.isEmpty() || baseClass == null ? null : baseClass.accept(this, null);
  }

  @Override
  public ClassReferable visitPi(@Nullable Object data, @Nonnull Collection<? extends Abstract.Parameter> parameters, @Nullable Abstract.Expression codomain, Void params) {
    if (codomain == null) {
      return null;
    }

    for (Abstract.Parameter parameter : parameters) {
      if (parameter.isExplicit()) {
        return null;
      }
    }

    return codomain.accept(this, null);
  }
}
