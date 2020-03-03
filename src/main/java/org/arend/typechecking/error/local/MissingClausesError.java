package org.arend.typechecking.error.local;

import org.arend.core.context.param.DependentLink;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class MissingClausesError extends TypecheckingError {
  public final @Nonnull List<List<ExpressionPattern>> missingClauses;
  public final @Nullable List<? extends Concrete.Parameter> concreteParameters;
  public final @Nonnull DependentLink parameters;
  public final @Nonnull List<DependentLink> eliminatedParameters;
  public int maxListSize = 10;

  public MissingClausesError(@Nonnull List<List<ExpressionPattern>> missingClauses, @Nullable List<? extends Concrete.Parameter> concreteParameters, @Nonnull DependentLink parameters, @Nonnull List<DependentLink> eliminatedParameters, Concrete.SourceNode cause) {
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

  public List<List<ExpressionPattern>> getLimitedMissingClauses() {
    return missingClauses.size() > maxListSize ? missingClauses.subList(0, maxListSize) : missingClauses;
  }

  public void setMaxListSize(@Nullable Integer maxSize) {
    maxListSize = maxSize == null ? missingClauses.size() : maxSize;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    PrettyPrinterConfig modPPConfig = new PrettyPrinterConfig() {
      @Override
      public NormalizationMode getNormalizationMode() {
        return null;
      }
    };

    List<LineDoc> docs = new ArrayList<>();
    for (List<ExpressionPattern> missingClause : getLimitedMissingClauses()) {
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
}
