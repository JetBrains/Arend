package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class LeafElimTreeNode extends ElimTreeNode {
  private List<Binding> myMatched;
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

    getParent().setChild(replacement);
    return root;
  }

  @Override
  public <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params) {
    return visitor.visitLeaf(this, params);
  }

  @Override
  public ElimTreeNode matchUntilStuck(Substitution subst, boolean normalize) {
    return this;
  }

  @Override
  public void updateLeavesMatched(List<Binding> context) {
    setMatched(context);
  }

  public void setMatched(List<Binding> context) {
    myMatched = new ArrayList<>(context);
  }

  public List<Binding> getMatched() {
    return myMatched;
  }

  public Substitution matchedToSubst(List<Expression> expressions) {
    Substitution result = new Substitution();
    for (int i = 0; i < myMatched.size(); i++) {
      result.add(myMatched.get(i), expressions.get(i));
    }
    return result;
  }

  public LeafElimTreeNode match(List<Expression> expressions) {
    return this;
  }
}
