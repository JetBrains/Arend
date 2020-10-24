package org.arend.typechecking.visitor;

import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;

import java.util.*;

public class ReplaceVarConcreteVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final Set<Referable> myVars;
  private final Set<Referable> myGlobalVars;
  private final Map<Referable, List<Referable>> myMap = new HashMap<>();
  private final Map<Referable, Referable> myOriginalRefs = new HashMap<>();

  public ReplaceVarConcreteVisitor(Set<Referable> globalVars) {
    myGlobalVars = globalVars;
    myVars = new HashSet<>(myGlobalVars);
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    List<Referable> refs = myMap.get(expr.getReferent());
    return refs != null ? new Concrete.ReferenceExpression(expr.getData(), refs.get(refs.size() - 1)) : expr;
  }

  private Referable addVar(Referable ref) {
    if (ref != null && !myVars.add(ref)) {
      Referable newRef = new LocalReferable(ref.getRefName());
      myMap.computeIfAbsent(ref, k -> new ArrayList<>()).add(newRef);
      myOriginalRefs.put(newRef, ref);
      return newRef;
    }
    return ref;
  }

  @Override
  protected void visitParameter(Concrete.Parameter parameter, Void params) {
    super.visitParameter(parameter, params);
    if (parameter instanceof Concrete.NameParameter) {
      ((Concrete.NameParameter) parameter).setReferable(addVar(((Concrete.NameParameter) parameter).getReferable()));
    } else if (parameter instanceof Concrete.TelescopeParameter) {
      List<Referable> newRefs = new ArrayList<>(parameter.getReferableList().size());
      for (Referable ref : parameter.getReferableList()) {
        newRefs.add(addVar(ref));
      }
      ((Concrete.TelescopeParameter) parameter).setReferableList(newRefs);
    }
  }

  private void freeVar(Referable ref) {
    if (ref == null) return;
    Referable origRef = myOriginalRefs.remove(ref);
    if (origRef != null) {
      myMap.computeIfPresent(origRef, (k, list) -> {
        if (list.size() > 1) {
          list.remove(list.size() - 1);
          return list;
        } else {
          return null;
        }
      });
    } else if (!myGlobalVars.contains(ref)) {
      myVars.remove(ref);
    }
  }

  private void freeVars(List<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      for (Referable ref : parameter.getReferableList()) {
        freeVar(ref);
      }
    }
  }

  private void freePattern(Concrete.Pattern pattern) {
    if (pattern instanceof Concrete.NamePattern) {
      freeVar(((Concrete.NamePattern) pattern).getReferable());
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      for (Concrete.Pattern subpattern : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
        freePattern(subpattern);
      }
    } else if (pattern instanceof Concrete.TuplePattern) {
      for (Concrete.Pattern subpattern : ((Concrete.TuplePattern) pattern).getPatterns()) {
        freePattern(subpattern);
      }
    }
    for (Concrete.TypedReferable ref : pattern.getAsReferables()) {
      freeVar(ref.referable);
    }
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    Concrete.Expression result = super.visitLam(expr, params);
    freeVars(expr.getParameters());
    return result;
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void params) {
    Concrete.Expression result = super.visitPi(expr, params);
    freeVars(expr.getParameters());
    return result;
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void params) {
    Concrete.Expression result = super.visitSigma(expr, params);
    freeVars(expr.getParameters());
    return result;
  }

  private void visitLetClausePattern(Concrete.LetClausePattern pattern) {
    pattern.setReferable(addVar(pattern.getReferable()));
    for (Concrete.LetClausePattern subpattern : pattern.getPatterns()) {
      visitLetClausePattern(subpattern);
    }
  }

  private void freeLetClausePattern(Concrete.LetClausePattern pattern) {
    freeVar(pattern.getReferable());
    for (Concrete.LetClausePattern subpattern : pattern.getPatterns()) {
      freeLetClausePattern(subpattern);
    }
  }

  @Override
  protected void visitLetClause(Concrete.LetClause clause, Void params) {
    visitLetClausePattern(clause.getPattern());
    super.visitLetClause(clause, params);
    freeVars(clause.getParameters());
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void params) {
    for (Concrete.CaseArgument caseArg : expr.getArguments()) {
      caseArg.referable = addVar(caseArg.referable);
    }
    super.visitCase(expr, null);
    for (Concrete.CaseArgument caseArg : expr.getArguments()) {
      freeVar(caseArg.referable);
    }
    visitClauses(expr.getClauses(), params);
    return expr;
  }

  @Override
  protected void visitPattern(Concrete.Pattern pattern, Void params) {
    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      if (namePattern.type != null) {
        namePattern.type = namePattern.type.accept(this, params);
      }
      namePattern.setReferable(addVar(namePattern.getReferable()));
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      for (Concrete.Pattern subpattern : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
        visitPattern(subpattern, null);
      }
    } else if (pattern instanceof Concrete.TuplePattern) {
      for (Concrete.Pattern subpattern : ((Concrete.TuplePattern) pattern).getPatterns()) {
        visitPattern(subpattern, null);
      }
    }
    for (Concrete.TypedReferable ref : pattern.getAsReferables()) {
      if (ref.type != null) {
        ref.type = ref.type.accept(this, params);
      }
      ref.referable = addVar(ref.referable);
    }
  }

  @Override
  protected void visitClause(Concrete.Clause clause, Void params) {
    super.visitClause(clause, params);
    if (clause.getPatterns() != null) {
      for (Concrete.Pattern pattern : clause.getPatterns()) {
        freePattern(pattern);
      }
    }
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    super.visitLet(expr, params);
    for (Concrete.LetClause clause : expr.getClauses()) {
      freeLetClausePattern(clause.getPattern());
    }
    return expr;
  }
}
