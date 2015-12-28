package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimExpression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.statement.DefineStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.FunCall;

public class FunctionDefinition extends Definition implements Abstract.FunctionDefinition, Function {
  // TODO: myArguments should have type List<TypeArguments>
  private List<Argument> myArguments;
  private Expression myResultType;
  private ElimTreeNode myElimTree;
  private boolean myTypeHasErrors;

  public FunctionDefinition(Namespace parentNamespace, Name name, Precedence precedence) {
    super(parentNamespace, name, precedence);
    myTypeHasErrors = true;
  }

  public FunctionDefinition(Namespace parentNamespace, Name name, Precedence precedence, List<Argument> arguments, Expression resultType, ElimTreeNode elimTree) {
    super(parentNamespace, name, precedence);
    setUniverse(new Universe.Type(0, Universe.Type.PROP));
    hasErrors(false);
    myArguments = arguments;
    myResultType = resultType;
    myTypeHasErrors = false;
    myElimTree = elimTree;
  }

  public Namespace getStaticNamespace() {
    return getParentNamespace().getChild(getName());
  }

  @Override
  public Arrow getArrow() {
    return ElimExpression.toArrow(myElimTree);
  }

  @Override
  public ElimTreeNode getElimTree() {
    return myElimTree;
  }

  @Override
  public boolean isAbstract() {
    return myElimTree == null;
  }

  @Override
  public boolean isOverridden() {
    return false;
  }

  @Override
  public Name getOriginalName() {
    return null;
  }

  @Override
  public Collection<? extends Abstract.Statement> getStatements() {
    Namespace staticNamespace = getStaticNamespace();
    List<Abstract.Statement> statements = new ArrayList<>(staticNamespace.getMembers().size());
    for (NamespaceMember pair : staticNamespace.getMembers()) {
      Abstract.Definition definition = pair.definition != null ? pair.definition : pair.abstractDefinition;
      if (definition != null) {
        statements.add(new DefineStatement(definition, true));
      }
    }
    return statements;
  }

  @Override
  public Abstract.Expression getTerm() {
    return ElimExpression.toElimExpression(myElimTree);
  }

  public void setElimTree(ElimTreeNode elimTree) {
    myElimTree = elimTree;
  }

  @Override
  public List<Argument> getArguments() {
    return myArguments;
  }

  public void setArguments(List<Argument> arguments) {
    myArguments = arguments;
  }

  @Override
  public Expression getResultType() {
    return myResultType;
  }

  public void setResultType(Expression resultType) {
    myResultType = resultType;
  }

  public boolean typeHasErrors() {
    return myTypeHasErrors;
  }

  public void typeHasErrors(boolean has) {
    myTypeHasErrors = has;
  }

  @Override
  public Expression getBaseType() {
    if (myTypeHasErrors) {
      return null;
    }
    return Utils.getFunctionType(this);
  }

  @Override
  public FunCallExpression getDefCall() {
    return FunCall(this);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunction(this, params);
  }
}
