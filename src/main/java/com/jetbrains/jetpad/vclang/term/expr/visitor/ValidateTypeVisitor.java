package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidateTypeVisitor extends BaseExpressionVisitor<Void, ValidateTypeVisitor.Result> {

  public interface Result {
    Result join(Result other);
  }

  public static class OKResult implements Result {
    @Override
    public Result join(Result other) {
      return other;
    }

    @Override
    public String toString() {
      return "OKResult{}";
    }
  }

  private final static OKResult OK = new OKResult();

  public static class FailedResult implements Result {
    @Override
    public String toString() {
      return "FailedResult{" +
              "failedExpressions=" + failedExpressions +
              ", reasons=" + reasons +
              '}';
    }

    public List<Expression> getFailedExpressions() {
      return failedExpressions;
    }

    public List<Reason> getReasons() {
      return reasons;
    }

    @Override
    public Result join(Result other) {
      if (other instanceof OKResult) {
        return this;
      } else if (other instanceof FailedResult) {
        FailedResult otherFailed = (FailedResult) other;
        List<Expression> failedExpressions = new ArrayList<>(this.failedExpressions);
        failedExpressions.addAll(otherFailed.failedExpressions);
        List<Reason> reasons = new ArrayList<>(this.reasons);
        reasons.addAll(otherFailed.reasons);
        return new FailedResult(failedExpressions, reasons);
      } else {
        throw new UnsupportedOperationException();
      }

    }

    public enum Reason {
      PiExpected, ProjNotTuple, AlreadyError, TypeMismatch, TooFewSigmas
    };

    private final List<Expression> failedExpressions;
    private final List<Reason> reasons;

    public FailedResult(Expression failedExpression, Reason reason) {
      this.failedExpressions = Collections.singletonList(failedExpression);
      this.reasons = Collections.singletonList(reason);
    }

    public FailedResult(List<Expression> failedExpressions, List<Reason> reasons) {
      this.failedExpressions = failedExpressions;
      this.reasons = reasons;
    }
  }

  @Override
  public Result visitDefCall(DefCallExpression expr, Void params) {
    return expr.getType().accept(this, params);
  }

  @Override
  public Result visitApp(AppExpression expr, Void params) {
    Expression fun = expr.getFunction();
    Expression funType = fun.getType();
    if (!(funType instanceof PiExpression)) {
      return new FailedResult(expr, FailedResult.Reason.PiExpected);
    }
    return OK;
  }

  @Override
  public Result visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getType().accept(this, params);
  }

  @Override
  public Result visitLam(LamExpression expr, Void params) {
    Result res = expr.getType().accept(this, params);
    res = res.join(expr.getBody().accept(this, params));
    return res;
  }

  @Override
  public Result visitPi(PiExpression expr, Void params) {
    Result res = expr.getType().accept(this, params);
    res = res.join(expr.getCodomain().accept(this, params));
    return res;
  }

  @Override
  public Result visitSigma(SigmaExpression expr, Void params) {
    return expr.getType().accept(this, params);
  }

  @Override
  public Result visitUniverse(UniverseExpression expr, Void params) {
    return OK;
  }

  @Override
  public Result visitError(ErrorExpression expr, Void params) {
    return new FailedResult(expr, FailedResult.Reason.AlreadyError);
  }

  @Override
  public Result visitTuple(TupleExpression expr, Void params) {
    SigmaExpression type = expr.getType();
    DependentLink link = type.getParameters();
    Result res = type.accept(this, params);
    for (Expression field : expr.getFields()) {
      res = res.join(field.accept(this, params));
      if (!field.getType().equals(link.getType())) {
        res = res.join(new FailedResult(field, FailedResult.Reason.TypeMismatch));
      }
      if (!link.hasNext()) {
        res = res.join(new FailedResult(expr, FailedResult.Reason.TooFewSigmas));
      }
      link = link.getNext();
    }
    return res;
  }

  @Override
  public Result visitProj(ProjExpression expr, Void params) {
    Result res = expr.getType().accept(this, params);
    return res.join(expr.getExpression().accept(this, params));
  }

  @Override
  public Result visitNew(NewExpression expr, Void params) {
    return expr.getType().accept(this, params);
  }

  @Override
  public Result visitLet(LetExpression letExpression, Void params) {
    return letExpression.getType().accept(this, params);
  }
}
