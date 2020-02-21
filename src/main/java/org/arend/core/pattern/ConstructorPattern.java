package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.prelude.Prelude;

import java.util.ArrayList;
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
    return new ConstructorPattern<Definition>(definition, subPatterns) {
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
      return new ConstructorExpressionPattern(new ConCallExpression(constructor, dataCall.getSortArgument(), dataCall.getDefCallArguments(), Collections.emptyList()), Pattern.toExpressionPatterns(mySubPatterns, constructor.getParameters()));
    } else if (type instanceof DataCallExpression && getDefinition() == Prelude.IDP) {
      return new ConstructorExpressionPattern(new FunCallExpression(Prelude.IDP, ((DataCallExpression) type).getSortArgument(), Collections.emptyList()), Collections.emptyList());
    } else if (type instanceof ClassCallExpression) {
      ClassCallExpression classCall = (ClassCallExpression) type;
      return new ConstructorExpressionPattern(classCall, Pattern.toExpressionPatterns(mySubPatterns, classCall.getClassFieldParameters()));
    } else if (type instanceof SigmaExpression) {
      SigmaExpression sigma = (SigmaExpression) type;
      return new ConstructorExpressionPattern(sigma, Pattern.toExpressionPatterns(mySubPatterns, sigma.getParameters()));
    } else {
      throw new IllegalStateException();
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

  @Override
  public List<? extends Pattern> getSubPatterns() {
    return mySubPatterns;
  }
}
