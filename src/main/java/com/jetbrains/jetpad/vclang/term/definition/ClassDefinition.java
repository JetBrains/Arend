package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.ModuleError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.*;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private Namespace myNamespace;
  private boolean myIsLocal;

  public ClassDefinition(String name, Definition parent) {
    this(name, parent, false);
  }

  public ClassDefinition(String name, Definition parent, boolean isLocal) {
    super(new Utils.Name(name, Fixity.PREFIX), parent, DEFAULT_PRECEDENCE);
    myIsLocal = isLocal;
    hasErrors(false);
    myNamespace = new Namespace(this);
  }

  @Override
  public Namespace getNamespace() {
    return myNamespace;
  }

  public boolean isLocal() {
    return myIsLocal;
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public List<Definition> getPublicFields() {
    return myNamespace.getPublicMembers();
  }

  @Override
  public Definition getStaticField(String name) {
    return myNamespace.getStaticMember(name);
  }

  @Override
  public Collection<Definition> getStaticFields() {
    return myNamespace.getStaticMembers();
  }

  public Definition getPublicField(String name) {
    return myNamespace.getPublicMember(name);
  }

  public Definition getPrivateField(String name) {
    return myNamespace.getPrivateMember(name);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  public ClassDefinition getClass(String name, List<ModuleError> errors) {
    return myNamespace.getClass(name, errors);
  }

  public boolean hasAbstracts() {
    if (myNamespace.getPublicMembers() == null) return false;
    for (Definition field : myNamespace.getPublicMembers()) {
      if (field.isAbstract()) return true;
    }
    return false;
  }

  public boolean addPublicField(Definition definition, List<ModuleError> errors) {
    return myNamespace.addPublicMember(definition, errors);
  }

  public void addPrivateField(Definition definition) {
     myNamespace.addPrivateMember(definition);
  }

  public boolean addStaticField(Definition definition, List<ModuleError> errors) {
   return myNamespace.checkDepsAddStaticMember(definition, errors);
  }

  public boolean addField(Definition definition, List<ModuleError> errors) {
    return myNamespace.addMember(definition, errors);
  }

  public void removeField(Definition definition) {
    myNamespace.removeMember(definition);
  }
}
