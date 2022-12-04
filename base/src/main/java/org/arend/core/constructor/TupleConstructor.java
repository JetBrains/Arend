package org.arend.core.constructor;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ProjExpression;
import org.arend.core.expr.TupleExpression;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TupleConstructor extends SingleConstructor {
  private final int myLength;
  private final Set<Integer> myPropertyIndices;

  public TupleConstructor(int length, Set<Integer> propertyIndices) {
    myLength = length;
    myPropertyIndices = propertyIndices;
  }

  public int getNumberOfParameters() {
    return myLength;
  }

  public Set<? extends Integer> getPropertyIndices() {
    return myPropertyIndices;
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
        args.add(ProjExpression.make(argument, i, myPropertyIndices.contains(i)));
      }
    }
    return args;
  }

  @Override
  public boolean compare(SingleConstructor other, Equations equations, Concrete.SourceNode sourceNode) {
    return other instanceof TupleConstructor && myLength == ((TupleConstructor) other).myLength;
  }
}
