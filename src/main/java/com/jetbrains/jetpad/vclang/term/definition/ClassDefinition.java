package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.Collection;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private Namespace myLocalNamespace = new Namespace(null, null);

  public ClassDefinition(Namespace namespace) {
    super(namespace, DEFAULT_PRECEDENCE);
    super.hasErrors(false);
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  public Definition getField(String name) {
    return myLocalNamespace.getMember(name);
  }

  public Namespace getLocalNamespace() {
    return myLocalNamespace;
  }

  public void setLocalNamespace(Namespace localNamespace) {
    myLocalNamespace = localNamespace;
  }

  @Override
  public Collection<Definition> getFields() {
    return myLocalNamespace.getMembers();
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }
}
