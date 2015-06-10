package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class FunctionDefinition extends Definition implements Abstract.FunctionDefinition {
  private final Abstract.Definition.Arrow myArrow;
  private final List<TelescopeArgument> myArguments;
  private Expression myResultType;
  private Expression myTerm;

  public FunctionDefinition(String name, ClassDefinition module, Precedence precedence, Fixity fixity, List<TelescopeArgument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
    super(name, module, precedence, fixity, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return myArrow;
  }

  @Override
  public Expression getTerm() {
    return myTerm;
  }

  public void setTerm(Expression term) {
    myTerm = term;
  }

  @Override
  public List<TelescopeArgument> getArguments() {
    return myArguments;
  }

  @Override
  public TelescopeArgument getArgument(int index) {
    return myArguments.get(index);
  }

  @Override
  public Expression getResultType() {
    return myResultType;
  }

  public void setResultType(Expression resultType) {
    myResultType = resultType;
  }

  @Override
  public Expression getType() {
    return myArguments.isEmpty() ? myResultType : Pi(new ArrayList<TypeArgument>(myArguments), myResultType);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunction(this, params);
  }
}
