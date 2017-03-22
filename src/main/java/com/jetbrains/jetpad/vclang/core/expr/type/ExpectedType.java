package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;

import java.util.List;

public interface ExpectedType extends PrettyPrintable {
  ExpectedType normalize(NormalizeVisitor.Mode mode);
  ExpectedType getPiParameters(List<SingleDependentLink> params, boolean normalize, boolean implicitOnly);

  ExpectedType OMEGA = new ExpectedType() {
    @Override
    public ExpectedType normalize(NormalizeVisitor.Mode mode) {
      return this;
    }

    @Override
    public ExpectedType getPiParameters(List<SingleDependentLink> params, boolean normalize, boolean implicitOnly) {
      return this;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
      builder.append("a universe");
    }
  };
}
