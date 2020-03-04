package org.arend.typechecking.patternmatching;

import org.arend.core.context.binding.Variable;
import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;
import static org.arend.ext.prettyprinting.doc.DocFactory.termDoc;

public class Condition {
  public final Expression expression;
  public final ExprSubstitution substitution;
  public final Expression result;

  public Condition(@Nullable Expression expression, @Nullable ExprSubstitution substitution, @Nullable Expression result) {
    this.expression = expression;
    this.substitution = substitution;
    this.result = result;
  }

  public Doc toDoc(PrettyPrinterConfig ppConfig) {
    Doc doc = expression == null ? null : termDoc(expression, new PrettyPrinterConfig() {
      @Override
      public boolean isSingleLine() {
        return ppConfig.isSingleLine();
      }

      @NotNull
      @Override
      public EnumSet<PrettyPrinterFlag> getExpressionFlags() {
        return ppConfig.getExpressionFlags();
      }

      @Override
      public NormalizationMode getNormalizationMode() {
        return null;
      }
    });

    if (substitution != null && !substitution.isEmpty()) {
      List<LineDoc> substDocs = new ArrayList<>(substitution.getEntries().size());
      for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
        String name = entry.getKey().getName() ;
        substDocs.add(hList(text((name == null ? "_" : name) + " = "), termLine(entry.getValue(), ppConfig)));
      }
      Doc list = hList(text("["), hSep(text(", "), substDocs), text("]"));
      doc = doc == null ? list : hang(doc, list);
    }

    return hang(doc == null ? text("[]") : doc, result == null ? text("does not evaluate") : hang(text("=>"), termDoc(result, ppConfig)));
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
    if (!(result == null && condition.result == null || result != null && condition.result != null && visitor.normalizedCompare(result, condition.result, null))) {
      return false;
    }
    if (!(expression == condition.expression || expression != null && condition.expression != null && visitor.normalizedCompare(expression, condition.expression, null))) {
      return false;
    }

    if (substitution != null) {
      for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
        Expression value = condition.substitution.get(entry.getKey());
        if (value == null || !visitor.normalizedCompare(entry.getValue(), value, null)) {
          return false;
        }
      }
    }

    return true;
  }
}
