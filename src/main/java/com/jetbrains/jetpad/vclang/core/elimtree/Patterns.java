package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

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
}
