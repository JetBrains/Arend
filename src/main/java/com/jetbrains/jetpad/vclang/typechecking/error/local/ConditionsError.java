package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ConditionsError extends TypecheckingError {
  public final Expression expr1;
  public final Expression expr2;
  public final ExprSubstitution substitution1;
  public final ExprSubstitution substitution2;
  public final Expression evaluatedExpr1;
  public final Expression evaluatedExpr2;

  public ConditionsError(Expression expr1, Expression expr2, ExprSubstitution substitution1, ExprSubstitution substitution2, Expression evaluatedExpr1, Expression evaluatedExpr2, Concrete.SourceNode sourceNode) {
    super("Conditions check failed:", sourceNode);
    this.expr1 = expr1;
    this.expr2 = expr2;
    this.substitution1 = substitution1;
    this.substitution2 = substitution2;
    this.evaluatedExpr1 = evaluatedExpr1;
    this.evaluatedExpr2 = evaluatedExpr2;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      dataToDoc(expr1, substitution1, evaluatedExpr1, ppConfig),
      dataToDoc(expr2, substitution2, evaluatedExpr2, ppConfig));
  }

  private Doc dataToDoc(Expression expr, ExprSubstitution substitution, Expression evaluatedExpr, PrettyPrinterConfig ppConfig) {
    Doc doc = termDoc(expr, new PrettyPrinterConfig() {
      @Override
      public boolean isSingleLine() {
        return ppConfig.isSingleLine();
      }

      @Override
      public EnumSet<ToAbstractVisitor.Flag> getExpressionFlags() {
        return ppConfig.getExpressionFlags();
      }

      @Override
      public NormalizeVisitor.Mode getNormalizationMode() {
        return null;
      }
    });
    if (substitution != null && !substitution.isEmpty()) {
      List<LineDoc> substDocs = new ArrayList<>(substitution.getEntries().size());
      for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
        String name = entry.getKey().getName() ;
        substDocs.add(hList(text((name == null ? "_" : name) + " = "), termLine(entry.getValue(), ppConfig)));
      }

      doc = hang(doc, hList(text("with "), hSep(text(", "), substDocs)));
    }

    return hang(doc, hang(text("evaluates to"), termDoc(evaluatedExpr, ppConfig)));
  }
}
