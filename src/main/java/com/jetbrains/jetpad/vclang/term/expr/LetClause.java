package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.LiftIndexVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.getFunctionType;

public class LetClause extends Binding implements Function {
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
  public ElimTreeNode getElimTree() {
    return myElimTree;
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
