package com.jetbrains.jetpad.vclang.model.definition;

import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.otmodel.node.Node;
import jetbrains.jetpad.otmodel.node.NodeChildId;
import jetbrains.jetpad.otmodel.node.NodeConceptId;
import jetbrains.jetpad.otmodel.node.NodePropertyId;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

import static com.jetbrains.jetpad.vclang.model.expr.Model.Argument;
import static com.jetbrains.jetpad.vclang.model.expr.Model.Expression;

public abstract class TypedDefinition extends Definition {
  private final Property<String> myName = getStringProperty(new NodePropertyId("FEoQxRj278t.DGLy3q9ECno", "name"));

  private final ObservableList<Argument> myArguments = getChildren(new NodeChildId("Fj2v8JlRPvV.H97-N-_NuHi", "arguments", false));
  private final Property<Expression> myResultType = getChild(new NodeChildId("HW0-jRa4POp.HBsGTaCrNba", "resultType", true));

  protected TypedDefinition(WrapperContext ctx, NodeConceptId conceptId) {
    super(ctx, conceptId);
  }
  protected TypedDefinition(WrapperContext ctx, Node node) {
    super(ctx, node);
  }

  public String getName() {
    return myName.get();
  }

  public ObservableList<Argument> arguments() {
    return myArguments;
  }

  public Expression getResultType() {
    return myResultType.get();
  }

  public Property<String> name() {
    return myName;
  }

  public Property<Expression> resultType() {
    return myResultType;
  }
}
