package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BranchElimTree extends ElimTree {
  private final Sort mySortArgument;
  private final List<Expression> myDataArguments;
  private final Map<Constructor, ElimTree> myChildren;

  public BranchElimTree(DependentLink parameters, Sort sortArgument, List<Expression> dataArguments, Map<Constructor, ElimTree> children) {
    super(parameters);
    mySortArgument = sortArgument;
    myDataArguments = dataArguments;
    myChildren = children;
  }

  public ElimTree getChild(Constructor constructor) {
    return myChildren.get(constructor);
  }

  public Collection<Map.Entry<Constructor, ElimTree>> getChildren() {
    return myChildren.entrySet();
  }

  public Sort getSortArgument() {
    return mySortArgument;
  }

  public List<Expression> getDataArguments() {
    return myDataArguments;
  }
}
