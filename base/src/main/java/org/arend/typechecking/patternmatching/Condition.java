package org.arend.typechecking.patternmatching;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Map;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class Condition extends SubstitutionData {
  public final Expression result;

  public Condition(@Nullable Expression expression, @Nullable ExprSubstitution substitution, @Nullable Expression result) {
    super(expression, substitution);
    this.result = result;
  }

  @Override
  public Doc toDoc(PrettyPrinterConfig ppConfig) {
    return hang(super.toDoc(ppConfig), result == null ? text("does not evaluate") : hang(text("=>"), termDoc(result, ppConfig)));
  }

  @Override
  public String toString() {
    return DocStringBuilder.build(toDoc(PrettyPrinterConfig.DEFAULT));
  }

  @TestOnly
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Condition)) {
      return false;
    }

    Condition condition = (Condition) obj;
    CompareVisitor visitor = new CompareVisitor(DummyEquations.getInstance(), CMP.EQ, null);
    if (!(substitution == null && condition.substitution == null || substitution != null && condition.substitution != null && substitution.size() == condition.substitution.size())) {
      return false;
    }
    if (!(result == null && condition.result == null || result != null && condition.result != null && visitor.normalizedCompare(result, condition.result, null, true))) {
      return false;
    }
    if (!(expression == condition.expression || expression != null && condition.expression != null && visitor.normalizedCompare(expression, condition.expression, null, true))) {
      return false;
    }

    if (substitution != null) {
      for (Map.Entry<Binding, Expression> entry : substitution.getEntries()) {
        Expression value = condition.substitution.get(entry.getKey());
        if (value == null || !visitor.normalizedCompare(entry.getValue(), value, null, true)) {
          return false;
        }
      }
    }

    return true;
  }
}
