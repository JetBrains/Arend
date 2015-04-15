package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.Node;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.otmodel.node.NodeChildId;
import jetbrains.jetpad.otmodel.node.NodeConceptId;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

import static com.jetbrains.jetpad.vclang.model.expr.Model.Expression;

public class FunctionDefinition extends TypedDefinition {
  private final Property<Expression> myTerm = getChild(new NodeChildId("CUItBV6Iy9p.BZkUGX5RvWg", "term", true));

  public FunctionDefinition(WrapperContext ctx) {
    super(ctx, new NodeConceptId("G0fpMO4ehvx.GFHlheip3SL", "FunctionDefinition"));
  }
  protected FunctionDefinition(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
    super(ctx, node);
  }

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
