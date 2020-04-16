package org.arend.typechecking.patternmatching;

import org.arend.core.context.binding.Variable;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterConfigImpl;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.prettyprinting.doc.LineDoc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class SubstitutionData {
  public final Expression expression;
  public final ExprSubstitution substitution;

  public SubstitutionData(Expression expression, ExprSubstitution substitution) {
    this.expression = expression;
    this.substitution = substitution;
  }

  public Doc toDoc(PrettyPrinterConfig ppConfig) {
    PrettyPrinterConfigImpl newPPConfig = new PrettyPrinterConfigImpl(ppConfig);
    newPPConfig.normalizationMode = null;
    Doc doc = expression == null ? null : termDoc(expression, newPPConfig);

    if (substitution != null && !substitution.isEmpty()) {
      List<LineDoc> substDocs = new ArrayList<>(substitution.getEntries().size());
      for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
        String name = entry.getKey().getName() ;
        substDocs.add(hList(text((name == null ? "_" : name) + " = "), termLine(entry.getValue(), ppConfig)));
      }
      Doc list = hList(text("["), hSep(text(", "), substDocs), text("]"));
      doc = doc == null ? list : hang(doc, list);
    }

    return doc == null ? text("[]") : doc;
  }

  @Override
  public String toString() {
    return DocStringBuilder.build(toDoc(PrettyPrinterConfig.DEFAULT));
  }
}
