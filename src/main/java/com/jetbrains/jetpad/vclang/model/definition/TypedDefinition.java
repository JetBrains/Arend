package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.Position;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.property.DelegateProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public abstract class TypedDefinition extends Definition {
  private final Property<String> myName = new ValueProperty<>();
  private final ObservableList<Argument> myArguments = new ObservableArrayList<>();
  private final ChildProperty<TypedDefinition, Expression> myResultType = new ChildProperty<>(this);

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
    return new DelegateProperty<Expression>(myResultType){
      @Override
      public void set(Expression resultType) {
        TypedDefinition.this.setResultType(resultType);
      }
    };
  }

  public void setName(String name) {
    myName.set(name);
  }

  public void setResultType(Expression resultType) {
    myResultType.set(resultType);
    if (resultType != null) {
      resultType.position = Position.DEF_RESULT_TYPE;
    }
  }
}
