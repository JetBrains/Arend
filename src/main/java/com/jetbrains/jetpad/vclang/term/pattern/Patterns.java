package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class Patterns {
  private final List<PatternArgument> myPatterns;

  public Patterns(List<PatternArgument> patterns) {
    myPatterns = patterns;
  }

  public List<PatternArgument> getPatterns() {
    return myPatterns;
  }

  public DependentLink getParameters() {
    for (PatternArgument pattern : myPatterns) {
      DependentLink result = pattern.getPattern().getParameters();
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
      Pattern.MatchResult subMatch = myPatterns.get(i).getPattern().match(exprs.get(i), normalize);
      if (subMatch instanceof Pattern.MatchFailedResult) {
        return subMatch;
      } else
      if (subMatch instanceof Pattern.MatchMaybeResult) {
        if (maybe == null) {
          maybe = (Pattern.MatchMaybeResult) subMatch;
        }
      } else
      if (subMatch instanceof Pattern.MatchOKResult) {
        result.addAll(((Pattern.MatchOKResult) subMatch).expressions);
      }
    }

    return maybe != null ? maybe : new Pattern.MatchOKResult(result);
  }
}
