package org.arend.naming.renamer;

import org.arend.ext.variable.Variable;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.NamedUnresolvedReference;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.arend.term.concrete.ConcreteExpressionFactory.cVar;
import static org.arend.term.concrete.ConcreteExpressionFactory.ref;

public class ReferableRenamer extends Renamer {
  private final Map<Variable, LocalReferable> myMap = new HashMap<>();

  public void addNewName(Variable variable, LocalReferable referable) {
    myMap.put(variable, referable);
  }

  @Override
  public String getNewName(Variable variable) {
    Referable ref = myMap.get(variable);
    return ref == null ? variable.getName() : ref.textRepresentation();
  }

  @Override
  public @NotNull String generateFreshName(@NotNull Variable var, @NotNull Collection<? extends Variable> variables) {
    String newName = super.generateFreshName(var, variables);
    addNewName(var, ref(newName));
    return newName;
  }

  public LocalReferable getNewReferable(Variable variable) {
    return myMap.get(variable);
  }

  public Concrete.Expression getConcreteExpression(Variable variable) {
    return makeReference(myMap.get(variable));
  }

  private static Concrete.ReferenceExpression makeReference(Referable referable) {
    return cVar(referable == null ? new NamedUnresolvedReference(null, "\\this") : referable);
  }

  public LocalReferable generateFreshReferable(Variable var, Collection<? extends Variable> variables) {
    String newName = super.generateFreshName(var, variables);
    LocalReferable referable = ref(newName);
    addNewName(var, referable);
    return referable;
  }
}
