package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
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
  private final List<ResolvedName> myResult;
  private final Set<ResolvedName> myVisited;
  private final Set<ResolvedName> myVisiting;

  private TypecheckingOrdering() {
    myCycle = null;
    myResult = new ArrayList<>();
    myVisited = new HashSet<>();
    myVisiting = new LinkedHashSet<>();
  }

  private boolean doOrder(ResolvedName name) {
    if (myCycle != null)
      return false;
    if (myVisited.contains(name)) {
      return true;
    }

    NamespaceMember member = name.toNamespaceMember();
    if (member.isTypeChecked())
      return true;

    if (member.abstractDefinition == null) {
      for (ResolvedName rn : DefinitionGetDepsVisitor.visitNamespace(member.namespace)) {
        if (!rn.equals(name)) {
          if (!rn.equals(name) && !doOrder(rn)) {
            return false;
          }
        }
      }
    } else {
      if (myVisiting.contains(name)) {
        myCycle = new ArrayList<>(myVisiting);
        myCycle = myCycle.subList(myCycle.lastIndexOf(name), myCycle.size());
        return false;
      }

      myVisiting.add(name);
      for (ResolvedName rn : member.abstractDefinition.accept(new DefinitionGetDepsVisitor(member.namespace), null)) {
        if (!rn.equals(name) && !doOrder(rn)) {
          return false;
        }
      }

      myVisiting.remove(name);
      if (!(member.abstractDefinition instanceof Abstract.Constructor))
        myResult.add(name);
    }

    myVisited.add(name);
    return true;
  }

  private Result getResult() {
    return myCycle == null ? new OKResult(myResult) : new CycleResult(myCycle);
  }

  public static Result order(ResolvedName rname) {
    TypecheckingOrdering orderer = new TypecheckingOrdering();
    orderer.doOrder(rname);
    return orderer.getResult();
  }

  public static Result order(Collection<ResolvedName> rnames) {
    TypecheckingOrdering orderer = new TypecheckingOrdering();
    for (ResolvedName rn : rnames) {
      if (!orderer.doOrder(rn)) {
        return orderer.getResult();
      }
    }
    return orderer.getResult();
  }

  private static void typecheck(Result result, ErrorReporter errorReporter) {
    if (result instanceof OKResult) {
      for (ResolvedName rn : ((OKResult) result).order) {
        NamespaceMember member = rn.toNamespaceMember();
        if (member.abstractDefinition instanceof Abstract.ClassDefinition) {
          member.definition = new ClassDefinition(member.namespace.getParent(), member.abstractDefinition.getName());
        }
      }
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
