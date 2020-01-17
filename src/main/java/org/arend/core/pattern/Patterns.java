package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.error.ErrorReporter;
import org.arend.term.concrete.Concrete;

import java.util.List;

public class Patterns {
  private final List<Pattern> myPatterns;

  public Patterns(List<Pattern> patterns) {
    myPatterns = patterns;
  }

  public List<Pattern> getPatternList() {
    return myPatterns;
  }

  public DependentLink getFirstBinding() {
    for (Pattern pattern : myPatterns) {
      DependentLink result = pattern.getFirstBinding();
      if (result.hasNext()) {
        return result;
      }
    }
    return EmptyDependentLink.getInstance();
  }

  public DependentLink getLastBinding() {
    for (int i = myPatterns.size() - 1; i >= 0; i--) {
      DependentLink result = myPatterns.get(i).getLastBinding();
      if (result.hasNext()) {
        return result;
      }
    }
    return EmptyDependentLink.getInstance();
  }

  public Pattern.MatchResult match(List<? extends Expression> expressions, List<Expression> result) {
    assert myPatterns.size() == expressions.size();

    Pattern.MatchResult matchResult = Pattern.MatchResult.OK;
    for (int i = 0; i < myPatterns.size(); i++) {
      Pattern.MatchResult subMatch = myPatterns.get(i).match(expressions.get(i), result);
      if (subMatch == Pattern.MatchResult.FAIL) {
        return subMatch;
      }
      if (subMatch == Pattern.MatchResult.MAYBE) {
        matchResult = Pattern.MatchResult.MAYBE;
      }
    }

    return matchResult;
  }

  public boolean unify(ExprSubstitution idpSubst, Patterns other, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    assert myPatterns.size() == other.myPatterns.size();
    for (int i = 0; i < myPatterns.size(); i++) {
      if (!myPatterns.get(i).unify(idpSubst, other.myPatterns.get(i), substitution1, substitution2, errorReporter, sourceNode)) {
        return false;
      }
    }
    return true;
  }
}
