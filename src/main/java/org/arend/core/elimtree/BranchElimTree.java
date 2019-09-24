package org.arend.core.elimtree;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.*;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;

import java.util.*;

public class BranchElimTree extends ElimTree {
  private final Map<Constructor, ElimChoice> myChildren;

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

  public BranchElimTree(int skipped, Map<Constructor, ElimChoice> children) {
    super(skipped);
    myChildren = children;
  }

  public ElimChoice getChild(Constructor constructor) {
    return myChildren.get(constructor);
  }

  public ElimChoice getTupleChild() {
    if (myChildren.size() == 1) {
      Map.Entry<Constructor, ElimChoice> entry = myChildren.entrySet().iterator().next();
      return entry.getKey() instanceof TupleConstructor ? entry.getValue() : null;
    } else {
      return null;
    }
  }

  public TupleConstructor getTupleConstructor() {
    if (myChildren.size() == 1) {
      Map.Entry<Constructor, ElimChoice> entry = myChildren.entrySet().iterator().next();
      return entry.getKey() instanceof TupleConstructor ? (TupleConstructor) entry.getKey() : null;
    } else {
      return null;
    }
  }

  private boolean isTupleTree() {
    return myChildren.size() == 1 && myChildren.keySet().iterator().next() instanceof TupleConstructor;
  }

  public Collection<Map.Entry<Constructor, ElimChoice>> getChildren() {
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
      if (myChildren.containsKey(conCall.getDefinition())) {
        newArguments = new ArrayList<>(conCall.getDefCallArguments().size() + arguments.size() - index - 1);
        newArguments.addAll(conCall.getDefCallArguments());
        newArguments.addAll(arguments.subList(index + 1, arguments.size()));
      } else {
        return new ArrayList<>(arguments.subList(index, arguments.size()));
      }
    } else if (argument.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr = argument.cast(IntegerExpression.class);
      boolean isZero = intExpr.isZero();
      if (myChildren.containsKey(isZero ? Prelude.ZERO : Prelude.SUC)) {
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
    Expression argument = arguments.get(skipped);
    Decision decision = argument.isWHNF();
    if (decision == Decision.NO) {
      return Decision.NO;
    }

    List<Expression> newArguments = getNewArguments(arguments, argument, skipped);
    if (newArguments == null) {
      return Decision.MAYBE;
    }

    if (isTupleTree()) {
      ElimChoice elimChoice = getTupleChild();
      if (elimChoice != null) {
        return elimChoice.elimTree.isWHNF(newArguments).min(decision);
      }
    } else if (argument.isInstance(ConCallExpression.class)) {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      ElimChoice elimChoice = myChildren.get(conCall.getDefinition());
      if (elimChoice != null) {
        return elimChoice.elimTree.isWHNF(newArguments).min(decision);
      } else {
        elimChoice = myChildren.get(null);
        return elimChoice == null ? decision : elimChoice.elimTree.isWHNF(newArguments).min(decision);
      }
    } else if (argument.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr = argument.cast(IntegerExpression.class);
      ElimChoice elimChoice = myChildren.get(intExpr.isZero() ? Prelude.ZERO : Prelude.SUC);
      if (elimChoice != null) {
        return elimChoice.elimTree.isWHNF(newArguments).min(decision);
      } else {
        elimChoice = myChildren.get(null);
        return elimChoice == null ? decision : elimChoice.elimTree.isWHNF(newArguments).min(decision);
      }
    }

    return decision;
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    Expression argument = arguments.get(skipped);

    List<Expression> newArguments = getNewArguments(arguments, argument, skipped);
    if (newArguments == null) {
      return argument.getStuckExpression();
    }

    if (isTupleTree()) {
      ElimChoice elimChoice = getTupleChild();
      if (elimChoice != null) {
        return elimChoice.elimTree.getStuckExpression(newArguments, expression);
      }
    } else if (argument.isInstance(ConCallExpression.class)) {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      ElimChoice elimChoice = myChildren.get(conCall.getDefinition());
      if (elimChoice != null) {
        return elimChoice.elimTree.getStuckExpression(newArguments, expression);
      } else {
        elimChoice = myChildren.get(null);
        return elimChoice != null ? elimChoice.elimTree.getStuckExpression(newArguments, expression) : expression;
      }
    } else if (argument.isInstance(IntegerExpression.class)) {
      IntegerExpression intExpr = argument.cast(IntegerExpression.class);
      ElimChoice elimChoice = myChildren.get(intExpr.isZero() ? Prelude.ZERO : Prelude.SUC);
      if (elimChoice != null) {
        return elimChoice.elimTree.getStuckExpression(newArguments, expression);
      } else {
        elimChoice = myChildren.get(null);
        return elimChoice != null ? elimChoice.elimTree.getStuckExpression(newArguments, expression) : expression;
      }
    }

    return argument.getStuckExpression();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BranchElimTree that = (BranchElimTree) o;
    return skipped == that.skipped && myChildren.equals(that.myChildren);
  }

  @Override
  public int hashCode() {
    return Objects.hash(skipped, myChildren);
  }
}
