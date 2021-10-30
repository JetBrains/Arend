package org.arend.core.expr.visitor;

import org.arend.core.context.binding.PersistentEvaluatingBinding;
import org.arend.core.definition.*;
import org.arend.core.expr.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SizeExpressionVisitor extends VoidExpressionVisitor<Void> {
  private int mySize;
  private final Set<PersistentEvaluatingBinding> myVisited = new HashSet<>();

  private SizeExpressionVisitor() {
  }

  public static int getSize(Expression expr) {
    SizeExpressionVisitor visitor = new SizeExpressionVisitor();
    expr.accept(visitor, null);
    return visitor.mySize;
  }

  public static int getSize(Definition def) {
    SizeExpressionVisitor visitor = new SizeExpressionVisitor();
    visitor.visitParameters(def.getParameters(), null);
    if (def instanceof FunctionDefinition) {
      FunctionDefinition funDef = (FunctionDefinition) def;
      funDef.getResultType().accept(visitor, null);
      if (funDef.getResultTypeLevel() != null) funDef.getResultTypeLevel().accept(visitor, null);
      visitor.visitBody(funDef.getReallyActualBody(), null);
    } else if (def instanceof DataDefinition) {
      for (Constructor constructor : ((DataDefinition) def).getConstructors()) {
        visitor.visitParameters(constructor.getParameters(), null);
      }
    } else if (def instanceof ClassDefinition) {
      ClassDefinition classDef = (ClassDefinition) def;
      for (ClassField field : classDef.getPersonalFields()) {
        field.getResultType().accept(visitor, null);
        if (field.getTypeLevel() != null) field.getTypeLevel().accept(visitor, null);
      }
      for (Map.Entry<ClassField, AbsExpression> entry : classDef.getImplemented()) {
        entry.getValue().getExpression().accept(visitor, null);
      }
      for (Map.Entry<ClassField, AbsExpression> entry : classDef.getDefaults()) {
        entry.getValue().getExpression().accept(visitor, null);
      }
      for (Map.Entry<ClassField, PiExpression> entry : classDef.getOverriddenFields()) {
        entry.getValue().getCodomain().accept(visitor, null);
      }
    } else {
      throw new IllegalStateException();
    }
    return visitor.mySize;
  }

  @Override
  public Void visitApp(AppExpression expr, Void params) {
    mySize++;
    return super.visitApp(expr, params);
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    mySize++;
    return super.visitDefCall(expr, params);
  }

  @Override
  protected void processConCall(ConCallExpression expr, Void params) {
    mySize++;
  }

  @Override
  public Void visitPath(PathExpression expr, Void params) {
    mySize++;
    return super.visitPath(expr, params);
  }

  @Override
  public Void visitAt(AtExpression expr, Void params) {
    mySize++;
    return super.visitAt(expr, params);
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    mySize++;
    if (expr.getBinding() instanceof PersistentEvaluatingBinding) {
      PersistentEvaluatingBinding binding = (PersistentEvaluatingBinding) expr.getBinding();
      if (myVisited.add(binding)) {
        binding.getExpression().accept(this, null);
      }
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    mySize++;
    return super.visitInferenceReference(expr, params);
  }

  @Override
  public Void visitSubst(SubstExpression expr, Void params) {
    mySize++;
    return super.visitSubst(expr, params);
  }

  @Override
  public Void visitLam(LamExpression expr, Void params) {
    mySize++;
    return super.visitLam(expr, params);
  }

  @Override
  public Void visitPi(PiExpression expr, Void params) {
    mySize++;
    return super.visitPi(expr, params);
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Void params) {
    mySize++;
    return super.visitSigma(expr, params);
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Void params) {
    mySize++;
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr, Void params) {
    mySize++;
    return super.visitError(expr, params);
  }

  @Override
  public Void visitTuple(TupleExpression expr, Void params) {
    mySize++;
    return super.visitTuple(expr, params);
  }

  @Override
  public Void visitProj(ProjExpression expr, Void params) {
    mySize++;
    return super.visitProj(expr, params);
  }

  @Override
  public Void visitNew(NewExpression expr, Void params) {
    mySize++;
    return super.visitNew(expr, params);
  }

  @Override
  public Void visitPEval(PEvalExpression expr, Void params) {
    mySize++;
    return super.visitPEval(expr, params);
  }

  @Override
  public Void visitLet(LetExpression expr, Void params) {
    mySize++;
    return super.visitLet(expr, params);
  }

  @Override
  public Void visitCase(CaseExpression expr, Void params) {
    mySize++;
    return super.visitCase(expr, params);
  }

  @Override
  public Void visitOfType(OfTypeExpression expr, Void params) {
    mySize++;
    return super.visitOfType(expr, params);
  }

  @Override
  public Void visitInteger(IntegerExpression expr, Void params) {
    mySize++;
    return null;
  }

  @Override
  public Void visitTypeCoerce(TypeCoerceExpression expr, Void params) {
    mySize++;
    return super.visitTypeCoerce(expr, params);
  }

  @Override
  public Void visitArray(ArrayExpression expr, Void params) {
    mySize++;
    return super.visitArray(expr, params);
  }
}
