package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BranchElimTree extends ElimTree {
  private final Map<Constructor, ElimTree> myChildren;

  public BranchElimTree(DependentLink parameters, Map<Constructor, ElimTree> children) {
    super(parameters);
    myChildren = children;
  }

  public ElimTree getChild(Constructor constructor) {
    return myChildren.get(constructor);
  }

  public Collection<Map.Entry<Constructor, ElimTree>> getChildren() {
    return myChildren.entrySet();
  }

  @Override
  public boolean isWHNF(List<? extends Expression> arguments) {
    int index = DependentLink.Helper.size(getParameters());
    if (arguments.get(index).isInstance(ConCallExpression.class)) {
      ConCallExpression conCall = arguments.get(index).cast(ConCallExpression.class);
      ElimTree elimTree = myChildren.get(conCall.getDefinition());
      if (elimTree != null) {
        List<Expression> newArguments = new ArrayList<>(conCall.getDefCallArguments().size() + arguments.size() - index - 1);
        newArguments.addAll(conCall.getDefCallArguments());
        newArguments.addAll(arguments.subList(index + 1, arguments.size()));
        return elimTree.isWHNF(newArguments);
      } else {
        elimTree = myChildren.get(null);
        //noinspection SimplifiableConditionalExpression
        return elimTree != null ? elimTree.isWHNF(arguments.subList(index, arguments.size())) : true;
      }
    }
    return arguments.get(index).isWHNF();
  }

  @Override
  public Expression getStuckExpression(List<? extends Expression> arguments, Expression expression) {
    int index = DependentLink.Helper.size(getParameters());
    if (arguments.get(index).isInstance(ConCallExpression.class)) {
      ConCallExpression conCall = arguments.get(index).cast(ConCallExpression.class);
      ElimTree elimTree = myChildren.get(conCall.getDefinition());
      if (elimTree != null) {
        List<Expression> newArguments = new ArrayList<>(conCall.getDefCallArguments().size() + arguments.size() - index - 1);
        newArguments.addAll(conCall.getDefCallArguments());
        newArguments.addAll(arguments.subList(index + 1, arguments.size()));
        return elimTree.getStuckExpression(newArguments, expression);
      } else {
        elimTree = myChildren.get(null);
        return elimTree != null ? elimTree.getStuckExpression(arguments.subList(index, arguments.size()), expression) : expression;
      }
    }
    return arguments.get(index).getStuckExpression();
  }
}
