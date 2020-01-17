package org.arend.core.expr.type;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;

import java.util.List;

public interface ExpectedType extends PrettyPrintable {
  ExpectedType normalize(NormalizationMode mode);
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
    public ExpectedType normalize(NormalizationMode mode) {
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
