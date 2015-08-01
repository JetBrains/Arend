package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionDefinition extends Definition implements Abstract.FunctionDefinition, Function {
  private Arrow myArrow;
  private List<Argument> myArguments;
  private Expression myResultType;
  private Expression myTerm;
  private boolean myTypeHasErrors;
  private Map<String, Definition> myNestedDefinitions;

  public FunctionDefinition(Utils.Name name, Definition parent, Precedence precedence, Arrow arrow) {
    super(name, parent, precedence);
    myArrow = arrow;
    myTypeHasErrors = true;
    myNestedDefinitions = null;
  }

  public FunctionDefinition(Utils.Name name, Definition parent, Precedence precedence, List<Argument> arguments, Expression resultType, Arrow arrow, Expression term) {
    super(name, parent, precedence);
    setUniverse(new Universe.Type(0, Universe.Type.PROP));
    hasErrors(false);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTypeHasErrors = false;
    myTerm = term;
    myNestedDefinitions = null;
  }

  public boolean addNestedDefinition(Definition definition, List<ModuleError> errors) {
    Definition oldDefinition = getNestedDefinition(definition.getName().name);
    if (oldDefinition != null && !(oldDefinition instanceof ClassDefinition && definition instanceof ClassDefinition && (!((ClassDefinition) oldDefinition).hasAbstracts() || !((ClassDefinition) definition).hasAbstracts()))) {
      errors.add(new ModuleError(getEnclosingModule(), "Name " + getFullNestedName(definition.getName().name) + " is already defined"));
      return false;
    }

    if (myNestedDefinitions == null) {
      myNestedDefinitions = new HashMap<>();
    }
    myNestedDefinitions.put(definition.getName().name, definition);
    return true;
  }

  public void removeNestedDefinition(String name) {
    if (myNestedDefinitions != null)
      myNestedDefinitions.remove(name);
  }

  public Definition getNestedDefinition(String name) {
    if (myNestedDefinitions == null)
      return null;
    return myNestedDefinitions.get(name);
  }

  private Module getEnclosingModule() {
    for (Definition def = this;; def = def.getParent()) {
      if (def instanceof ClassDefinition && !((ClassDefinition) def).isLocal())
        return new Module((ClassDefinition)def.getParent(), def.getName().name);
    }
  }

  private String getFullNestedName(String name) {
    String result = name;
    for (Definition def = this; !(def instanceof ClassDefinition) || ((ClassDefinition) def).isLocal(); def = def.getParent()) {
      result = def.getName().name + "." + result;
    }
    return result;
  }

  public ClassDefinition getClass(String name, List<ModuleError> errors) {
    if (myNestedDefinitions != null) {
      Definition definition = getNestedDefinition(name);
      if (definition != null) {
        if (definition instanceof ClassDefinition) {
          return (ClassDefinition) definition;
        } else {
          errors.add(new ModuleError(getEnclosingModule(), "Name " + getFullNestedName(name) + " is already defined"));
          return null;
        }
      }
    }

    ClassDefinition result = new ClassDefinition(name, this, true);
    result.hasErrors(true);
    if (myNestedDefinitions == null) {
      myNestedDefinitions = new HashMap<>();
    }
    myNestedDefinitions.put(result.getName().name, result);
    return result;
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
  public Collection<Definition> getNestedDefinitions() {
    return myNestedDefinitions == null ? null : myNestedDefinitions.values();
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
