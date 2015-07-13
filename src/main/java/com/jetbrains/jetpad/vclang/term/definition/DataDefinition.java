package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class DataDefinition extends Definition implements Abstract.DataDefinition {
  private List<Constructor> myConstructors;
  private List<TypeArgument> myParameters;

  public DataDefinition(String name, Definition parent, Precedence precedence, Fixity fixity, List<Constructor> constructors) {
    super(name, parent, precedence, fixity);
    myConstructors = constructors;
  }

  public DataDefinition(String name, Definition parent, Precedence precedence, Fixity fixity, Universe universe, List<TypeArgument> parameters, List<Constructor> constructors) {
    super(name, parent, precedence, fixity);
    setUniverse(universe);
    hasErrors(false);
    myConstructors = constructors;
    myParameters = parameters;
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

  public void setConstructors(List<Constructor> constructors) {
    myConstructors = constructors;
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

  @Override
  public Constructor getStaticField(String name) {
    for (Constructor constructor : myConstructors) {
      if (constructor.getName().equals(name)) {
        return constructor;
      }
    }

    return null;
  }

  @Override
  public List<Constructor> getStaticFields() {
    return myConstructors;
  }
}
