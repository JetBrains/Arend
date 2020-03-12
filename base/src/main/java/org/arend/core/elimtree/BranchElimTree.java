package org.arend.core.elimtree;

import org.arend.core.constructor.SingleConstructor;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.IntegerExpression;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;

import java.util.*;

public class BranchElimTree extends ElimTree {
  private final Map<BranchKey, ElimTree> myChildren = new HashMap<>();
  private final boolean myKeepConCall;

  public BranchElimTree(int skip, boolean keepConCall) {
    super(skip);
    myKeepConCall = keepConCall;
  }

  public boolean keepConCall() {
    return myKeepConCall;
  }

  public ElimTree getSingleConstructorChild() {
    if (myChildren.size() == 1) {
      Map.Entry<BranchKey, ElimTree> entry = myChildren.entrySet().iterator().next();
      return entry.getKey() instanceof SingleConstructor ? entry.getValue() : null;
    } else {
      return null;
    }
  }

  public SingleConstructor getSingleConstructorKey() {
    if (myChildren.size() == 1) {
      Map.Entry<BranchKey, ElimTree> entry = myChildren.entrySet().iterator().next();
      return entry.getKey() instanceof SingleConstructor ? (SingleConstructor) entry.getKey() : null;
    } else {
      return null;
    }
  }

  public Collection<Map.Entry<BranchKey, ElimTree>> getChildren() {
    return myChildren.entrySet();
  }

  public ElimTree getChild(BranchKey key) {
    return myChildren.get(key);
  }

  public void addChild(BranchKey key, ElimTree elimTree) {
    myChildren.put(key, elimTree);
  }

  private boolean isSingleConstructorTree() {
    return myChildren.size() == 1 && myChildren.keySet().iterator().next() instanceof SingleConstructor;
  }

  private List<Expression> getNewArguments(List<? extends Expression> arguments, Expression argument, int index) {
    List<Expression> newArguments = null;
    SingleConstructor singleConstructor = getSingleConstructorKey();
    if (singleConstructor != null) {
      newArguments = singleConstructor.getMatchedArguments(argument, false);
      if (newArguments == null) {
        return null;
      }
      if (index + 1 < arguments.size()) {
        newArguments = new ArrayList<>(newArguments);
        newArguments.addAll(arguments.subList(index + 1, arguments.size()));
      }
    } else {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      if (conCall != null) {
        ElimTree elimTree = myChildren.get(conCall.getDefinition());
        if (elimTree != null) {
          newArguments = new ArrayList<>(conCall.getDefCallArguments().size() + arguments.size() - index - 1);
          newArguments.addAll(conCall.getDefCallArguments());
          newArguments.addAll(arguments.subList(index + 1, arguments.size()));
        } else {
          return new ArrayList<>(arguments.subList(index, arguments.size()));
        }
      } else {
        IntegerExpression intExpr = argument.cast(IntegerExpression.class);
        if (intExpr != null) {
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
      }
    }
    return newArguments;
  }

  @Override
  public Decision isWHNF(List<? extends Expression> arguments) {
    int index = getSkip();
    Expression argument = arguments.get(index);
    Decision decision = argument.isWHNF();
    if (decision == Decision.NO) {
      return Decision.NO;
    }

    List<Expression> newArguments = getNewArguments(arguments, argument, index);
    if (newArguments == null) {
      return Decision.MAYBE;
    }

    if (isSingleConstructorTree()) {
      ElimTree elimTree = getSingleConstructorChild();
      if (elimTree != null) {
        return elimTree.isWHNF(newArguments).min(decision);
      }
    } else {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      if (conCall != null) {
        ElimTree elimTree = myChildren.get(conCall.getDefinition());
        if (elimTree != null) {
          return elimTree.isWHNF(newArguments).min(decision);
        } else {
          elimTree = myChildren.get(null);
          return elimTree == null ? decision : elimTree.isWHNF(newArguments).min(decision);
        }
      } else {
        IntegerExpression intExpr = argument.cast(IntegerExpression.class);
        if (intExpr != null) {
          ElimTree elimTree = myChildren.get(intExpr.isZero() ? Prelude.ZERO : Prelude.SUC);
          if (elimTree != null) {
            return elimTree.isWHNF(newArguments).min(decision);
          } else {
            elimTree = myChildren.get(null);
            return elimTree == null ? decision : elimTree.isWHNF(newArguments).min(decision);
          }
        }
      }
    }

    return decision;
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    int index = getSkip();
    Expression argument = arguments.get(index);

    List<Expression> newArguments = getNewArguments(arguments, argument, index);
    if (newArguments == null) {
      return argument.getStuckExpression();
    }

    if (isSingleConstructorTree()) {
      ElimTree elimTree = getSingleConstructorChild();
      if (elimTree != null) {
        return elimTree.getStuckExpression(newArguments, expression);
      }
    } else {
      ConCallExpression conCall = argument.cast(ConCallExpression.class);
      if (conCall != null) {
        ElimTree elimTree = myChildren.get(conCall.getDefinition());
        if (elimTree != null) {
          return elimTree.getStuckExpression(newArguments, expression);
        } else {
          elimTree = myChildren.get(null);
          return elimTree != null ? elimTree.getStuckExpression(newArguments, expression) : expression;
        }
      } else {
        IntegerExpression intExpr = argument.cast(IntegerExpression.class);
        if (intExpr != null) {
          ElimTree elimTree = myChildren.get(intExpr.isZero() ? Prelude.ZERO : Prelude.SUC);
          if (elimTree != null) {
            return elimTree.getStuckExpression(newArguments, expression);
          } else {
            elimTree = myChildren.get(null);
            return elimTree != null ? elimTree.getStuckExpression(newArguments, expression) : expression;
          }
        }
      }
    }

    return argument.getStuckExpression();
  }
}
