package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class DataDefinition extends Definition implements Abstract.DataDefinition {
  private List<Constructor> myConstructors;
  private List<TypeArgument> myParameters;

  public DataDefinition(Utils.Name name, Definition parent, Precedence precedence) {
    super(name, parent, precedence);
    myConstructors = new ArrayList<>();
  }

  public DataDefinition(Utils.Name name, Definition parent, Precedence precedence, Universe universe, List<TypeArgument> parameters) {
    super(name, parent, precedence);
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

  @Override
  public Definition getField(String name) {
    return getStaticField(name);
  }

  public void addConstructor(Constructor constructor) {
    myConstructors.add(constructor);
    addStaticField(constructor, Collections.EMPTY_LIST);
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
