package org.arend.core.expr.visitor;

import org.arend.core.definition.ClassField;
import org.arend.core.expr.*;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.ExpressionMapper;

import java.util.HashMap;
import java.util.Map;

public class RecreateExpressionVisitor extends SubstVisitor {
  private final ExpressionMapper myMapper;

  public RecreateExpressionVisitor(ExpressionMapper mapper) {
    super(new ExprSubstitution(), LevelSubstitution.EMPTY);
    myMapper = mapper;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitApp(expr, params);
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitDefCall(expr, params);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitConCall(expr, params);
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }

    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassCallExpression classCall = new ClassCallExpression(expr.getDefinition(), expr.getSortArgument().subst(getLevelSubstitution()), fieldSet, expr.getSort().subst(getLevelSubstitution()), expr.getUniverseKind());
    if (expr.getImplementedHere().isEmpty()) {
      return classCall;
    }

    getExprSubstitution().add(expr.getThisBinding(), new ReferenceExpression(classCall.getThisBinding()));
    for (ClassField field : classCall.getDefinition().getFields()) {
      Expression impl = expr.getAbsImplementationHere(field);
      if (impl != null) {
        fieldSet.put(field, impl.accept(this, null));
      }
    }
    getExprSubstitution().remove(expr.getThisBinding());
    return classCall;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitFieldCall(expr, params);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitReference(expr, params);
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitInferenceReference(expr, params);
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitSubst(expr, params);
  }

  @Override
  public Expression visitLam(LamExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitLam(expr, params);
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitPi(expr, params);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitSigma(expr, params);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitUniverse(expr, params);
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitError(expr, params);
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitTuple(expr, params);
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitProj(expr, params);
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitNew(expr, params);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitPEval(expr, params);
  }

  @Override
  public Expression visitLet(LetExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitLet(expr, params);
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitCase(expr, params);
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitOfType(expr, params);
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitInteger(expr, params);
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitFunCall(expr, params);
  }

  @Override
  public Expression visitDataCall(DataCallExpression expr, Void params) {
    CoreExpression result = myMapper.map(expr);
    if (result instanceof Expression) {
      return (Expression) result;
    }
    return super.visitDataCall(expr, params);
  }
}
