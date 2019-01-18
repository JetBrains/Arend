package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.*;
import org.arend.prelude.Prelude;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BranchElimTree extends ElimTree {
  private final Map<Constructor, ElimTree> myChildren;

  public final static class TupleConstructor extends Constructor {
    private final int myLength;

    public TupleConstructor(int length) {
      super(null, null);
      myLength = length;
    }

    public int getLength() {
      return myLength;
    }
  }

  public BranchElimTree(DependentLink parameters, Map<Constructor, ElimTree> children) {
    super(parameters);
    myChildren = children;
  }

  public ElimTree getChild(Constructor constructor) {
    return myChildren.get(constructor);
  }

  public ElimTree getTupleChild() {
    if (myChildren.size() == 1) {
      Map.Entry<Constructor, ElimTree> entry = myChildren.entrySet().iterator().next();
      return entry.getKey() instanceof TupleConstructor ? entry.getValue() : null;
    } else {
      return null;
    }
  }

  public boolean isTupleTree() {
    return myChildren.size() == 1 && myChildren.keySet().iterator().next() instanceof TupleConstructor;
  }

  public Collection<Map.Entry<Constructor, ElimTree>> getChildren() {
    return myChildren.entrySet();
  }

  private List<Expression> getNewArguments(List<? extends Expression> arguments, Expression argument, int index) {
    List<Expression> newArguments = null;
    if (isTupleTree()) {
      if (argument.isInstance(TupleExpression.class) || argument.isInstance(NewExpression.class)) {
        newArguments = new ArrayList<>();
        if (argument.isInstance(TupleExpression.class)) {
          newArguments.addAll(argument.cast(TupleExpression.class).getFields());
        } else {
          NewExpression newExpr = argument.cast(NewExpression.class);
          ClassCallExpression classCall = newExpr.getExpression();
          for (ClassField field : classCall.getDefinition().getFields()) {
            newArguments.add(classCall.getImplementation(field, newExpr));
          }
        }
        newArguments.addAll(arguments.subList(index + 1, arguments.size()));
      }
    } else if (argument.isInstance(ConCallExpression.class)) {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      ElimTree elimTree = myChildren.get(conCall.getDefinition());
      if (elimTree != null) {
        newArguments = new ArrayList<>(conCall.getDefCallArguments().size() + arguments.size() - index - 1);
        newArguments.addAll(conCall.getDefCallArguments());
        newArguments.addAll(arguments.subList(index + 1, arguments.size()));
      } else {
        return new ArrayList<>(arguments.subList(index, arguments.size()));
      }
    } else if (argument.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr = argument.cast(IntegerExpression.class);
      boolean isZero = intExpr.isZero();
      ElimTree elimTree = myChildren.get(isZero ? Prelude.ZERO : Prelude.SUC);
      if (elimTree != null) {
        newArguments = new ArrayList<>();
        if (!isZero) {
          newArguments.add(intExpr.pred());
        }
        newArguments.addAll(arguments.subList(index + 1, arguments.size()));
      } else {
        return new ArrayList<>(arguments.subList(index, arguments.size()));
      }
    }
    return newArguments;
  }

  @Override
  public boolean isWHNF(List<? extends Expression> arguments) {
    int index = DependentLink.Helper.size(getParameters());
    Expression argument = arguments.get(index);
    if (!argument.isWHNF()) {
      return false;
    }

    List<Expression> newArguments = getNewArguments(arguments, argument, index);
    if (newArguments == null) {
      return true;
    }

    if (isTupleTree()) {
      if (argument.isInstance(TupleExpression.class) || argument.isInstance(NewExpression.class)) {
        ElimTree elimTree = getTupleChild();
        if (elimTree != null) {
          return elimTree.isWHNF(newArguments);
        }
      }
    } else if (argument.isInstance(ConCallExpression.class)) {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      ElimTree elimTree = myChildren.get(conCall.getDefinition());
      if (elimTree != null) {
        return elimTree.isWHNF(newArguments);
      } else {
        elimTree = myChildren.get(null);
        return elimTree == null || elimTree.isWHNF(newArguments);
      }
    } else if (argument.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr = argument.cast(IntegerExpression.class);
      ElimTree elimTree = myChildren.get(intExpr.isZero() ? Prelude.ZERO : Prelude.SUC);
      if (elimTree != null) {
        return elimTree.isWHNF(newArguments);
      } else {
        elimTree = myChildren.get(null);
        return elimTree == null || elimTree.isWHNF(newArguments);
      }
    }

    return true;
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    int index = DependentLink.Helper.size(getParameters());
    Expression argument = arguments.get(index);

    List<Expression> newArguments = getNewArguments(arguments, argument, index);
    if (newArguments == null) {
      return argument.getStuckExpression();
    }

    if (isTupleTree()) {
      if (argument.isInstance(TupleExpression.class) || argument.isInstance(NewExpression.class)) {
        ElimTree elimTree = getTupleChild();
        if (elimTree != null) {
          return elimTree.getStuckExpression(newArguments, expression);
        }
      }
    } else if (argument.isInstance(ConCallExpression.class)) {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      ElimTree elimTree = myChildren.get(conCall.getDefinition());
      if (elimTree != null) {
        return elimTree.getStuckExpression(newArguments, expression);
      } else {
        elimTree = myChildren.get(null);
        return elimTree != null ? elimTree.getStuckExpression(newArguments, expression) : expression;
      }
    } else if (argument.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr = argument.cast(IntegerExpression.class);
      ElimTree elimTree = myChildren.get(intExpr.isZero() ? Prelude.ZERO : Prelude.SUC);
      if (elimTree != null) {
        return elimTree.getStuckExpression(newArguments, expression);
      } else {
        elimTree = myChildren.get(null);
        return elimTree != null ? elimTree.getStuckExpression(newArguments, expression) : expression;
      }
    }

    return argument.getStuckExpression();
  }
}
