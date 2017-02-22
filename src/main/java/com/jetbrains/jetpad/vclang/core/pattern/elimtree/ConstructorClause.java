package com.jetbrains.jetpad.vclang.core.pattern.elimtree;

import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

import java.util.ArrayList;
import java.util.List;

public class ConstructorClause implements Clause {
  private final Constructor myConstructor;
  private final DependentLink myParameters;
  private ElimTreeNode myChild;
  private final BranchElimTreeNode myParent;
  private final List<TypedBinding> myTailBindings;

  public ConstructorClause(Constructor constructor, DependentLink parameters, List<TypedBinding> tailBindings, BranchElimTreeNode parent) {
    assert constructor.status().headerIsOK();
    myConstructor = constructor;
    myParameters = parameters;
    myTailBindings = tailBindings;
    myParent = parent;
    myChild = EmptyElimTreeNode.getInstance();
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

  public List<TypedBinding> getTailBindings() {
    return myTailBindings;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  @Override
  public ExprSubstitution getSubst() {
    ExprSubstitution result = new ExprSubstitution();

    List<Expression> arguments = new ArrayList<>();
    for (DependentLink link = myParameters; link.hasNext(); link = link.getNext()) {
      arguments.add(ExpressionFactory.Reference(link));
    }
    DataCallExpression dataCall = myParent.getReference().getType().normalize(NormalizeVisitor.Mode.WHNF).toExpression().toDataCall();
    result.add(myParent.getReference(), new ConCallExpression(myConstructor, dataCall.getLevelArguments(), myConstructor.matchDataTypeArguments(new ArrayList<>(dataCall.getDefCallArguments())), arguments));

    for (int i = 0; i < myParent.getContextTail().size(); i++) {
      result.add(myParent.getContextTail().get(i), ExpressionFactory.Reference(myTailBindings.get(i)));
    }

    return result;
  }
}
