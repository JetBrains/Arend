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

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    PrettyPrinterConfig modPPConfig = new PrettyPrinterConfig() {
      @Override
      public NormalizeVisitor.Mode getNormalizationMode() {
        return null;
      }
    };

    List<LineDoc> docs = new ArrayList<>(missingClauses.size());
    for (List<Pattern> missingClause : missingClauses) {
      docs.add(missingClause == null ? text("...") : hSep(text(", "), missingClause.stream().map(pattern -> termLine(pattern.toExpression(), modPPConfig)).collect(Collectors.toList())));
    }
    return vList(docs);
  }
}
