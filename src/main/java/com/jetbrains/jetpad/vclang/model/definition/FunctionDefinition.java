package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.Node;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;

import static com.jetbrains.jetpad.vclang.model.expr.Model.Expression;

public class FunctionDefinition extends TypedDefinition {
  private final ChildProperty<FunctionDefinition, Expression> myTerm = new ChildProperty<>(this);

  public Expression getTerm() {
    return myTerm.get();
  }

  public Property<Expression> term() {
    return myTerm;
  }

  @Override
  public Node[] children() {
    Node[] nodes = new Node[arguments().size() + 2];
    arguments().toArray(nodes);
    nodes[arguments().size()] = getResultType();
    nodes[arguments().size() + 1] = getTerm();
    return nodes;
  }
}
