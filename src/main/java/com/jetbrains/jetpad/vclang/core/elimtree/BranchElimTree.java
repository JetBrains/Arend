package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BranchElimTree extends ElimTree {
  private final Sort mySortArgument;
  private final List<Expression> myDataArguments;
  private final Map<Pattern, ElimTree> myChildren;

  public BranchElimTree(Sort sortArgument, List<Expression> dataArguments, DependentLink parameters, Map<Pattern, ElimTree> children) {
    super(parameters);
    mySortArgument = sortArgument;
    myDataArguments = dataArguments;
    myChildren = children;
  }

  public ElimTree getChild(Pattern pattern) {
    return myChildren.get(pattern);
  }

  public Collection<Map.Entry<Pattern, ElimTree>> getChildren() {
    return myChildren.entrySet();
  }

  public Sort getSortArgument() {
    return mySortArgument;
  }

  public List<Expression> getDataArguments() {
    return myDataArguments;
  }

  public interface Pattern {
    Pattern ANY = new Pattern() {
      @Override
      public String toString() {
        return "ANY";
      }
    };
  }
}
