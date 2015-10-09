package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.statement.DefineStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private Namespace myLocalNamespace;

  public ClassDefinition(Namespace parentNamespace, Name name) {
    super(parentNamespace, name, DEFAULT_PRECEDENCE);
    super.hasErrors(false);
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  public Definition getField(String name) {
    return myLocalNamespace.getDefinition(name);
  }

  public Namespace getLocalNamespace() {
    return myLocalNamespace;
  }

  public void setLocalNamespace(Namespace localNamespace) {
    myLocalNamespace = localNamespace;
  }

  @Override
  public Collection<? extends Abstract.Statement> getStatements() {
    Namespace namespace = getParentNamespace().findChild(getName().name);
    int size = namespace == null ? 0 : namespace.getMembers().size();

    List<Abstract.Statement> statements = new ArrayList<>(myLocalNamespace.getMembers().size() + size);
    for (NamespaceMember pair : myLocalNamespace.getMembers()) {
      Abstract.Definition definition = pair.definition != null ? pair.definition : pair.abstractDefinition;
      if (definition != null) {
        statements.add(new DefineStatement(definition, false));
      }
    }
    if (namespace != null) {
      for (NamespaceMember pair : namespace.getMembers()) {
        Abstract.Definition definition = pair.definition != null ? pair.definition : pair.abstractDefinition;
        if (definition != null) {
          statements.add(new DefineStatement(definition, true));
        }
      }
    }
    return statements;
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }
}
