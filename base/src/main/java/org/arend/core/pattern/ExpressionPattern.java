package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.body.CoreExpressionPattern;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ExpressionPattern extends Pattern, CoreExpressionPattern {
  @Override Expression toExpression();
  Decision match(Expression expression, List<Expression> result);
  boolean unify(ExprSubstitution idpSubst, ExpressionPattern other, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode);
  @Nullable ExpressionPattern intersect(ExpressionPattern other);
  ExpressionPattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, ExpressionPattern> patternSubst);
  Pattern removeExpressions();
  @Override @NotNull List<? extends ExpressionPattern> getSubPatterns();
  Concrete.Pattern toConcrete(Object data, boolean isExplicit, Map<DependentLink, Concrete.Pattern> subPatterns);

  default Expression toPatternExpression() {
    return toExpression();
  }

  @Override
  default LineDoc prettyPrint(PrettyPrinterConfig ppConfig) {
    return DocFactory.pattern(this, ppConfig);
  }

  static List<Expression> toExpressions(List<? extends ExpressionPattern> patterns) {
    List<Expression> result = new ArrayList<>(patterns.size());
    for (ExpressionPattern pattern : patterns) {
      result.add(pattern.toExpression());
    }
    return result;
  }

  static List<Pattern> removeExpressions(List<? extends ExpressionPattern> patterns) {
    List<Pattern> result = new ArrayList<>();
    for (ExpressionPattern pattern : patterns) {
      result.add(pattern.removeExpressions());
    }
    return result;
  }

  static Decision match(List<? extends ExpressionPattern> patterns, List<? extends Expression> expressions, List<Expression> result) {
    assert patterns.size() == expressions.size();

    Decision decision = Decision.YES;
    for (int i = 0; i < patterns.size(); i++) {
      Decision subDecision = patterns.get(i).match(expressions.get(i), result);
      if (subDecision == Decision.NO) {
        return subDecision;
      }
      if (subDecision == Decision.MAYBE) {
        decision = Decision.MAYBE;
      }
    }

    return decision;
  }

  static List<Expression> applyClauseArguments(List<? extends ExpressionPattern> patterns, List<? extends Expression> clauseArguments, LevelSubstitution levelSubst) {
    ExprSubstitution substitution = new ExprSubstitution().add(Pattern.getFirstBinding(patterns), clauseArguments);
    List<Expression> result = new ArrayList<>(patterns.size());
    for (ExpressionPattern pattern : patterns) {
      result.add(pattern.toExpression().subst(substitution, levelSubst));
    }
    return result;
  }

  static boolean unify(List<? extends ExpressionPattern> patterns1, List<? extends ExpressionPattern> patterns2, ExprSubstitution idpSubst, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    assert patterns1.size() == patterns2.size();
    for (int i = 0; i < patterns1.size(); i++) {
      if (!patterns1.get(i).unify(idpSubst, patterns2.get(i), substitution1, substitution2, errorReporter, sourceNode)) {
        return false;
      }
    }
    return true;
  }

  static boolean compare(List<? extends ExpressionPattern> patterns1, List<? extends ExpressionPattern> patterns2) {
    if (patterns1.size() != patterns2.size()) return false;
    for (int i = 0; i < patterns1.size(); i++) {
      if (!compare(patterns1.get(i), patterns2.get(i))) {
        return false;
      }
    }
    return true;
  }

  static boolean compare(ExpressionPattern pattern1, ExpressionPattern pattern2) {
    return pattern1.isAbsurd() == pattern2.isAbsurd() && pattern1.getBinding() == pattern2.getBinding() && pattern1.getDefinition() == pattern2.getDefinition() && compare(pattern1.getSubPatterns(), pattern2.getSubPatterns());
  }
}
