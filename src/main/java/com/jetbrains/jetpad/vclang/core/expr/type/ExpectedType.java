package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;

import java.util.List;

public interface ExpectedType {
  ExpectedType normalize(NormalizeVisitor.Mode mode);
  ExpectedType getPiParameters(List<SingleDependentLink> params, boolean implicitOnly);

  ExpectedType OMEGA = new ExpectedType() {
    @Override
    public ExpectedType normalize(NormalizeVisitor.Mode mode) {
      return this;
    }

    @Override
    public ExpectedType getPiParameters(List<SingleDependentLink> params, boolean implicitOnly) {
      return this;
    }
  };
}
