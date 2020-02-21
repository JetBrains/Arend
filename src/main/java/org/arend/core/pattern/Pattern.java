package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.ext.core.ops.NormalizationMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Pattern {
  DependentLink getFirstBinding();
  DependentLink getLastBinding();
  Definition getDefinition();
  List<? extends Pattern> getSubPatterns();
  DependentLink replaceBindings(DependentLink link, List<Pattern> result);
  ExpressionPattern toExpressionPattern(Expression type);

  static DependentLink getFirstBinding(Collection<? extends Pattern> patterns) {
    for (Pattern pattern : patterns) {
      DependentLink link = pattern.getFirstBinding();
      if (link.hasNext()) {
        return link;
      }
    }
    return EmptyDependentLink.getInstance();
  }

  static DependentLink getLastBinding(List<? extends Pattern> patterns) {
    for (int i = patterns.size() - 1; i >= 0; i--) {
      DependentLink link = patterns.get(i).getLastBinding();
      if (link.hasNext()) {
        return link;
      }
    }
    return EmptyDependentLink.getInstance();
  }

  static List<Pattern> replaceBindings(List<? extends Pattern> patterns, DependentLink link) {
    List<Pattern> result = new ArrayList<>();
    for (Pattern pattern : patterns) {
      link = pattern.replaceBindings(link, result);
    }
    return result;
  }

  static List<ExpressionPattern> toExpressionPatterns(List<? extends Pattern> patterns, DependentLink link) {
    List<ExpressionPattern> result = new ArrayList<>();
    for (Pattern pattern : patterns) {
      if (pattern instanceof ExpressionPattern) {
        result.add((ExpressionPattern) pattern);
      } else if (pattern instanceof ConstructorPattern) {
        result.add(pattern.toExpressionPattern(link.getTypeExpr().normalize(NormalizationMode.WHNF).getUnderlyingExpression()));
      } else {
        throw new IllegalStateException();
      }
      link = link.getNext();
    }
    return result;
  }
}
