package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.List;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private final List<Definition> myFields;

  public ClassDefinition(String name, ClassDefinition module, Universe universe, List<Definition> fields) {
    super(name, module, DEFAULT_PRECEDENCE, Fixity.PREFIX, universe);
    myFields = fields;
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  @Override
  public List<Definition> getFields() {
    return myFields;
  }

  @Override
  public Definition getField(int index) {
    return myFields.get(index);
  }

  public int findField(String name) {
    for (int i = 0; i < myFields.size(); ++i) {
      if (name.equals(myFields.get(i).getName())) {
        return i;
      }
    }
    return -1;
  }
}
