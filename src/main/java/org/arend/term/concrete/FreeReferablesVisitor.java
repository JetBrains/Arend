package org.arend.term.concrete;

import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;

import java.util.List;
import java.util.Set;

public class FreeReferablesVisitor implements ConcreteExpressionVisitor<Void, TCReferable> {
  private final Set<? extends TCReferable> myReferables;

  public FreeReferablesVisitor(Set<? extends TCReferable> referables) {
    myReferables = referables;
  }

  public TCReferable visitPatterns(List<? extends Concrete.Pattern> patterns) {
    for (Concrete.Pattern pattern : patterns) {
      TCReferable found = visitPattern(pattern);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  public TCReferable visitPattern(Concrete.Pattern pattern) {
    if (pattern instanceof Concrete.NamePattern) {
      Referable ref = ((Concrete.NamePattern) pattern).getReferable();
      return ref instanceof TCReferable && myReferables.contains(ref) ? (TCReferable) ref : null;
    }
    if (pattern instanceof Concrete.ConstructorPattern) {
      Referable ref = ((Concrete.ConstructorPattern) pattern).getConstructor();
      return ref instanceof TCReferable && myReferables.contains(ref) ? (TCReferable) ref : visitPatterns(((Concrete.ConstructorPattern) pattern).getPatterns());
    }
    if (pattern instanceof Concrete.TuplePattern) {
      return visitPatterns(((Concrete.TuplePattern) pattern).getPatterns());
    }
    return null;
  }

  public TCReferable visitParameter(Concrete.Parameter parameter) {
    for (Referable ref : parameter.getReferableList()) {
      if (ref instanceof TCReferable && myReferables.contains(ref)) {
        return (TCReferable) ref;
      }
    }
    return null;
  }

  public TCReferable visitParameters(List<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      TCReferable ref = visitParameter(parameter);
      if (ref != null) {
        return ref;
      }
    }
    return null;
  }

  @Override
  public TCReferable visitApp(Concrete.AppExpression expr, Void params) {
    for (Concrete.Argument arg : expr.getArguments()) {
      TCReferable ref = arg.expression.accept(this, null);
      if (ref != null) {
        return ref;
      }
    }
    return expr.getFunction().accept(this, null);
  }

  @Override
  public TCReferable visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    return ref instanceof TCReferable && myReferables.contains(ref) ? (TCReferable) ref : null;
  }

  @Override
  public TCReferable visitThis(Concrete.ThisExpression expr, Void params) {
    return null;
  }

  @Override
  public TCReferable visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public TCReferable visitLam(Concrete.LamExpression expr, Void params) {
    TCReferable ref = visitParameters(expr.getParameters());
    return ref != null ? ref : expr.getBody().accept(this, null);
  }

  @Override
  public TCReferable visitPi(Concrete.PiExpression expr, Void params) {
    TCReferable ref = visitParameters(expr.getParameters());
    return ref != null ? ref : expr.codomain.accept(this, null);
  }

  @Override
  public TCReferable visitUniverse(Concrete.UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public TCReferable visitHole(Concrete.HoleExpression expr, Void params) {
    return null;
  }

  @Override
  public TCReferable visitGoal(Concrete.GoalExpression expr, Void params) {
    return expr.expression != null ? expr.expression.accept(this, null) : null;
  }

  @Override
  public TCReferable visitTuple(Concrete.TupleExpression expr, Void params) {
    for (Concrete.Expression field : expr.getFields()) {
      TCReferable ref = field.accept(this, null);
      if (ref != null) {
        return ref;
      }
    }
    return null;
  }

  @Override
  public TCReferable visitSigma(Concrete.SigmaExpression expr, Void params) {
    return visitParameters(expr.getParameters());
  }

  @Override
  public TCReferable visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void params) {
    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      TCReferable ref = elem.expression.accept(this, null);
      if (ref != null) {
        return ref;
      }
    }
    return null;
  }

  public TCReferable visitClause(Concrete.FunctionClause clause) {
    TCReferable ref = visitPatterns(clause.getPatterns());
    return ref != null ? ref : clause.expression != null ? clause.expression.accept(this, null) : null;
  }

  @Override
  public TCReferable visitCase(Concrete.CaseExpression expr, Void params) {
    for (Concrete.CaseArgument arg : expr.getArguments()) {
      TCReferable ref = arg.expression.accept(this, null);
      if (ref != null) {
        return ref;
      }
      ref = arg.type != null ? arg.type.accept(this, null) : null;
      if (ref != null) {
        return ref;
      }
    }
    if (expr.getResultType() != null) {
      TCReferable ref = expr.getResultType().accept(this, null);
      if (ref != null) {
        return ref;
      }
    }
    if (expr.getResultTypeLevel() != null) {
      TCReferable ref = expr.getResultTypeLevel().accept(this, null);
      if (ref != null) {
        return ref;
      }
    }
    for (Concrete.FunctionClause clause : expr.getClauses()) {
      TCReferable ref = visitClause(clause);
      if (ref != null) {
        return ref;
      }
    }
    return null;
  }

  @Override
  public TCReferable visitProj(Concrete.ProjExpression expr, Void params) {
    return expr.expression.accept(this, null);
  }

  private TCReferable visitClassFieldImpl(Concrete.ClassFieldImpl fieldImpl) {
    Referable ref = fieldImpl.getImplementedField();
    if (ref instanceof TCReferable && myReferables.contains(ref)) {
      return (TCReferable) ref;
    }
    TCReferable tcRef = fieldImpl.implementation == null ? null : fieldImpl.implementation.accept(this, null);
    if (tcRef != null) {
      return tcRef;
    }
    for (Concrete.ClassFieldImpl subFieldImpl : fieldImpl.subClassFieldImpls) {
      tcRef = visitClassFieldImpl(subFieldImpl);
      if (tcRef != null) {
        return tcRef;
      }
    }
    return null;
  }

  @Override
  public TCReferable visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    for (Concrete.ClassFieldImpl fieldImpl : expr.getStatements()) {
      TCReferable ref = visitClassFieldImpl(fieldImpl);
      if (ref != null) {
        return ref;
      }
    }
    return expr.getBaseClassExpression().accept(this, null);
  }

  @Override
  public TCReferable visitNew(Concrete.NewExpression expr, Void params) {
    return expr.expression.accept(this, null);
  }

  @Override
  public TCReferable visitLet(Concrete.LetExpression expr, Void params) {
    for (Concrete.LetClause clause : expr.getClauses()) {
      TCReferable ref = visitParameters(clause.getParameters());
      if (ref != null) {
        return ref;
      }
      if (clause.resultType != null) {
        ref = clause.resultType.accept(this, null);
        if (ref != null) {
          return ref;
        }
      }
      ref = clause.term.accept(this, null);
      if (ref != null) {
        return ref;
      }
    }
    return expr.expression.accept(this, null);
  }

  @Override
  public TCReferable visitNumericLiteral(Concrete.NumericLiteral expr, Void params) {
    return null;
  }

  @Override
  public TCReferable visitTyped(Concrete.TypedExpression expr, Void params) {
    TCReferable ref = expr.expression.accept(this, null);
    return ref != null ? ref : expr.type.accept(this, null);
  }
}
