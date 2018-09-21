package org.arend.typechecking.error.local;

import org.arend.core.context.binding.Variable;
import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.ToAbstractVisitor;
import org.arend.core.subst.ExprSubstitution;
import org.arend.error.doc.Doc;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.arend.error.doc.DocFactory.*;

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
