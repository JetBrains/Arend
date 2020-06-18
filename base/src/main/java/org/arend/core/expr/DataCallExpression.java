package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.expr.CoreDataCallExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.arend.util.GraphClosure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;

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

  @Override
  public @Nullable List<ConstructorWithParameters> computeMatchedConstructorsWithParameters() {
    List<ConCallExpression> conCalls = getMatchedConstructors();
    if (conCalls == null) {
      return null;
    }

    List<ConstructorWithParameters> constructors = new ArrayList<>();
    for (ConCallExpression conCall : conCalls) {
      constructors.add(new ConstructorWithParameters(conCall.getDefinition(), conCall.getDataTypeArguments(), conCall.getDataTypeArguments().isEmpty() ? conCall.getDefinition().getParameters() : DependentLink.Helper.subst(conCall.getDefinition().getParameters(), new ExprSubstitution().add(getDefinition().getParameters(), conCall.getDataTypeArguments()))));
    }
    return constructors;
  }

  private static boolean addConstructor(Expression expr, Constructor constructor, GraphClosure<Constructor> closure) {
    if (expr == null) {
      return true;
    }
    if (expr instanceof ConCallExpression) {
      closure.addSymmetric(((ConCallExpression) expr).getDefinition(), constructor);
      return true;
    } else {
      return false;
    }
  }

  private static boolean checkInteger(BigInteger n, Expression expr) {
    if (expr instanceof IntegerExpression) {
      return !n.equals(((IntegerExpression) expr).getBigInteger());
    }
    if (!(expr instanceof ConCallExpression)) {
      return false;
    }

    ConCallExpression conCall = (ConCallExpression) expr;
    if (conCall.getDefinition() == Prelude.ZERO) {
      return !n.equals(BigInteger.ZERO);
    }
    if (conCall.getDefinition() == Prelude.SUC) {
      return n.equals(BigInteger.ZERO) || checkInteger(n.subtract(BigInteger.ONE), conCall.getDefCallArguments().get(0).normalize(NormalizationMode.WHNF));
    }
    return false;
  }

  private static boolean compareConstructors(Expression expr1, Expression expr2) {
    expr1 = expr1.normalize(NormalizationMode.WHNF);
    expr2 = expr2.normalize(NormalizationMode.WHNF);
    if (expr1 instanceof IntegerExpression) {
      return checkInteger(((IntegerExpression) expr1).getBigInteger(), expr2);
    }
    if (expr2 instanceof IntegerExpression) {
      return checkInteger(((IntegerExpression) expr2).getBigInteger(), expr1);
    }
    if (!(expr1 instanceof ConCallExpression) || !(expr2 instanceof ConCallExpression)) {
      return false;
    }

    ConCallExpression conCall1 = (ConCallExpression) expr1;
    ConCallExpression conCall2 = (ConCallExpression) expr2;
    Constructor con1 = conCall1.getDefinition();
    Constructor con2 = conCall2.getDefinition();

    if (con1 == con2) {
      for (int i = 0; i < conCall1.getDefCallArguments().size(); i++) {
        if (compareConstructors(conCall1.getDefCallArguments().get(i), conCall2.getDefCallArguments().get(i))) {
          return true;
        }
      }
      return false;
    }

    if (con1.getBody() == null && con2.getBody() == null && !con1.getDataType().isHIT()) {
      return true;
    }

    GraphClosure<Constructor> closure = new GraphClosure<>();
    for (Constructor constructor : con1.getDataType().getConstructors()) {
      Body body = constructor.getBody();
      if (body == null) {
        continue;
      }

      if (body instanceof Expression) {
        if (!addConstructor((Expression) body, constructor, closure)) {
          return false;
        }
      } else if (body instanceof IntervalElim) {
        for (IntervalElim.CasePair pair : ((IntervalElim) body).getCases()) {
          if (!addConstructor(pair.proj1, constructor, closure) || !addConstructor(pair.proj2, constructor, closure)) {
            return false;
          }
        }
        body = ((IntervalElim) body).getOtherwise();
      }
      if (body instanceof ElimBody) {
        for (ElimClause<Pattern> clause : ((ElimBody) body).getClauses()) {
          if (!addConstructor(clause.getExpression(), constructor, closure)) {
            return false;
          }
        }
      } else if (body != null) {
        throw new IllegalStateException();
      }
    }

    return !closure.areEquivalent(con1, con2);
  }

  public List<ConCallExpression> getMatchedConstructors() {
    if (getDefinition() == Prelude.PATH && getDefCallArguments().get(0).removeConstLam() != null && compareConstructors(getDefCallArguments().get(1), getDefCallArguments().get(2))) {
      return Collections.emptyList();
    }

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
