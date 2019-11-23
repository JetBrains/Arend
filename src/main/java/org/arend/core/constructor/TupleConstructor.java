package org.arend.core.constructor;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ProjExpression;
import org.arend.core.expr.TupleExpression;

import java.util.ArrayList;
import java.util.List;

public final class TupleConstructor extends SingleConstructor {
  private final int myLength;

  public TupleConstructor(int length) {
    myLength = length;
  }

  @Override
  public int getLength() {
    return myLength;
  }

  @Override
  public List<Expression> getMatchedArguments(Expression argument, boolean normalizing) {
    List<Expression> args;
    TupleExpression tuple = argument.cast(TupleExpression.class);
    if (tuple != null) {
      args = tuple.getFields();
    } else {
      args = new ArrayList<>(myLength);
      for (int i = 0; i < myLength; i++) {
        args.add(ProjExpression.make(argument, i));
      }
    }
    return args;
  }
}
