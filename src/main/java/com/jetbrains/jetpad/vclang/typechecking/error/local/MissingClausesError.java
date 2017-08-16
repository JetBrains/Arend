package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class MissingClausesError extends LocalTypeCheckingError {
  private final List<List<Expression>> myMissingClauses;

  public MissingClausesError(List<List<Expression>> missingClauses, Abstract.SourceNode cause) {
    super("Some clauses are missing", cause);
    myMissingClauses = missingClauses;
  }

  public List<List<Expression>> getMissingClauses() {
    return myMissingClauses;
  }

  @Override
  public Doc getBodyDoc(SourceInfoProvider src) {
    List<LineDoc> docs = new ArrayList<>(myMissingClauses.size());
    for (List<Expression> missingClause : myMissingClauses) {
      docs.add(hSep(text(", "), missingClause.stream().map(DocFactory::termLine).collect(Collectors.toList())));
    }
    return vList(docs);
  }
}
