package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionGetDepsVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.LocalErrorReporter;

import java.util.*;

public class TypecheckingOrdering {
  public static abstract class Result {
  }

  public static class OKResult extends Result {
    public final List<ResolvedName> order;

    OKResult(List<ResolvedName> order) {
      this.order = order;
    }
  }

  public static class CycleResult extends Result {
    public final List<ResolvedName> cycle;

    CycleResult(List<ResolvedName> cycle) {
      this.cycle = cycle;
    }
  }

  private List<ResolvedName> myCycle;
  private final Queue<ResolvedName> myOthers;
  private final List<ResolvedName> myResult;
  private final Set<ResolvedName> myVisited;
  private final Set<ResolvedName> myVisiting;

  private final HashMap<ResolvedName, List<ResolvedName>> myClassToNonStatic;

  private TypecheckingOrdering(Queue<ResolvedName> queue) {
    myCycle = null;
    myOthers = queue;
    myResult = new ArrayList<>();
    myVisited = new HashSet<>();
    myVisiting = new LinkedHashSet<>();
    myClassToNonStatic = new HashMap<>();
  }

  private boolean doOrder(final ResolvedName name) {
    if (myCycle != null)
      return false;
    if (myVisited.contains(name)) {
      return true;
    }

    NamespaceMember member = name.toNamespaceMember();
    if (member.isTypeChecked())
      return true;

    if (member.abstractDefinition != null) {
      if (myVisiting.contains(name)) {
        myCycle = new ArrayList<>(myVisiting);
        myCycle = myCycle.subList(myCycle.lastIndexOf(name), myCycle.size());
        return false;
      }

      myVisiting.add(name);
      for (final ResolvedName rn : member.abstractDefinition.accept(new DefinitionGetDepsVisitor(member.namespace, myOthers, myClassToNonStatic), false)) {
        boolean good = rn.toAbstractDefinition().accept(new AbstractDefinitionVisitor<Void, Boolean>() {
          @Override
          public Boolean visitFunction(Abstract.FunctionDefinition def, Void params) {
            return rn.equals(name) || doOrder(rn);
          }

          @Override
          public Boolean visitAbstract(Abstract.AbstractDefinition def, Void params) {
            return rn.parent.getResolvedName().equals(name) || doOrder(rn.parent.getResolvedName());
          }

          @Override
          public Boolean visitData(Abstract.DataDefinition def, Void params) {
            return rn.equals(name) || doOrder(rn);
          }

          @Override
          public Boolean visitConstructor(Abstract.Constructor def, Void params) {
            return rn.parent.getResolvedName().equals(name) || doOrder(rn.parent.getResolvedName());
          }

          @Override
          public Boolean visitClass(Abstract.ClassDefinition def, Void params) {
            if (!rn.equals(name) && !doOrder(rn)) {
              return false;
            }
            for (ResolvedName nonStatic : myClassToNonStatic.get(rn)) {
              if (!nonStatic.equals(name) && !doOrder(nonStatic)) {
                return false;
              }
            }
            return true;
          }
        }, null);

        if (!good)
          return false;
      }

      for (ResolvedName trueName = name; trueName.toAbstractDefinition() != null && trueName.toAbstractDefinition().getParentStatement() != null; trueName = trueName.parent.getResolvedName()) {
        if (!trueName.toAbstractDefinition().getParentStatement().isStatic()) {
          if (!doOrder(trueName.parent.getResolvedName()))
            return false;
        }
      }

      myVisiting.remove(name);
      if (!(member.abstractDefinition instanceof Abstract.Constructor) && !(member.abstractDefinition instanceof Abstract.AbstractDefinition))
        myResult.add(name);
    }

    myVisited.add(name);
    return true;
  }

  private Result getResult() {
    return myCycle == null ? new OKResult(myResult) : new CycleResult(myCycle);
  }

  public static Result order(ResolvedName rname) {
    return order(Collections.singletonList(rname));
  }

  public static Result order(Collection<ResolvedName> rnames) {
    ArrayDeque<ResolvedName> queue = new ArrayDeque<>(rnames);
    TypecheckingOrdering orderer = new TypecheckingOrdering(queue);
    while (!queue.isEmpty()) {
      if (!orderer.doOrder(queue.pollFirst())) {
        return orderer.getResult();
      }
    }
    return orderer.getResult();
  }

  private static void typecheck(Result result, ErrorReporter errorReporter) {
    if (result instanceof OKResult) {
      for (ResolvedName rn : ((OKResult) result).order) {
        DefinitionCheckTypeVisitor.typeCheck(rn.toNamespaceMember(), rn.parent, new LocalErrorReporter(rn, errorReporter));
      }
    } else if (result instanceof CycleResult) {
      StringBuilder errorMessage = new StringBuilder();
      errorMessage.append("Definition dependencies form a cycle: ");
      for (ResolvedName rn : ((CycleResult) result).cycle)
        errorMessage.append(rn.toString()).append(" - ");
      errorMessage.append(((CycleResult) result).cycle.get(0));
      errorReporter.report(new GeneralError(errorMessage.toString()));
    }
  }

  public static void typecheck(ResolvedName rname, ErrorReporter errorReporter) {
    typecheck(order(rname), errorReporter);
  }

  public static void typecheck(List<ResolvedName> rnames, ErrorReporter errorReporter) {
    typecheck(order(rnames), errorReporter);
  }
}
