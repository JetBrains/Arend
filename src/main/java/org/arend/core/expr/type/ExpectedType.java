package org.arend.core.expr.type;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.prettyprint.PrettyPrintable;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.List;

public interface ExpectedType extends PrettyPrintable {
  ExpectedType normalize(NormalizeVisitor.Mode mode);
  ExpectedType getPiParameters(List<? super SingleDependentLink> params, boolean implicitOnly);
  ExpectedType subst(ExprSubstitution subst);
  ExpectedType subst(LevelSubstitution subst);

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
    public ExpectedType getPiParameters(List<? super SingleDependentLink> params, boolean implicitOnly) {
      return this;
    }

    @Override
    public ExpectedType subst(ExprSubstitution subst) {
      return this;
    }

    @Override
    public ExpectedType subst(LevelSubstitution subst) {
      return this;
    }
  };
}
