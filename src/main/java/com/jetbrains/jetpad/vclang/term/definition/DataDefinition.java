package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class DataDefinition extends Definition implements Abstract.DataDefinition {
  private List<Constructor> myConstructors;
  private List<TypeArgument> myParameters;

  public DataDefinition(Namespace namespace, Precedence precedence) {
    super(namespace, precedence);
    myConstructors = new ArrayList<>();
  }

  public DataDefinition(Namespace namespace, Precedence precedence, Universe universe, List<TypeArgument> parameters) {
    super(namespace, precedence);
    setUniverse(universe);
    hasErrors(false);
    myParameters = parameters;
    myConstructors = new ArrayList<>();
  }

  @Override
  public List<TypeArgument> getParameters() {
    return myParameters;
  }

  public void setParameters(List<TypeArgument> arguments) {
    myParameters = arguments;
  }

  @Override
  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public Constructor getConstructor(String name) {
    for (Constructor constructor : myConstructors) {
      if (constructor.getName().name.equals(name)) {
        return constructor;
      }
    }
    return null;
  }

  public void addConstructor(Constructor constructor) {
    myConstructors.add(constructor);
    getNamespace().addDefinition(constructor);
  }

  @Override
  public Expression getType() {
    Expression resultType = new UniverseExpression(getUniverse());
    return myParameters.isEmpty() ? resultType : Pi(myParameters, resultType);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitData(this, params);
  }
}
