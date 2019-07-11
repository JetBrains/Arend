package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.*;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BranchElimTree extends ElimTree {
  private final Map<Constructor, ElimTree> myChildren;

  public final static class TupleConstructor extends Constructor {
    private final int myLength;
    private final boolean myClassInstance;

    public TupleConstructor(int length, boolean isClassInstance) {
      super(null, null);
      myLength = length;
      myClassInstance = isClassInstance;
    }

    public int getLength() {
      return myLength;
    }

    public boolean isClassInstance() {
      return myClassInstance;
    }

    public List<Expression> getMatchedArguments(Expression argument) {
      List<Expression> args;
      if (myClassInstance) {
        if (argument.isInstance(NewExpression.class)) {
          args = argument.cast(NewExpression.class).getExpression().getImplementedHereList();
        } else {
          Expression type = argument.getType(false);
          ClassDefinition classDef = type != null && type.isInstance(ClassCallExpression.class) ? type.cast(ClassCallExpression.class).getDefinition() : null;
          if (classDef == null) {
            return null;
          }
          args = new ArrayList<>(myLength);
          for (ClassField field : classDef.getFields()) {
            if (!classDef.isImplemented(field)) {
              args.add(FieldCallExpression.make(field, type.cast(ClassCallExpression.class).getSortArgument(), argument));
            }
          }
        }
      } else {
        if (argument.isInstance(TupleExpression.class)) {
          args = argument.cast(TupleExpression.class).getFields();
        } else {
          args = new ArrayList<>(myLength);
          for (int i = 0; i < myLength; i++) {
            args.add(ProjExpression.make(argument, i));
          }
        }
      }
      return args;
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

  public TupleConstructor getTupleConstructor() {
    if (myChildren.size() == 1) {
      Map.Entry<Constructor, ElimTree> entry = myChildren.entrySet().iterator().next();
      return entry.getKey() instanceof TupleConstructor ? (TupleConstructor) entry.getKey() : null;
    } else {
      return null;
    }
  }

  private boolean isTupleTree() {
    return myChildren.size() == 1 && myChildren.keySet().iterator().next() instanceof TupleConstructor;
  }

  public Collection<Map.Entry<Constructor, ElimTree>> getChildren() {
    return myChildren.entrySet();
  }

  private List<Expression> getNewArguments(List<? extends Expression> arguments, Expression argument, int index) {
    List<Expression> newArguments = null;
    TupleConstructor tupleConstructor = getTupleConstructor();
    if (tupleConstructor != null) {
      newArguments = tupleConstructor.getMatchedArguments(argument);
      if (newArguments == null) {
        return null;
      }
      if (index + 1 < arguments.size()) {
        newArguments = new ArrayList<>(newArguments);
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
  public Decision isWHNF(List<? extends Expression> arguments) {
    int index = DependentLink.Helper.size(getParameters());
    Expression argument = arguments.get(index);
    Decision decision = argument.isWHNF();
    if (decision == Decision.NO) {
      return Decision.NO;
    }

    List<Expression> newArguments = getNewArguments(arguments, argument, index);
    if (newArguments == null) {
      return Decision.MAYBE;
    }

    if (isTupleTree()) {
      ElimTree elimTree = getTupleChild();
      if (elimTree != null) {
        return elimTree.isWHNF(newArguments).min(decision);
      }
    } else if (argument.isInstance(ConCallExpression.class)) {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      ElimTree elimTree = myChildren.get(conCall.getDefinition());
      if (elimTree != null) {
        return elimTree.isWHNF(newArguments).min(decision);
      } else {
        elimTree = myChildren.get(null);
        return elimTree == null ? decision : elimTree.isWHNF(newArguments).min(decision);
      }
    } else if (argument.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr = argument.cast(IntegerExpression.class);
      ElimTree elimTree = myChildren.get(intExpr.isZero() ? Prelude.ZERO : Prelude.SUC);
      if (elimTree != null) {
        return elimTree.isWHNF(newArguments).min(decision);
      } else {
        elimTree = myChildren.get(null);
        return elimTree == null ? decision : elimTree.isWHNF(newArguments).min(decision);
      }
    }

    return decision;
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
      ElimTree elimTree = getTupleChild();
      if (elimTree != null) {
        return elimTree.getStuckExpression(newArguments, expression);
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
