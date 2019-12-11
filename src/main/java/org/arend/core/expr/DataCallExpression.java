package org.arend.core.expr;

import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.ErrorReporter;

import java.util.ArrayList;
import java.util.List;

public class DataCallExpression extends DefCallExpression implements Type {
  private final Sort mySortArgument;
  private final List<Expression> myArguments;

  public DataCallExpression(DataDefinition definition, Sort sortArgument, List<Expression> arguments) {
    super(definition);
    mySortArgument = sortArgument;
    myArguments = arguments;
  }

  @Override
  public Sort getSortArgument() {
    return mySortArgument;
  }

  @Override
  public List<Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public DataDefinition getDefinition() {
    return (DataDefinition) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDataCall(this, params);
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return getDefinition().getSort().subst(mySortArgument.toLevelSubstitution());
  }

  @Override
  public DataCallExpression subst(SubstVisitor substVisitor) {
    return substVisitor.isEmpty() ? this : substVisitor.visitDataCall(this, null);
  }

  @Override
  public DataCallExpression strip(ErrorReporter errorReporter) {
    return new StripVisitor(errorReporter).visitDataCall(this, null);
  }

  @Override
  public DataCallExpression normalize(NormalizeVisitor.Mode mode) {
    return NormalizeVisitor.INSTANCE.visitDataCall(this, mode);
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
      Pattern.MatchResult matchResult = constructor.getPatterns().match(myArguments, matchedParameters);
      if (matchResult == Pattern.MatchResult.MAYBE) {
        return false;
      }
      if (matchResult == Pattern.MatchResult.FAIL) {
        return true;
      }
    } else {
      matchedParameters = myArguments;
    }

    conCalls.add(new ConCallExpression(constructor, mySortArgument, matchedParameters, new ArrayList<>()));
    return true;
  }
}
