package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionGetDepsVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.CycleError;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.*;

public class TypecheckingOrdering {
  public static abstract class Result {
  }

  public static class OKResult extends Result {
    public final LinkedHashSet<Abstract.Definition> order;

    OKResult(LinkedHashSet<Abstract.Definition> order) {
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
  private final LinkedHashSet<Abstract.Definition> myResult;
  private final Set<Abstract.Definition> myVisited;
  private final LinkedHashSet<Abstract.Definition> myVisiting;

  private final HashMap<Abstract.Definition, List<Abstract.Definition>> myClassToNonStatic;

  private TypecheckingOrdering(Queue<Abstract.Definition> queue) {
    myCycle = null;
    myOthers = queue;
    myResult = new LinkedHashSet<>();
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
    for (final Referable def : definition.accept(new DefinitionGetDepsVisitor(myOthers, myClassToNonStatic), false)) {
      if (def instanceof Abstract.Definition) {
        Boolean good = ((Abstract.Definition) def).accept(new AbstractDefinitionVisitor<Void, Boolean>() {
          @Override
          public Boolean visitFunction(Abstract.FunctionDefinition def, Void params) {
            return def.equals(definition) || doOrder(def);
          }

          @Override
          public Boolean visitAbstract(Abstract.AbstractDefinition def, Void params) {
            return def.getParentStatement().getParentDefinition().equals(definition) || doOrder(def.getParentStatement().getParentDefinition());
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
        }, null);
        if (!good)
          return false;
      }
    }

    for (Abstract.Definition curThis = definition; curThis != null && curThis.getParentStatement() != null; curThis = curThis.getParentStatement().getParentDefinition()) {
      if (curThis.getParentStatement().getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC) {
        if (!doOrder(curThis.getParentStatement().getParentDefinition()))
          return false;
      }
    }

    myVisiting.remove(definition);
    myResult.add(definition);

    myVisited.add(definition);
    return true;
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

  private static void typecheck(Result result, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter) {
    if (result instanceof OKResult) {
      TypecheckerState state = new TypecheckerState();
      for (Abstract.Definition def : ((OKResult) result).order) {
        DefinitionCheckTypeVisitor.typeCheck(state, def,new LocalErrorReporter(def, errorReporter));
        Definition typechecked = state.getTypechecked(def);
        if (typechecked == null || typechecked.hasErrors()) {
          typecheckedReporter.typecheckingFailed(def);
        } else {
          typecheckedReporter.typecheckingSucceeded(def);
        }
      }
    } else if (result instanceof CycleResult) {
      errorReporter.report(new CycleError(((CycleResult) result).cycle));
    } else {
      throw new IllegalStateException();
    }
  }
  public static void typecheck(Abstract.Definition definition, ErrorReporter errorReporter) {
    typecheck(definition, errorReporter, new DummyTypecheckedReported());
  }

  public static void typecheck(Abstract.Definition definition, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter) {
    typecheck(order(definition), errorReporter, typecheckedReporter);
  }

  public static void typecheck(List<Abstract.Definition> definitions, ErrorReporter errorReporter) {
    typecheck(definitions, errorReporter, new DummyTypecheckedReported());
  }

  public static void typecheck(List<Abstract.Definition> definitions, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter) {
    typecheck(order(definitions), errorReporter, typecheckedReporter);
  }
}
