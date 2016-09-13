package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.CycleError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.visitor.DefinitionCheckTypeVisitor;

import java.util.*;

public class TypecheckingOrdering {
  public static abstract class Result {
  }

  public static class OKResult extends Result {
    public final LinkedHashMap<Abstract.Definition, Abstract.ClassDefinition> order;

    OKResult(LinkedHashMap<Abstract.Definition, Abstract.ClassDefinition> order) {
      this.order = order;
    }
  }

  public static class CycleResult extends Result {
    public final List<Abstract.Definition> cycle;

    CycleResult(List<Abstract.Definition> cycle) {
      this.cycle = cycle;
    }
  }

  private List<Abstract.Definition> myCycle;
  private final Queue<Abstract.Definition> myOthers;
  private final LinkedHashMap<Abstract.Definition, Abstract.ClassDefinition> myResult;
  private final Set<Abstract.Definition> myVisited;
  private final LinkedHashSet<Abstract.Definition> myVisiting;

  private final HashMap<Abstract.Definition, List<Abstract.Definition>> myClassToNonStatic;

  private TypecheckingOrdering(Queue<Abstract.Definition> queue) {
    myCycle = null;
    myOthers = queue;
    myResult = new LinkedHashMap<>();
    myVisited = new HashSet<>();
    myVisiting = new LinkedHashSet<>();
    myClassToNonStatic = new HashMap<>();
  }

  private boolean doOrder(final Abstract.Definition definition) {
    if (myCycle != null)
      return false;
    if (myVisited.contains(definition)) {
      return true;
    }

    // TODO
    //if (toNamespaceMember(definition).isTypeChecked()) {
    //  myVisited.add(definition);
    //  return true;
    //}

    if (myVisiting.contains(definition)) {
      myCycle = new ArrayList<>(myVisiting);
      myCycle = myCycle.subList(myCycle.lastIndexOf(definition), myCycle.size());
      return false;
    }

    myVisiting.add(definition);

    Abstract.ClassDefinition enclosingClass = getEnclosingClass(definition);
    if (enclosingClass != null && !doOrder(enclosingClass)) {
      return false;
    }

    for (final Abstract.Definition def : definition.accept(new DefinitionGetDepsVisitor(myOthers, myClassToNonStatic), false)) {
      Boolean good = def.accept(new AbstractDefinitionVisitor<Void, Boolean>() {
        @Override
        public Boolean visitFunction(Abstract.FunctionDefinition def, Void params) {
          return def.equals(definition) || doOrder(def);
        }

        @Override
        public Boolean visitAbstract(Abstract.AbstractDefinition def, Void params) {
          // Calss to abstracts (class fields) can not possibly add any dependencies
          // as in order to call a field we need to have an instance of the class
          // which (the class) is the actual dependency.
          return true;
        }

        @Override
        public Boolean visitData(Abstract.DataDefinition def, Void params) {
          return def.equals(definition) || doOrder(def);
        }

        @Override
        public Boolean visitConstructor(Abstract.Constructor def, Void params) {
          return def.getDataType().equals(definition) || doOrder(def.getDataType());
        }

        @Override
        public Boolean visitClass(Abstract.ClassDefinition def, Void params) {
          if (!def.equals(definition) && !doOrder(def)) {
            return false;
          }
          for (Abstract.Definition nonStatic : myClassToNonStatic.get(def)) {
            if (!(nonStatic.equals(definition)) && !doOrder(nonStatic)) {
              return false;
            }
          }
          return true;
        }

        @Override
        public Boolean visitImplement(Abstract.ImplementDefinition def, Void params) {
          return def.getParentStatement().getParentDefinition().equals(definition) || doOrder(def.getParentStatement().getParentDefinition());
        }
      }, null);
      if (!good)
        return false;
    }

    myVisiting.remove(definition);
    myResult.put(definition, enclosingClass);

    myVisited.add(definition);
    return true;
  }

  private static Abstract.ClassDefinition getEnclosingClass(Abstract.Definition definition) {
    Abstract.DefineStatement parentStatement = definition.getParentStatement();
    if (parentStatement == null) return null;

    Abstract.Definition parentDefinition = parentStatement.getParentDefinition();
    if (!Abstract.DefineStatement.StaticMod.STATIC.equals(parentStatement.getStaticMod()) && parentDefinition instanceof Abstract.ClassDefinition) {
      return (Abstract.ClassDefinition) parentDefinition;
    } else {
      return getEnclosingClass(parentDefinition);
    }
  }

  private Result getResult() {
    return myCycle == null ? new OKResult(myResult) : new CycleResult(myCycle);
  }

  public static Result order(Abstract.Definition definition) {
    return order(Collections.singletonList(definition));
  }

  public static Result order(Collection<Abstract.Definition> definitions) {
    Queue<Abstract.Definition> queue = new LinkedList<>(definitions);
    TypecheckingOrdering orderer = new TypecheckingOrdering(queue);
    while (!queue.isEmpty()) {
      if (!orderer.doOrder(queue.poll())) {
        return orderer.getResult();
      }
    }
    return orderer.getResult();
  }

  private static void typecheck(TypecheckerState state, Result result, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, boolean isPrelude) {
    if (result instanceof OKResult) {
      for (Map.Entry<Abstract.Definition, Abstract.ClassDefinition> entry : ((OKResult) result).order.entrySet()) {
        Abstract.Definition def = entry.getKey();
        DefinitionCheckTypeVisitor.typeCheck(state, (ClassDefinition) state.getTypechecked(entry.getValue()), def, new ProxyErrorReporter(def, errorReporter), isPrelude);
        Definition typechecked = state.getTypechecked(def);
        if (typechecked == null || typechecked.hasErrors()) {
          typecheckedReporter.typecheckingFailed(def);
        } else {
          typecheckedReporter.typecheckingSucceeded(def);
        }
      }
    } else if (result instanceof CycleResult) {
      List<Abstract.Definition> cycle = ((CycleResult) result).cycle;
      errorReporter.report(new TypeCheckingError(cycle.get(0), new CycleError(cycle)));
    } else {
      throw new IllegalStateException();
    }
  }

  public static boolean typecheck(TypecheckerState state, Abstract.Definition definition, ErrorReporter errorReporter) {
    return typecheck(state, definition, errorReporter, new TypecheckedReporter.Dummy());
  }

  public static boolean typecheck(TypecheckerState state, Abstract.Definition definition, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter) {
    Result result = order(definition);
    typecheck(state, result, errorReporter, typecheckedReporter, false);
    return result instanceof OKResult;
  }

  public static boolean typecheck(TypecheckerState state, List<Abstract.Definition> definitions, ErrorReporter errorReporter, boolean isPrelude) {
    return typecheck(state, definitions, errorReporter, new TypecheckedReporter.Dummy(), isPrelude);
  }

  public static boolean typecheck(TypecheckerState state, List<Abstract.Definition> definitions, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, boolean isPrelude) {
    Result result = order(definitions);
    typecheck(state, result, errorReporter, typecheckedReporter, isPrelude);
    return result instanceof OKResult;
  }
}
