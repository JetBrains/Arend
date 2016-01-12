package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.param.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.param.TypedBinding;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static com.jetbrains.jetpad.vclang.term.expr.param.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.expr.param.Utils.trimToSize;

class ConCallContextExpander implements AutoCloseable {
  private final int myOldContextSize;
  private final List<Binding> myContext;
  private final List<Binding> myTail;

  private final int myIndex;
  private final Expression mySubst;
  private final int myNumConstructorArguments;

  ConCallContextExpander(int index, ConCallExpression conCall, List<Binding> context) {
    myIndex = index;
    myContext = context;
    myTail = new ArrayList<>(context.subList(context.size() - 1 - index, context.size()));
    context.subList(context.size() - 1 - index, context.size()).clear();
    conCall = (ConCallExpression) conCall.liftIndex(0, -index);


    List<TypeArgument> conArguments = new ArrayList<>();
    splitArguments(conCall.getType(myContext), conArguments, myContext);
    myNumConstructorArguments = conArguments.size();
    myOldContextSize = myContext.size();
    Expression subst = conCall;
    for (TypeArgument arg : conArguments) {
      myContext.add(new TypedBinding((String) null, arg.getType()));
      subst = Apps(subst.liftIndex(0, 1), Index(0));
    }
    mySubst = subst;

    for (int i = 1; i < myTail.size(); i++) {
      myContext.add(new TypedBinding(myTail.get(i).getName(),
          myTail.get(i).getType().liftIndex(i, conArguments.size()).subst(subst.liftIndex(0, i - 1), i - 1)));
    }
  }

  public final Expression substIn(Expression expression) {
    return expression.liftIndex(myIndex + 1, myNumConstructorArguments).subst(mySubst.liftIndex(0, myIndex), myIndex);
  }

  public final List<Expression> substIn(List<Expression> expressions) {
    List<Expression> result = new ArrayList<>();
    for (Expression expr : expressions) {
      result.add(substIn(expr));
    }
    return result;
  }

  @Override
  public void close() {
    trimToSize(myContext, myOldContextSize);
    myContext.addAll(myTail);
  }
}
