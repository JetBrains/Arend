package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DConstructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.ops.NormalizationMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Pattern extends CorePattern {
  DependentLink getFirstBinding();
  DependentLink getLastBinding();
  Definition getDefinition();
  @Override @NotNull List<? extends Pattern> getSubPatterns();
  DependentLink replaceBindings(DependentLink link, List<Pattern> result);
  ExpressionPattern toExpressionPattern(Expression type);
  @Override @NotNull Pattern subst(@NotNull Map<? extends CoreBinding, ? extends CorePattern> map);

  @Override
  default Definition getConstructor() {
    Definition def = getDefinition();
    return def instanceof Constructor || def instanceof DConstructor ? def : null;
  }

  @Override
  default @NotNull CoreParameter getAllBindings() {
    return getFirstBinding();
  }

  @Override
  @Nullable
  default CoreBinding getBinding() {
    return null;
  }

  @Override
  default boolean isAbsurd() {
    return false;
  }

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
    ExprSubstitution substitution = new ExprSubstitution();
    List<ExpressionPattern> result = new ArrayList<>();
    for (Pattern pattern : patterns) {
      ExpressionPattern exprPattern;
      if (pattern instanceof ExpressionPattern) {
        exprPattern = (ExpressionPattern) pattern;
      } else if (pattern instanceof ConstructorPattern) {
        exprPattern = pattern.toExpressionPattern(link.getTypeExpr().subst(substitution).normalize(NormalizationMode.WHNF).getUnderlyingExpression());
        if (exprPattern == null) {
          return null;
        }
      } else {
        throw new IllegalStateException();
      }

      substitution.add(link, exprPattern.toExpression());
      result.add(exprPattern);
      link = link.getNext();
    }
    return result;
  }
}
