package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class ConstructorClause implements Clause {
  private final Constructor myConstructor;
  private final DependentLink myParameters;
  private ElimTreeNode myChild;
  private final BranchElimTreeNode myParent;
  private final List<Binding> myTailBindings;

  ConstructorClause(Constructor constructor, DependentLink parameters, List<Binding> tailBindings, BranchElimTreeNode parent) {
    myConstructor = constructor;
    myParameters = parameters;
    myTailBindings = tailBindings;
    myParent = parent;
    setChild(EmptyElimTreeNode.getInstance());
  }

  public BranchElimTreeNode getParent() {
    return myParent;
  }

  public void setChild(ElimTreeNode child) {
    myChild = child;
    child.setParent(this);
  }

  public ElimTreeNode getChild() {
    return myChild;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  public List<Binding> getTailBindings() {
    return myTailBindings;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public Substitution getSubst() {
    Substitution result = new Substitution();

    List<Expression> arguments = new ArrayList<>();
    myParent.getReference().getType().getFunction(arguments);
    Collections.reverse(arguments);

    Expression substExpr = new ConCallExpression(myConstructor, myConstructor.matchDataTypeArguments(arguments));
    for (DependentLink link = myParameters; link.hasNext(); link = link.getNext()) {
      substExpr = Apps(substExpr, Reference(link));
    }
    result.add(myParent.getReference(), substExpr);

    for (int i = 0; i < myParent.getContextTail().size(); i++) {
      result.add(myParent.getContextTail().get(i), Reference(myTailBindings.get(i)));
    }

    return result;
  }
}
