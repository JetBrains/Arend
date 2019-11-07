package org.arend.typechecking.error.local;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.Pattern;
import org.arend.error.doc.Doc;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.*;

public class MissingClausesError extends TypecheckingError {
  public final @Nonnull List<List<Pattern>> missingClauses;
  public final @Nullable List<? extends Concrete.Parameter> concreteParameters;
  public final @Nonnull DependentLink parameters;
  public final @Nonnull List<DependentLink> eliminatedParameters;
  public int maxListSize = 10;

  public MissingClausesError(@Nonnull List<List<Pattern>> missingClauses, @Nullable List<? extends Concrete.Parameter> concreteParameters, @Nonnull DependentLink parameters, @Nonnull List<DependentLink> eliminatedParameters, Concrete.SourceNode cause) {
    super("Some clauses are missing", cause);
    this.missingClauses = missingClauses;
    this.concreteParameters = concreteParameters;
    this.parameters = parameters;
    this.eliminatedParameters = eliminatedParameters;
  }

  public boolean isCase() {
    return concreteParameters == null;
  }

  public boolean isElim() {
    return !eliminatedParameters.isEmpty();
  }

  public List<List<Pattern>> getLimitedMissingClauses() {
    return missingClauses.size() > maxListSize ? missingClauses.subList(0, maxListSize) : missingClauses;
  }

  public void setMaxListSize(@Nullable Integer maxSize) {
    maxListSize = maxSize == null ? missingClauses.size() : maxSize;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    PrettyPrinterConfig modPPConfig = new PrettyPrinterConfig() {
      @Override
      public NormalizeVisitor.Mode getNormalizationMode() {
        return null;
      }
    };

    List<LineDoc> docs = new ArrayList<>();
    for (List<Pattern> missingClause : getLimitedMissingClauses()) {
      docs.add(hSep(text(", "), missingClause.stream().map(pattern -> termLine(pattern.toPatternExpression(), modPPConfig)).collect(Collectors.toList())));
    }
    if (docs.size() < missingClauses.size()) {
      docs.add(text("..."));
    }
    return vList(docs);
  }

  @Override
  public boolean isShort() {
    return false;
  }

  @Override
  public boolean hasExpressions() {
    return false;
  }
}
