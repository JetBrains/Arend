package org.arend.typechecking.visitor;

import org.arend.core.definition.Definition;
import org.arend.ext.util.Pair;
import org.arend.naming.reference.ParameterReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.Concrete;

import java.util.*;

public class WhereVarsCollector extends VoidConcreteVisitor<Void> {
  private final Set<ParameterReferable> myWhereRefs = new HashSet<>();
  private final Set<Definition> myWhereDefs = new HashSet<>();
  private Concrete.Definition myDefinition;

  private WhereVarsCollector() {}

  public static Pair<Set<ParameterReferable>, Set<Definition>> findWhereVars(Collection<? extends Concrete.Definition> definitions) {
    WhereVarsCollector collector = new WhereVarsCollector();
    for (Concrete.Definition definition : definitions) {
      collector.myDefinition = definition;
      definition.accept(collector, null);
    }
    return new Pair<>(collector.myWhereRefs, collector.myWhereDefs);
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    if (ref instanceof ParameterReferable) {
      myWhereRefs.add((ParameterReferable) ref);
      expr.setReferent(((ParameterReferable) ref).getReferable());
    } else if (ref instanceof TCDefReferable && myDefinition != null) {
      Definition def = ((TCDefReferable) ref).getTypechecked();
      if (def != null && !def.getParametersOriginalDefinitions().isEmpty()) {
        for (Pair<TCDefReferable, Integer> pair : def.getParametersOriginalDefinitions()) {
          if ((myDefinition.getData() == pair.proj1 || myDefinition.getExternalParameters().containsKey(pair.proj1)) && myDefinition.getData() != pair.proj1) {
            myWhereDefs.add(def);
            break;
          }
        }
      }
    }
    return null;
  }
}