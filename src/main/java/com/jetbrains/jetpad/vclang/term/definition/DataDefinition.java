package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class DataDefinition extends Definition implements Abstract.DataDefinition {
  private final List<Constructor> myConstructors;
  private final List<TypeArgument> myParameters;

  public DataDefinition(String name, ClassDefinition parent, Precedence precedence, Fixity fixity, Universe universe, List<TypeArgument> parameters, List<Constructor> constructors) {
    super(name, parent, precedence, fixity, universe);
    myParameters = parameters;
    myConstructors = constructors;
  }

  @Override
  public List<TypeArgument> getParameters() {
    return myParameters;
  }

  @Override
  public TypeArgument getParameter(int index) {
    return myParameters.get(index);
  }

  @Override
  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  @Override
  public Constructor getConstructor(int index) {
    return myConstructors.get(index);
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
