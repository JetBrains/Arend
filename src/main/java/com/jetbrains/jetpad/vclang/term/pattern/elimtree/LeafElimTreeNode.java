package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

public class LeafElimTreeNode extends ElimTreeNode {
  private Abstract.Definition.Arrow myArrow;
  private Expression myExpression;

  public LeafElimTreeNode(Abstract.Definition.Arrow arrow, Expression expression) {
    myArrow = arrow;
    myExpression = expression;
  }

  public LeafElimTreeNode() {
    this(null, null);
  }

  public Expression getExpression() {
    return myExpression;
  }

  public void setExpression(Expression expression) {
    myExpression = expression;
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return myArrow;
  }

  public void setArrow(Abstract.Definition.Arrow arrow) {
    myArrow = arrow;
  }

  public ElimTreeNode replaceWith(ElimTreeNode root, ElimTreeNode replacement) {
    if (getParent() == null) {
      return replacement;
    }

    getParent().setChild(replacement);
    return root;
  }

  @Override
  public <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params) {
    return visitor.visitLeaf(this, params);
  }
}
