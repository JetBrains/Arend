package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
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
  private final Namespace myStaticNamespace;
  private Arrow myArrow;
  private List<Argument> myArguments;
  private Expression myResultType;
  private Expression myTerm;
  private boolean myTypeHasErrors;

  public FunctionDefinition(Namespace staticNamespace, Namespace dynamicNamespace, Precedence precedence, Arrow arrow) {
    super(dynamicNamespace == null ? staticNamespace : dynamicNamespace, precedence);
    myStaticNamespace = staticNamespace;
    myArrow = arrow;
    myTypeHasErrors = true;
  }

  public FunctionDefinition(Namespace staticNamespace, Namespace dynamicNamespace, Precedence precedence, List<Argument> arguments, Expression resultType, Arrow arrow, Expression term) {
    super(dynamicNamespace == null ? staticNamespace : dynamicNamespace, precedence);
    setUniverse(new Universe.Type(0, Universe.Type.PROP));
    hasErrors(false);
    myStaticNamespace = staticNamespace;
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTypeHasErrors = false;
    myTerm = term;
  }

  public Namespace getStaticNamespace() {
    return myStaticNamespace;
  }

  public Namespace getDynamicNamespace() {
    return myStaticNamespace == getNamespace() ? null : getNamespace();
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
  public Utils.Name getOriginalName() {
    return null;
  }

  @Override
  public Collection<? extends Abstract.Statement> getStatements() {
    Namespace dynamicNamespace = myStaticNamespace == getNamespace() ? null : getNamespace();
    List<Abstract.Statement> statements = new ArrayList<>(myStaticNamespace.getMembers().size() + (dynamicNamespace == null ? 0 : dynamicNamespace.getMembers().size()));
    if (dynamicNamespace != null) {
      for (DefinitionPair pair : dynamicNamespace.getMembers()) {
        Abstract.Definition definition = pair.definition != null ? pair.definition : pair.abstractDefinition;
        if (definition != null) {
          statements.add(new DefineStatement(definition, true));
        }
      }
    }
    for (DefinitionPair pair : myStaticNamespace.getMembers()) {
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
  public Expression getType() {
    if (typeHasErrors())
      return null;
    return Utils.getFunctionType(this);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunction(this, params);
  }
}
