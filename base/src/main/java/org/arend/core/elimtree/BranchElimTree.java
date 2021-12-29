package org.arend.core.elimtree;

import org.arend.core.constructor.*;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.*;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.Suc;

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

  public boolean isArray() {
    return !myChildren.isEmpty() && myChildren.keySet().iterator().next() instanceof ArrayConstructor;
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

  public Collection<? extends BranchKey> getKeys() {
    return myChildren.keySet();
  }

  public boolean withArrayElementsType() {
    BranchKey key = myChildren.keySet().iterator().next();
    if (!(key instanceof ArrayConstructor)) {
      throw new IllegalStateException();
    }
    return ((ArrayConstructor) key).withElementsType();
  }

  public boolean withArrayLength() {
    BranchKey key = myChildren.keySet().iterator().next();
    if (!(key instanceof ArrayConstructor)) {
      throw new IllegalStateException();
    }
    return ((ArrayConstructor) key).withLength();
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
      argument = argument.getUnderlyingExpression();
      if (argument instanceof ConCallExpression) {
        ConCallExpression conCall = (ConCallExpression) argument;
        ElimTree elimTree = myChildren.get(conCall.getDefinition());
        if (elimTree != null) {
          newArguments = new ArrayList<>(conCall.getDefCallArguments().size() + arguments.size() - index - 1);
          newArguments.addAll(conCall.getDefCallArguments());
          newArguments.addAll(arguments.subList(index + 1, arguments.size()));
        } else {
          return new ArrayList<>(arguments.subList(index, arguments.size()));
        }
      } else if (argument instanceof IntegerExpression) {
        IntegerExpression intExpr = (IntegerExpression) argument;
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
      } else if (argument instanceof ArrayExpression) {
        ArrayExpression array = (ArrayExpression) argument;
        ElimTree elimTree = myChildren.get(new ArrayConstructor(array.getElements().isEmpty(), true, true));
        if (elimTree != null) {
          newArguments = new ArrayList<>();
          newArguments.add(array.getElementsType());
          if (!array.getElements().isEmpty()) {
            newArguments.add(array.getElements().get(0));
            newArguments.add(array.drop(1));
          }
          newArguments.addAll(arguments.subList(index + 1, arguments.size()));
        } else {
          return new ArrayList<>(arguments.subList(index, arguments.size()));
        }
      }
    }
    return newArguments;
  }

  private static BranchKey getBranchKey(Expression argument) {
    argument = argument.getUnderlyingExpression();
    if (argument instanceof ConCallExpression) {
      return ((ConCallExpression) argument).getDefinition();
    } else if (argument instanceof IntegerExpression) {
      return ((IntegerExpression) argument).isZero() ? Prelude.ZERO : Prelude.SUC;
    } else if (argument instanceof ArrayExpression) {
      return new ArrayConstructor(((ArrayExpression) argument).getElements().isEmpty(), true, true);
    } else {
      return null;
    }
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
      BranchKey key = getBranchKey(argument);
      if (key != null) {
        ElimTree elimTree = myChildren.get(key);
        if (elimTree != null) {
          return elimTree.isWHNF(newArguments).min(decision);
        } else {
          elimTree = myChildren.get(null);
          return elimTree == null ? decision : elimTree.isWHNF(newArguments).min(decision);
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
      BranchKey key = getBranchKey(argument);
      if (key != null) {
        ElimTree elimTree = myChildren.get(key);
        if (elimTree != null) {
          return elimTree.getStuckExpression(newArguments, expression);
        } else {
          elimTree = myChildren.get(null);
          return elimTree != null ? elimTree.getStuckExpression(newArguments, expression) : expression;
        }
      }
    }

    return argument.getStuckExpression();
  }

  @Override
  public List<Expression> normalizeArguments(List<? extends Expression> arguments) {
    List<Expression> result = new ArrayList<>(arguments.size());
    int index = getSkip();
    result.addAll(arguments.subList(0, index));
    Expression argument = arguments.get(index).normalize(NormalizationMode.WHNF);

    SingleConstructor singleConstructor = getSingleConstructorKey();
    if (singleConstructor instanceof IdpConstructor) {
      if (singleConstructor.getMatchedArguments(argument, true) != null) {
        result.add(argument);
        result.addAll(getSingleConstructorChild().normalizeArguments(arguments.subList(index + 1, arguments.size())));
        return result;
      }
    } else if (singleConstructor instanceof TupleConstructor) {
      if (argument instanceof TupleExpression) {
        TupleExpression tuple = (TupleExpression) argument;
        List<Expression> args = new ArrayList<>();
        args.addAll(tuple.getFields());
        args.addAll(arguments.subList(index + 1, arguments.size()));
        List<Expression> newArgs = getSingleConstructorChild().normalizeArguments(args);
        result.add(new TupleExpression(newArgs.subList(0, tuple.getFields().size()), tuple.getSigmaType()));
        result.addAll(newArgs.subList(tuple.getFields().size(), newArgs.size()));
        return result;
      }
    } else if (singleConstructor instanceof ClassConstructor) {
      if (argument instanceof NewExpression) {
        ClassConstructor classCon = (ClassConstructor) singleConstructor;
        ClassCallExpression classCall = ((NewExpression) argument).getType();
        List<Expression> args = new ArrayList<>();
        for (ClassField field : classCon.getClassDefinition().getFields()) {
          if (!classCon.getClassDefinition().isImplemented(field) && !classCon.getImplementedFields().contains(field)) {
            if (field.isProperty()) {
              args.add(FieldCallExpression.make(field, classCall.getLevels(field.getParentClass()), argument));
            } else {
              args.add(classCall.getAbsImplementationHere(field));
            }
          }
        }
        args.addAll(arguments.subList(index + 1, arguments.size()));
        List<Expression> newArgs = getSingleConstructorChild().normalizeArguments(args);
        Map<ClassField, Expression> implementations = new LinkedHashMap<>();
        int i = 0;
        for (ClassField field : classCon.getClassDefinition().getFields()) {
          if (!classCon.getClassDefinition().isImplemented(field)) {
            if (classCon.getImplementedFields().contains(field)) {
              implementations.put(field, classCall.getAbsImplementationHere(field));
            } else {
              implementations.put(field, newArgs.get(i++));
            }
          }
        }
        result.add(new NewExpression(null, new ClassCallExpression(classCall.getDefinition(), classCall.getLevels(), implementations, classCall.getSort(), classCall.getUniverseKind())));
        result.addAll(newArgs.subList(i, newArgs.size()));
        return result;
      }
    } else if (argument instanceof ConCallExpression) {
      ConCallExpression conCall = (ConCallExpression) argument;
      ElimTree elimTree = myChildren.get(conCall.getDefinition());
      if (elimTree != null) {
        List<Expression> args = new ArrayList<>();
        args.addAll(conCall.getDefCallArguments());
        args.addAll(arguments.subList(index + 1, arguments.size()));
        List<Expression> newArgs = elimTree.normalizeArguments(args);
        result.add(ConCallExpression.make(conCall.getDefinition(), conCall.getLevels(), conCall.getDataTypeArguments(), newArgs.subList(0, conCall.getDefCallArguments().size())));
        result.addAll(newArgs.subList(conCall.getDefCallArguments().size(), newArgs.size()));
        return result;
      }
    } else if (argument instanceof IntegerExpression) {
      IntegerExpression intExpr = (IntegerExpression) argument;
      boolean isZero = intExpr.isZero();
      ElimTree elimTree = myChildren.get(isZero ? Prelude.ZERO : Prelude.SUC);
      if (elimTree != null) {
        List<Expression> args = new ArrayList<>();
        if (!isZero) args.add(intExpr.pred());
        args.addAll(arguments.subList(index + 1, arguments.size()));
        List<Expression> newArgs = elimTree.normalizeArguments(args);
        result.add(isZero ? intExpr : Suc(newArgs.get(0)));
        result.addAll(isZero ? newArgs : newArgs.subList(1, newArgs.size()));
        return result;
      }
    } else if (argument instanceof ArrayExpression) {
      ArrayExpression array = (ArrayExpression) argument;
      ElimTree elimTree = myChildren.get(new ArrayConstructor(array.getElements().isEmpty(), true, true));
      if (elimTree != null) {
        List<Expression> args = new ArrayList<>();
        boolean withElementsType = withArrayElementsType();
        boolean withLength = withArrayLength();
        if (!withLength) {
          args.add(array.getLength());
        }
        if (!withElementsType) {
          args.add(array.getElementsType());
        }
        if (!array.getElements().isEmpty()) {
          args.add(array.getElements().get(0));
          args.add(array.drop(1));
        }
        args.addAll(arguments.subList(index + 1, arguments.size()));
        List<Expression> newArgs = elimTree.normalizeArguments(args);
        result.add(array.getElements().isEmpty() ? array : FunCallExpression.make(Prelude.ARRAY_CONS, array.getLevels(), newArgs.subList(0, 2 + (withLength ? 0 : 1) + (withElementsType ? 0 : 1))));
        result.addAll(newArgs.subList((array.getElements().isEmpty() ? 0 : 2) + (withElementsType ? 0 : 1) + (withLength ? 0 : 1), newArgs.size()));
        return result;
      }
    }

    result.add(argument);
    result.addAll(arguments.subList(index + 1, arguments.size()));
    return result;
  }
}
