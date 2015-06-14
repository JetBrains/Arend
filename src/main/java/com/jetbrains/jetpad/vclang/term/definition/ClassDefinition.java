package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
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
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  @Override
  public List<Definition> getFields() {
    return myFields;
  }

  public Definition findField(String name, List<VcError> errors) {
    for (Definition field : myFields) {
      if (name.equals(field.getName())) {
        return field;
      }
    }

    return ModuleLoader.loadModule(new Module(this, name), errors);
  }
}
