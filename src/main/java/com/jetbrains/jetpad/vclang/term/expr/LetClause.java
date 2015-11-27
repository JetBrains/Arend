package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.LiftIndexVisitor;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.getFunctionType;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintLetClause;

public class LetClause extends Binding implements Abstract.LetClause, Function {
  private final List<Argument> myArguments;
  private final Expression myResultType;
  private final Abstract.Definition.Arrow myArrow;
  private final Expression myTerm;

  public LetClause(Name name, List<Argument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
    super(name);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public LetClause(String name, List<Argument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
    super(name);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintLetClause(this, builder, names, 0);
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return myArrow;
  }

  @Override
  public Expression getTerm() {
    return myTerm;
  }

  @Override
  public List<Argument> getArguments() {
    return myArguments;
  }

  @Override
  public Expression getResultType() {
    return myResultType;
  }

  @Override
  public ClassDefinition getThisClass() {
    return null;
  }

  @Override
  public Expression getType() {
    return getFunctionType(this);
  }

  @Override
  public LetClause lift(int on) {
    return on == 0 ? this : new LiftIndexVisitor(on).visitLetClause(this, 0);
  }
}
