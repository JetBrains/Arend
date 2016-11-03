package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public class InferenceReferenceExpression extends Expression {
  private InferenceVariable myVar;
  private Expression mySubstExpression;

  public InferenceReferenceExpression(InferenceVariable binding, Equations equations) {
    myVar = binding;
    myVar.setReference(this);

    Type type = myVar.getType().normalize(NormalizeVisitor.Mode.WHNF);
    if (type.toExpression() != null && type.toExpression().toClassCall() != null) {
      ClassCallExpression classCall = type.toExpression().toClassCall();
      FieldSet fieldSet = classCall.getFieldSet();
      if (!fieldSet.getFields().isEmpty()) {
        for (ClassField field : fieldSet.getFields()) {
          FieldSet.Implementation impl = fieldSet.getImplementation(field);
          if (impl != null) {
            equations.add(new FieldCallExpression(field, this), impl.term, Equations.CMP.EQ, myVar.getSourceNode(), myVar);
          }
        }
        type = classCall instanceof ClassViewCallExpression ? new ClassViewCallExpression(classCall.getDefinition(), classCall.getPolyArguments(), ((ClassViewCallExpression) classCall).getClassView()) : new ClassCallExpression(classCall.getDefinition(), classCall.getPolyArguments());
      }
    }
    myVar.setType(type);
  }

  public InferenceReferenceExpression(InferenceVariable binding, Expression substExpression) {
    myVar = binding;
    mySubstExpression = substExpression;
  }

  public InferenceVariable getVariable() {
    return mySubstExpression == null ? myVar : null;
  }

  public InferenceVariable getOriginalVariable() {
    return myVar;
  }

  public Expression getSubstExpression() {
    return mySubstExpression;
  }

  public void setSubstExpression(Expression substExpression) {
    mySubstExpression = substExpression;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInferenceReference(this, params);
  }

  @Override
  public Expression getFunction() {
    return mySubstExpression != null ? mySubstExpression.getFunction() : super.getFunction();
  }

  @Override
  public List<? extends Expression> getArguments() {
    return mySubstExpression != null ? mySubstExpression.getArguments() : super.getArguments();
  }

  @Override
  public AppExpression toApp() {
    return mySubstExpression != null ? mySubstExpression.toApp() : null;
  }

  @Override
  public ClassCallExpression toClassCall() {
    return mySubstExpression != null ? mySubstExpression.toClassCall() : null;
  }

  @Override
  public ConCallExpression toConCall() {
    return mySubstExpression != null ? mySubstExpression.toConCall() : null;
  }

  @Override
  public DataCallExpression toDataCall() {
    return mySubstExpression != null ? mySubstExpression.toDataCall() : null;
  }

  @Override
  public DefCallExpression toDefCall() {
    return mySubstExpression != null ? mySubstExpression.toDefCall() : null;
  }

  @Override
  public ErrorExpression toError() {
    return mySubstExpression != null ? mySubstExpression.toError() : null;
  }

  @Override
  public FieldCallExpression toFieldCall() {
    return mySubstExpression != null ? mySubstExpression.toFieldCall() : null;
  }

  @Override
  public FunCallExpression toFunCall() {
    return mySubstExpression != null ? mySubstExpression.toFunCall() : null;
  }

  @Override
  public LamExpression toLam() {
    return mySubstExpression != null ? mySubstExpression.toLam() : null;
  }

  @Override
  public LetExpression toLet() {
    return mySubstExpression != null ? mySubstExpression.toLet() : null;
  }

  @Override
  public NewExpression toNew() {
    return mySubstExpression != null ? mySubstExpression.toNew() : null;
  }

  @Override
  public OfTypeExpression toOfType() {
    return mySubstExpression != null ? mySubstExpression.toOfType() : null;
  }

  @Override
  public PiExpression toPi() {
    return mySubstExpression != null ? mySubstExpression.toPi() : null;
  }

  @Override
  public ProjExpression toProj() {
    return mySubstExpression != null ? mySubstExpression.toProj() : null;
  }

  @Override
  public ReferenceExpression toReference() {
    return mySubstExpression != null ? mySubstExpression.toReference() : null;
  }

  @Override
  public InferenceReferenceExpression toInferenceReference() {
    return this;
  }

  @Override
  public SigmaExpression toSigma() {
    return mySubstExpression != null ? mySubstExpression.toSigma() : null;
  }

  @Override
  public TupleExpression toTuple() {
    return mySubstExpression != null ? mySubstExpression.toTuple() : null;
  }

  @Override
  public UniverseExpression toUniverse() {
    return mySubstExpression != null ? mySubstExpression.toUniverse() : null;
  }
}
