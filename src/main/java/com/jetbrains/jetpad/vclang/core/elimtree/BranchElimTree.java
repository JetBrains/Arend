package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;

import java.util.Collection;
import java.util.Map;

public class BranchElimTree extends ElimTree {
  private Map<Pattern, ElimTree> myChildren;

  public BranchElimTree(DependentLink parameters, Map<Pattern, ElimTree> children) {
    super(parameters);
    myChildren = children;
  }

  public ElimTree getChild(Pattern pattern) {
    return myChildren.get(pattern);
  }

  public Collection<Map.Entry<Pattern, ElimTree>> getChildren() {
    return myChildren.entrySet();
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
