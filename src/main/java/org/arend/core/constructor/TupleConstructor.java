package org.arend.core.constructor;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ProjExpression;
import org.arend.core.expr.TupleExpression;
import org.arend.ext.core.elimtree.CoreTupleBranchKey;

import java.util.ArrayList;
import java.util.List;

public final class TupleConstructor extends SingleConstructor implements CoreTupleBranchKey {
  private final int myLength;

  public TupleConstructor(int length) {
    myLength = length;
  }

  @Override
  public int getNumberOfParameters() {
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
