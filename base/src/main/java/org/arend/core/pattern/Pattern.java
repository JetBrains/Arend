package org.arend.core.pattern;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DConstructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.TypeCoerceExpression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.naming.renamer.Renamer;
import org.arend.prelude.Prelude;
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
  @NotNull
  default DependentLink getParameters() {
    Definition def = getDefinition();
    List<? extends Pattern> subPatterns = getSubPatterns();
    return def == Prelude.EMPTY_ARRAY && subPatterns.size() == 0 || def == Prelude.ARRAY_CONS && subPatterns.size() == 2 ? def.getParameters().getNext() : def != null ? def.getParameters() : EmptyDependentLink.getInstance();
  }

  @Override
  default @NotNull CoreParameter getAllBindings() {
    return getFirstBinding();
  }

  @Override
  @Nullable
  default Binding getBinding() {
    return null;
  }

  @Override
  default boolean isAbsurd() {
    return false;
  }

  @Override
  default String getBindingName() {
    Binding binding = getBinding();
    if (binding == null) return null;
    String name = binding.getName();
    return name == null ? Renamer.getNameFromType(binding.getTypeExpr(), null) : name;
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
        exprPattern = pattern.toExpressionPattern(TypeCoerceExpression.unfoldType(link.getTypeExpr().subst(substitution)));
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

  static Pattern fromCorePattern(CorePattern pattern) {
    if (pattern instanceof Pattern) {
      return (Pattern) pattern;
    }

    if (pattern.isAbsurd()) {
      CoreParameter param = pattern.getAllBindings();
      if (!(param instanceof DependentLink) || !param.hasNext()) {
        throw new IllegalArgumentException();
      }
      return new EmptyPattern((DependentLink) param);
    }

    CoreBinding binding = pattern.getBinding();
    if (binding != null) {
      if (!(binding instanceof DependentLink)) {
        throw new IllegalArgumentException();
      }
      return new BindingPattern((DependentLink) binding);
    }

    CoreDefinition def = pattern.getConstructor();
    if (!(def == null || def instanceof Definition)) {
      throw new IllegalArgumentException();
    }

    List<Pattern> subPatterns = new ArrayList<>();
    for (CorePattern subPattern : pattern.getSubPatterns()) {
      subPatterns.add(fromCorePattern(subPattern));
    }
    return ConstructorPattern.make((Definition) def, subPatterns);
  }
}
