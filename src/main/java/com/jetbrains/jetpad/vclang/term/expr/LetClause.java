package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.LiftIndexVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimExpression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.getFunctionType;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintLetClause;

public class LetClause extends Binding implements Abstract.LetClause, Function {
  private final List<TypeArgument> myArguments;
  private final ElimTreeNode myElimTree;
  private final Expression myResultType;

  public LetClause(Name name, List<TypeArgument> arguments, Expression resultType, ElimTreeNode elimTree) {
    super(name);
    myArguments = arguments;
    myResultType = resultType;
    myElimTree = elimTree;
  }

  public LetClause(String name, List<TypeArgument> arguments, Expression resultType, ElimTreeNode elimTree) {
    super(name);
    myArguments = arguments;
    myResultType = resultType;
    myElimTree = elimTree;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintLetClause(this, builder, names, 0);
  }

  @Override
  public ElimTreeNode getElimTree() {
    return myElimTree;
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return ElimExpression.toArrow(myElimTree);
  }

  @Override
  public Abstract.Expression getTerm() {
    return ElimExpression.toElimExpression(myElimTree);
  }

  @Override
  public List<TypeArgument> getArguments() {
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
