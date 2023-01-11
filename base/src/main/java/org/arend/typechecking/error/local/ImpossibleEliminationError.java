package org.arend.typechecking.error.local;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ImpossibleEliminationError extends TypecheckingError {
  public final LeveledDefCallExpression defCall;
  public final ExprSubstitution substitution;
  public final DependentLink clauseParameters;
  public final DependentLink myParameters;
  public final List<DependentLink> myElimParams;
  public final List<Expression> myCaseExpressions;

  public ImpossibleEliminationError(LeveledDefCallExpression defCall, @NotNull Concrete.SourceNode cause, @Nullable ExprSubstitution substitution,
                                    @Nullable DependentLink clauseParameters, @Nullable DependentLink parameters, @Nullable List<DependentLink> elimParams, @Nullable List<Expression> caseExpressions) {
    super("Elimination is not possible here, cannot determine the set of eligible constructors", cause);
    this.defCall = defCall;
    this.substitution = substitution;
    this.clauseParameters = clauseParameters;
    this.myParameters = parameters;
    this.myElimParams = elimParams;
    this.myCaseExpressions = caseExpressions;
  }

  public ImpossibleEliminationError(LeveledDefCallExpression defCall, @NotNull Concrete.SourceNode cause) {
    this(defCall, cause, null, null, null, null, null);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return hList(text(defCall instanceof ClassCallExpression ? "for record " : "for data type "), termLine(defCall, ppConfig));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
