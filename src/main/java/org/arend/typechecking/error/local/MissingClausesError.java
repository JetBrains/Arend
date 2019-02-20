package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.error.doc.Doc;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.*;

public class MissingClausesError extends TypecheckingError {
  private final List<List<Expression>> myMissingClauses;

  public MissingClausesError(List<List<Expression>> missingClauses, Concrete.SourceNode cause) {
    super("Some clauses are missing", cause);
    myMissingClauses = missingClauses;
  }

  public List<List<Expression>> getMissingClauses() {
    return myMissingClauses;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    PrettyPrinterConfig modPPConfig = new PrettyPrinterConfig() {
      @Override
      public NormalizeVisitor.Mode getNormalizationMode() {
        return null;
      }
    };

    List<LineDoc> docs = new ArrayList<>(myMissingClauses.size());
    for (List<Expression> missingClause : myMissingClauses) {
      docs.add(missingClause == null ? text("...") : hSep(text(", "), missingClause.stream().map(expr -> termLine(expr, modPPConfig)).collect(Collectors.toList())));
    }
    return vList(docs);
  }
}
