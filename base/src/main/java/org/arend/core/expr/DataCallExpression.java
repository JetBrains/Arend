package org.arend.core.expr;

import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.expr.CoreDataCallExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DataCallExpression extends DefCallExpression implements Type, CoreDataCallExpression {
  private final List<Expression> myArguments;

  public DataCallExpression(DataDefinition definition, Sort sortArgument, List<Expression> arguments) {
    super(definition, sortArgument);
    myArguments = arguments;
  }

  @NotNull
  @Override
  public List<Expression> getDefCallArguments() {
    return myArguments;
  }

  @NotNull
  @Override
  public DataDefinition getDefinition() {
    return (DataDefinition) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDataCall(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitDataCall(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDataCall(this, params);
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return getDefinition().getSort().subst(getSortArgument().toLevelSubstitution());
  }

  @Override
  public void subst(InPlaceLevelSubstVisitor substVisitor) {
    substVisitor.visitDataCall(this, null);
  }

  @Override
  public DataCallExpression strip(StripVisitor visitor) {
    return visitor.visitDataCall(this, null);
  }

  @NotNull
  @Override
  public DataCallExpression normalize(@NotNull NormalizationMode mode) {
    return NormalizeVisitor.INSTANCE.visitDataCall(this, mode);
  }

  @Override
  public @Nullable List<CoreConstructor> computeMatchedConstructors() {
    List<ConCallExpression> conCalls = getMatchedConstructors();
    if (conCalls == null) {
      return null;
    }

    List<CoreConstructor> constructors = new ArrayList<>();
    for (ConCallExpression conCall : conCalls) {
      constructors.add(conCall.getDefinition());
    }
    return constructors;
  }

  @Override
  public @Nullable List<ConstructorWithDataArguments> computeMatchedConstructorsWithDataArguments() {
    List<ConCallExpression> conCalls = getMatchedConstructors();
    if (conCalls == null) {
      return null;
    }

    List<ConstructorWithDataArguments> constructors = new ArrayList<>();
    for (ConCallExpression conCall : conCalls) {
      constructors.add(new ConstructorWithDataArguments(conCall.getDefinition(), conCall.getDataTypeArguments()));
    }
    return constructors;
  }

  public List<ConCallExpression> getMatchedConstructors() {
    List<ConCallExpression> result = new ArrayList<>();
    for (Constructor constructor : getDefinition().getConstructors()) {
      if (!getMatchedConCall(constructor, result)) {
        return null;
      }
    }
    return result;
  }

  public boolean getMatchedConCall(Constructor constructor, List<ConCallExpression> conCalls) {
    if (!constructor.status().headerIsOK()) {
      return true;
    }

    List<Expression> matchedParameters;
    if (constructor.getPatterns() != null) {
      matchedParameters = new ArrayList<>();
      Decision matchResult = ExpressionPattern.match(constructor.getPatterns(), myArguments, matchedParameters);
      if (matchResult == Decision.MAYBE) {
        return false;
      }
      if (matchResult == Decision.NO) {
        return true;
      }
    } else {
      matchedParameters = myArguments;
    }

    conCalls.add(new ConCallExpression(constructor, getSortArgument(), matchedParameters, new ArrayList<>()));
    return true;
  }
}
