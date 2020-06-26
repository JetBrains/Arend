package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.subst.ExprSubstitution;
import org.arend.prelude.Prelude;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ConstructorPattern<T> implements Pattern {
  protected final T data;
  private final List<? extends Pattern> mySubPatterns;

  protected ConstructorPattern(T data, List<? extends Pattern> subPatterns) {
    this.data = data;
    mySubPatterns = subPatterns;
  }

  public static ConstructorPattern<Definition> make(Definition definition, List<? extends Pattern> subPatterns) {
    return new ConstructorPattern<>(definition, subPatterns) {
      @Override
      public Definition getDefinition() {
        return data;
      }

      @Override
      public DependentLink replaceBindings(DependentLink link, List<Pattern> result) {
        List<Pattern> subPatterns = new ArrayList<>();
        result.add(make(data, subPatterns));
        for (Pattern pattern : getSubPatterns()) {
          link = pattern.replaceBindings(link, subPatterns);
        }
        return link;
      }
    };
  }

  @Override
  public ConstructorExpressionPattern toExpressionPattern(Expression type) {
    if (type instanceof DataCallExpression && getDefinition() instanceof Constructor) {
      Constructor constructor = (Constructor) getDefinition();
      DataCallExpression dataCall = (DataCallExpression) type;
      List<ExpressionPattern> subPatterns = Pattern.toExpressionPatterns(mySubPatterns, DependentLink.Helper.subst(constructor.getParameters(), new ExprSubstitution().add(constructor.getDataTypeParameters(), dataCall.getDefCallArguments())));
      if (subPatterns == null) {
        return null;
      }

      List<Expression> args = constructor.matchDataTypeArguments(dataCall.getDefCallArguments());
      if (args == null) {
        return null;
      }

      return new ConstructorExpressionPattern(new ConCallExpression(constructor, dataCall.getSortArgument(), args, Collections.emptyList()), subPatterns);
    } else if (type instanceof DataCallExpression && getDefinition() == Prelude.IDP) {
      FunCallExpression equality = type.toEquality();
      if (equality == null) {
        return null;
      }
      return new ConstructorExpressionPattern(FunCallExpression.makeFunCall(Prelude.IDP, equality.getSortArgument(), Arrays.asList(equality.getDefCallArguments().get(0), equality.getDefCallArguments().get(1))), Collections.emptyList());
    } else if (type instanceof ClassCallExpression) {
      ClassCallExpression classCall = (ClassCallExpression) type;
      List<ExpressionPattern> subPatterns = Pattern.toExpressionPatterns(mySubPatterns, classCall.getClassFieldParameters());
      if (subPatterns == null) {
        return null;
      }
      return new ConstructorExpressionPattern(classCall, subPatterns);
    } else if (type instanceof SigmaExpression) {
      SigmaExpression sigma = (SigmaExpression) type;
      List<ExpressionPattern> subPatterns = Pattern.toExpressionPatterns(mySubPatterns, sigma.getParameters());
      if (subPatterns == null) {
        return null;
      }
      return new ConstructorExpressionPattern(sigma, subPatterns);
    } else {
      return null;
    }
  }

  @Override
  public DependentLink getFirstBinding() {
    return Pattern.getFirstBinding(mySubPatterns);
  }

  @Override
  public DependentLink getLastBinding() {
    return Pattern.getFirstBinding(mySubPatterns);
  }

  @NotNull
  @Override
  public List<? extends Pattern> getSubPatterns() {
    return mySubPatterns;
  }
}
