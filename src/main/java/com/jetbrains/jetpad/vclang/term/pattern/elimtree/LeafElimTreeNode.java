package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
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

    if (replacement == EmptyElimTreeNode.getInstance()) {
      for (ConstructorClause clause = getParent(); clause != null; clause = clause.getParent().getParent()) {
        clause.getParent().getConstructorClauses().remove(clause);
        if (!clause.getParent().getConstructorClauses().isEmpty()) {
          return root;
        }
      }
      return EmptyElimTreeNode.getInstance();
    }
    getParent().setChild(replacement);
    return root;
  }

  @Override
  public <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params) {
    return visitor.visitLeaf(this, params);
  }

  @Override
  public ElimTreeNode matchUntilStuck(Substitution subst) {
    return this;
  }
}
