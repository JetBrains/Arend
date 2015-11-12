package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.statement.DefineStatement;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ClassCall;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private Map<String, ClassField> myFields = null;

  public ClassDefinition(Namespace parentNamespace, Name name) {
    super(parentNamespace, name, DEFAULT_PRECEDENCE);
    super.hasErrors(false);
  }

  @Override
  public Expression getBaseType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public ClassCallExpression getDefCall() {
    return ClassCall(this, new HashMap<ClassField, ClassCallExpression.ImplementStatement>());
  }

  public ClassField getField(String name) {
    return myFields == null ? null : myFields.get(name);
  }

  public Collection<ClassField> getFields() {
    return myFields == null ? Collections.<ClassField>emptyList() : myFields.values();
  }

  public void addField(ClassField field) {
    if (myFields == null) {
      myFields = new HashMap<>();
    }
    myFields.put(field.getName().name, field);
    field.setThisClass(this);
  }

  public ClassField removeField(String name) {
    return myFields != null ? myFields.remove(name) : null;
  }

  public void removeField(ClassField field) {
    if (myFields != null) {
      myFields.remove(field.getName().name);
    }
  }

  public ClassField getParentField() {
    return getField("\\parent");
  }

  public void addParentField(ClassDefinition parentClass) {
    setThisClass(parentClass);
    addField(new ClassField(getParentNamespace().getChild(getName()), new Name("\\parent", Fixity.PREFIX), DEFAULT_PRECEDENCE, ClassCall(parentClass), this));
  }

  @Override
  public Collection<? extends Abstract.Statement> getStatements() {
    Namespace namespace = getParentNamespace().findChild(getName().name);
    int size = namespace == null ? 0 : namespace.getMembers().size();
    Collection<? extends ClassField> fields = getFields();

    List<Abstract.Statement> statements = new ArrayList<>(fields.size() + size);
    for (ClassField field : fields) {
      statements.add(new DefineStatement(new FunctionDefinition(namespace, field.getName(), field.getPrecedence(), Collections.<Argument>emptyList(), field.getType(), null, null), false));
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
