package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.List;

public interface ExpectedType extends PrettyPrintable {
  ExpectedType normalize(NormalizeVisitor.Mode mode);
  ExpectedType getPiParameters(List<SingleDependentLink> params, boolean implicitOnly);

  ExpectedType OMEGA = new ExpectedType() {
    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      builder.append("a universe");
    }

    @Override
    public Doc prettyPrint(PrettyPrinterConfig ppConfig) {
      return DocFactory.text("a universe");
    }

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
