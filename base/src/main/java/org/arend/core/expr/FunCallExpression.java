package org.arend.core.expr;

import org.arend.core.definition.Constructor;
import org.arend.core.definition.DConstructor;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.expr.CoreFunCallExpression;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.arend.util.SingletonList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FunCallExpression extends DefCallExpression implements CoreFunCallExpression {
  private final List<Expression> myArguments;

  private FunCallExpression(FunctionDefinition definition, LevelPair levels, List<Expression> arguments) {
    super(definition, levels);
    myArguments = arguments;
  }

  // a fake funCall that can be used only in ConstructorExpressionPattern
  public FunCallExpression(DConstructor function, LevelPair levels, Expression elementsType) {
    super(function, levels);
    myArguments = elementsType == null ? Collections.emptyList() : new SingletonList<>(elementsType);
  }

  public static Expression make(FunctionDefinition definition, LevelPair levels, List<Expression> arguments) {
    if ((definition == Prelude.PLUS || definition == Prelude.MUL || definition == Prelude.MINUS || definition == Prelude.DIV || definition == Prelude.MOD || definition == Prelude.DIV_MOD) && arguments.size() == 2 && arguments.get(0) instanceof IntegerExpression && arguments.get(1) instanceof IntegerExpression) {
      IntegerExpression expr1 = (IntegerExpression) arguments.get(0);
      IntegerExpression expr2 = (IntegerExpression) arguments.get(1);
      return definition == Prelude.PLUS ? expr1.plus(expr2)
        : definition == Prelude.MUL ? expr1.mul(expr2)
        : definition == Prelude.MINUS ? expr1.minus(expr2)
        : definition == Prelude.DIV ? expr1.div(expr2)
        : definition == Prelude.MOD ? expr1.mod(expr2)
        : expr1.divMod(expr2);
    }
    if (definition == Prelude.AT && arguments.size() == 5) {
      if (arguments.get(4) instanceof ConCallExpression) {
        Constructor constructor = ((ConCallExpression) arguments.get(4)).getDefinition();
        if (constructor == Prelude.LEFT) {
          return arguments.get(1);
        }
        if (constructor == Prelude.RIGHT) {
          return arguments.get(2);
        }
      } else if (arguments.get(3) instanceof ConCallExpression && ((ConCallExpression) arguments.get(3)).getDefinition() == Prelude.PATH_CON) {
        return AppExpression.make(((ConCallExpression) arguments.get(3)).getDefCallArguments().get(0), arguments.get(4), true);
      }
    }
    if (definition == Prelude.EMPTY_ARRAY && arguments.size() == 1) {
      return ArrayExpression.make(levels, arguments.get(0), Collections.emptyList(), null);
    }
    if (definition == Prelude.ARRAY_CONS && arguments.size() == 3) {
      return ArrayExpression.make(levels, arguments.get(0), new SingletonList<>(arguments.get(1)), arguments.get(2));
    }
    return new FunCallExpression(definition, levels, arguments);
  }

  public static FunCallExpression makeFunCall(FunctionDefinition definition, LevelPair levels, List<Expression> arguments) {
    Expression result = make(definition, levels, arguments);
    if (!(result instanceof FunCallExpression)) {
      throw new IllegalArgumentException();
    }
    return (FunCallExpression) result;
  }

  @NotNull
  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public List<? extends Expression> getConCallArguments() {
    return getDefinition() instanceof DConstructor ? myArguments.subList(((DConstructor) getDefinition()).getNumberOfParameters(), myArguments.size()) : myArguments;
  }

  @NotNull
  @Override
  public FunctionDefinition getDefinition() {
    return (FunctionDefinition) super.getDefinition();
  }

  @Override
  public @Nullable Expression evaluate() {
    return NormalizeVisitor.INSTANCE.eval(this);
  }

  @Override
  public boolean canBeConstructor() {
    return !(getDefinition().isSFunc() || getDefinition().getBody() == null && getDefinition().status().isOK());
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunCall(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitFunCall(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunCall(this, params);
  }

  @Override
  public Decision isWHNF() {
    FunctionDefinition definition = getDefinition();
    if (definition == Prelude.COERCE || definition == Prelude.COERCE2) {
      return Objects.requireNonNull(definition.getBody()).isWHNF(myArguments).min(Decision.MAYBE);
    } else {
      return definition.getBody() != null ? definition.getBody().isWHNF(myArguments) : Decision.YES;
    }
  }

  @Override
  public Expression getStuckExpression() {
    FunctionDefinition definition = getDefinition();
    if (definition == Prelude.COERCE) {
      Expression stuck = myArguments.get(2).getStuckExpression();
      return stuck != null ? stuck : myArguments.get(0).getStuckExpression();
    }
    if (definition == Prelude.COERCE2) {
      Expression stuck = myArguments.get(1).getStuckExpression();
      if (stuck != null) {
        return stuck;
      }
      stuck = myArguments.get(3).getStuckExpression();
      return stuck != null ? stuck : myArguments.get(0).getStuckExpression();
    }
    if (definition == Prelude.ARRAY_INDEX) {
      Expression stuck = myArguments.get(1).getStuckExpression();
      if (stuck instanceof InferenceReferenceExpression) return stuck;
      Expression stuck2 = myArguments.get(0).getStuckExpression();
      return stuck2 instanceof InferenceReferenceExpression ? stuck2 : stuck;
    }
    return definition.getBody() != null ? definition.getBody().getStuckExpression(myArguments, this) : null;
  }
}
