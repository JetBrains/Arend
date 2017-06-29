package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class Patterns {
  private final List<Pattern> myPatterns;

  public Patterns(List<Pattern> patterns) {
    myPatterns = patterns;
  }

  public List<Pattern> getPatterns() {
    return myPatterns;
  }

  public DependentLink getParameters() {
    for (Pattern pattern : myPatterns) {
      DependentLink result = pattern.getParameters();
      if (result.hasNext()) {
        return result;
      }
    }
    return EmptyDependentLink.getInstance();
  }

  public Pattern.MatchResult match(List<? extends Expression> exprs) {
    return match(exprs, true);
  }

  public Pattern.MatchResult match(List<? extends Expression> exprs, boolean normalize) {
    assert myPatterns.size() == exprs.size();
    List<Expression> result = new ArrayList<>();

    Pattern.MatchMaybeResult maybe = null;
    for (int i = 0; i < myPatterns.size(); i++) {
      Pattern.MatchResult subMatch = myPatterns.get(i).match(exprs.get(i), normalize);
      if (subMatch instanceof Pattern.MatchFailedResult) {
        return subMatch;
      }
      if (maybe == null) {
        if (subMatch instanceof Pattern.MatchMaybeResult) {
          maybe = (Pattern.MatchMaybeResult) subMatch;
        } else if (subMatch instanceof Pattern.MatchOKResult) {
          result.addAll(((Pattern.MatchOKResult) subMatch).expressions);
        }
      }
    }

    return maybe != null ? maybe : new Pattern.MatchOKResult(result);
  }
}
