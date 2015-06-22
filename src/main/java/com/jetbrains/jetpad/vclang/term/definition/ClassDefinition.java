package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.Map;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private final Map<String, Definition> myFields;

  public ClassDefinition(String name, Definition parent, Map<String, Definition> fields) {
    super(name, parent, DEFAULT_PRECEDENCE, Fixity.PREFIX);
    myFields = fields;
    hasErrors(false);
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public Map<String, Definition> getFields() {
    return myFields;
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  public void add(Definition definition) {
    myFields.put(definition.getName(), definition);
  }

  @Override
  public Definition findChild(String name) {
    return myFields.get(name);
  }
}
