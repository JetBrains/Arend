package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.List;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private final List<Definition> myFields;

  public ClassDefinition(String name, ClassDefinition parent, Universe universe, List<Definition> fields) {
    super(name, parent, DEFAULT_PRECEDENCE, Fixity.PREFIX, universe);
    myFields = fields;
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public List<Definition> getFields() {
    return myFields;
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  public Definition findField(String name) {
    for (Definition field : myFields) {
      if (name.equals(field.getName())) {
        return field;
      }
    }

    return null;
  }
}
