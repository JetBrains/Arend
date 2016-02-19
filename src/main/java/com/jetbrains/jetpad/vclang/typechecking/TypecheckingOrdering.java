package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.BaseDefinition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionGetDepsVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.LocalErrorReporter;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.definition.BaseDefinition.Helper.toNamespaceMember;

public class TypecheckingOrdering {
  public static abstract class Result {
  }

  public static class OKResult extends Result {
    public final Map<Abstract.Definition, NamespaceMember> order;

    OKResult(Map<Abstract.Definition, NamespaceMember> order) {
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
  private final Map<Abstract.Definition, NamespaceMember> myResult;
  private final Set<Abstract.Definition> myVisited;
  private final Set<Abstract.Definition> myVisiting;

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

    NamespaceMember member = toNamespaceMember(definition);
    if (toNamespaceMember(definition).isTypeChecked()) {
      myVisited.add(definition);
      return true;
    }

    if (myVisiting.contains(definition)) {
      myCycle = new ArrayList<>(myVisiting);
      myCycle = myCycle.subList(myCycle.lastIndexOf(definition), myCycle.size());
      return false;
    }

    myVisiting.add(definition);
    for (final BaseDefinition def : member.abstractDefinition.accept(new DefinitionGetDepsVisitor(member.namespace, myOthers, myClassToNonStatic), false)) {
      if (def instanceof Abstract.Definition) {
        Boolean good = ((Abstract.Definition) def).accept(new AbstractDefinitionVisitor<Void, Boolean>() {
          @Override
          public Boolean visitFunction(Abstract.FunctionDefinition def, Void params) {
            return def == definition || doOrder(def);
          }

          @Override
          public Boolean visitAbstract(Abstract.AbstractDefinition def, Void params) {
            return def.getParentStatement().getParentDefinition() == definition || doOrder(def.getParentStatement().getParentDefinition());
          }

          @Override
          public Boolean visitData(Abstract.DataDefinition def, Void params) {
            return def == definition || doOrder(def);
          }

          @Override
          public Boolean visitConstructor(Abstract.Constructor def, Void params) {
            return def.getDataType() == definition || doOrder(def.getDataType());
          }

          @Override
          public Boolean visitClass(Abstract.ClassDefinition def, Void params) {
            if (!(def == definition) && !doOrder(def)) {
              return false;
            }
            for (Abstract.Definition nonStatic : myClassToNonStatic.get(def)) {
              if (!(nonStatic == definition) && !doOrder(nonStatic)) {
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
    myResult.put(definition, member);

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
      for (Abstract.Definition def : ((OKResult) result).order.keySet()) {
        NamespaceMember member = ((OKResult) result).order.get(def);
        DefinitionCheckTypeVisitor.typeCheck(member, new LocalErrorReporter(member.getResolvedName(), errorReporter));
        if (member.definition == null || member.definition.hasErrors()) {
          typecheckedReporter.typecheckingFailed(def);
        } else {
          typecheckedReporter.typecheckingSucceeded(def);
        }
      }
    } else if (result instanceof CycleResult) {
      StringBuilder errorMessage = new StringBuilder();
      errorMessage.append("Definition dependencies form a cycle: ");
      for (Abstract.Definition def : ((CycleResult) result).cycle)
        errorMessage.append(toNamespaceMember(def).getResolvedName().getFullName()).append(" - ");
      errorMessage.append(toNamespaceMember(((CycleResult) result).cycle.get(0)).getResolvedName().getFullName());
      errorReporter.report(new GeneralError(errorMessage.toString()));
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
