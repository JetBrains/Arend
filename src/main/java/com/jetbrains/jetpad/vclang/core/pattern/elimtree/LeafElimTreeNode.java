package com.jetbrains.jetpad.vclang.core.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;

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
  public String toString() {
    return accept(new ToAbstractVisitor(new ConcreteExpressionFactory()), null).toString();
  }

  @Override
  public <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params) {
    return visitor.visitLeaf(this, params);
  }

  @Override
  public ElimTreeNode matchUntilStuck(ExprSubstitution subst, boolean normalize) {
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

  public ExprSubstitution matchedToSubst(List<Expression> expressions) {
    ExprSubstitution result = new ExprSubstitution();
    for (int i = 0; i < myMatched.size(); i++) {
      result.add(myMatched.get(i), expressions.get(i));
    }
    return result;
  }

  @Override
  public LeafElimTreeNode match(List<Expression> expressions) {
    return this;
  }
}
