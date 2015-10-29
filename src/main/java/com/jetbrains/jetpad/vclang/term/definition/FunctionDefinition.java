package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.statement.DefineStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FunctionDefinition extends Definition implements Abstract.FunctionDefinition, Function {
  private Arrow myArrow;
  // TODO: myArguments should have type List<TypeArguments>
  private List<Argument> myArguments;
  private Expression myResultType;
  private Expression myTerm;
  private boolean myTypeHasErrors;

  public FunctionDefinition(Namespace parentNamespace, Name name, Precedence precedence, Arrow arrow) {
    super(parentNamespace, name, precedence);
    myArrow = arrow;
    myTypeHasErrors = true;
  }

  public FunctionDefinition(Namespace parentNamespace, Name name, Precedence precedence, List<Argument> arguments, Expression resultType, Arrow arrow, Expression term) {
    super(parentNamespace, name, precedence);
    setUniverse(new Universe.Type(0, Universe.Type.PROP));
    hasErrors(false);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTypeHasErrors = false;
    myTerm = term;
  }

  public Namespace getStaticNamespace() {
    return getParentNamespace().getChild(getName());
  }

  @Override
  public Arrow getArrow() {
    return myArrow;
  }

  public void setArrow(Arrow arrow) {
    myArrow = arrow;
  }

  @Override
  public boolean isAbstract() {
    return myArrow == null;
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
  public Expression getTerm() {
    return myTerm;
  }

  public void setTerm(Expression term) {
    myTerm = term;
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
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunction(this, params);
  }
}
