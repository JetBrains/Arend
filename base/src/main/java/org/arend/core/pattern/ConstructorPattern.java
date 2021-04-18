package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DConstructor;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterConfigImpl;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.prelude.Prelude;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.arend.ext.prettyprinting.doc.DocFactory.termLine;

public abstract class ConstructorPattern<T> implements Pattern {
  protected final T data;
  private final List<? extends Pattern> mySubPatterns;

  protected ConstructorPattern(T data, List<? extends Pattern> subPatterns) {
    this.data = data;
    mySubPatterns = subPatterns;
  }

  private static Expression toExpression(Pattern pattern) {
    if (pattern instanceof ExpressionPattern) {
      return ((ExpressionPattern) pattern).toPatternExpression();
    }

    if (!(pattern instanceof ConstructorPattern)) {
      throw new IllegalStateException();
    }

    ConstructorPattern<?> conPattern = (ConstructorPattern<?>) pattern;
    List<Expression> args = new ArrayList<>();
    for (Pattern subPattern : conPattern.getSubPatterns()) {
      args.add(toExpression(subPattern));
    }
    if (conPattern.data instanceof Constructor) {
      return ConCallExpression.make((Constructor) conPattern.data, Sort.STD, Collections.emptyList(), args);
    }
    if (conPattern.data instanceof FunctionDefinition) {
      return FunCallExpression.make((FunctionDefinition) conPattern.data, Sort.STD, args);
    }
    return new TupleExpression(args, new SigmaExpression(Sort.PROP, EmptyDependentLink.getInstance()));
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

      @Override
      public LineDoc prettyPrint(PrettyPrinterConfig ppConfig) {
        PrettyPrinterConfigImpl newConfig = new PrettyPrinterConfigImpl(ppConfig);
        newConfig.expressionFlags.remove(PrettyPrinterFlag.SHOW_CON_PARAMS);
        newConfig.expressionFlags.remove(PrettyPrinterFlag.SHOW_TUPLE_TYPE);
        newConfig.expressionFlags.remove(PrettyPrinterFlag.SHOW_LEVELS);
        return termLine(ConstructorPattern.toExpression(this), newConfig);
      }
    };
  }

  @Override
  public ConstructorExpressionPattern toExpressionPattern(Expression type) {
    if (type instanceof DataCallExpression && getDefinition() instanceof Constructor) {
      Constructor constructor = (Constructor) getDefinition();
      DataCallExpression dataCall = (DataCallExpression) type;

      List<Expression> args = constructor.matchDataTypeArguments(dataCall.getDefCallArguments());
      if (args == null) {
        return null;
      }

      List<ExpressionPattern> subPatterns = Pattern.toExpressionPatterns(mySubPatterns, DependentLink.Helper.subst(constructor.getParameters(), new ExprSubstitution().add(constructor.getDataTypeParameters(), args)));
      if (subPatterns == null) {
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
      if (classCall.getDefinition() == Prelude.ARRAY) {
        Definition def = getDefinition();
        if (def == Prelude.EMPTY_ARRAY || def == Prelude.ARRAY_CONS) {
          Expression elementsType = classCall.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE);
          return new ConstructorExpressionPattern(new FunCallExpression((DConstructor) def, classCall.getSortArgument(), elementsType), classCall.getAbsImplementationHere(Prelude.ARRAY_LENGTH), Pattern.toExpressionPatterns(mySubPatterns, ((DConstructor) def).getArrayParameters(classCall)));
        }
      }

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

  @Override
  public @NotNull Pattern subst(@NotNull Map<? extends CoreBinding, ? extends CorePattern> map) {
    List<Pattern> subPatterns = new ArrayList<>();
    for (Pattern subPattern : mySubPatterns) {
      subPatterns.add(subPattern.subst(map));
    }
    return make(getConstructor(), subPatterns);
  }
}
