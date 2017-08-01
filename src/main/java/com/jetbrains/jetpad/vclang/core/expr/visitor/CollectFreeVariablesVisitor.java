package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;

import java.util.*;
import java.util.function.Consumer;

public class CollectFreeVariablesVisitor extends BaseExpressionVisitor<Set<Variable>, Void> {
  private Map<Binding, Set<Variable>> myFreeVariables = new HashMap<>();

  Set<Variable> getFreeVariables(Binding binding) {
    Set<Variable> freeVars = myFreeVariables.get(binding);
    return freeVars == null ? Collections.emptySet() : freeVars;
  }

  private void addFreeVariables(Binding binding, Set<Variable> variables) {
    if (!variables.isEmpty()) {
      myFreeVariables.computeIfAbsent(binding, k -> new HashSet<>()).addAll(variables);
    }
  }

  @Override
  public Void visitApp(AppExpression expr, Set<Variable> variables) {
    expr.getFunction().accept(this, variables);
    expr.getArgument().accept(this, variables);
    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Set<Variable> variables) {
    variables.add(expr.getBinding());
    return null;
  }

  @Override
  public Void visitInferenceReference(InferenceReferenceExpression expr, Set<Variable> variables) {
    if (expr.getSubstExpression() != null) {
      expr.getSubstExpression().accept(this, variables);
    }
    return null;
  }

  private void visitParameters(DependentLink link, Consumer<Set<Variable>> body, Set<Variable> variables) {
    if (!link.hasNext()) {
      if (body != null) {
        body.accept(variables);
      }
      return;
    }

    Set<Variable> newSet = variables.isEmpty() ? variables : new HashSet<>();
    DependentLink link1 = link.getNextTyped(null);
    visitParameters(link1.getNext(), body, newSet);
    addFreeVariables(link1, newSet);

    for (; link != link1; link = link.getNext()) {
      newSet.remove(link);
    }
    newSet.remove(link1);
    if (newSet != variables) {
      variables.addAll(newSet);
    }
    link1.getTypeExpr().accept(this, variables);
  }

  @Override
  public Void visitLam(LamExpression expr, Set<Variable> variables) {
    visitParameters(expr.getParameters(), vars -> expr.getBody().accept(this, vars), variables);
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, Set<Variable> variables) {
    visitParameters(expr.getParameters(), vars -> expr.getCodomain().accept(this, vars), variables);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Set<Variable> variables) {
    visitParameters(expr.getParameters(), null, variables);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Set<Variable> variables) {
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr, Set<Variable> variables) {
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, variables);
    }
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr, Set<Variable> variables) {
    visitSigma(expr.getSigmaType(), variables);
    for (Expression field : expr.getFields()) {
      field.accept(this, variables);
    }
    return null;
  }

  @Override
  public Void visitProj(ProjExpression expr, Set<Variable> variables) {
    expr.getExpression().accept(this, variables);
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, Set<Variable> variables) {
    visitClassCall(expr.getExpression(), variables);
    return null;
  }

  @Override
  public Void visitLet(LetExpression expr, Set<Variable> variables) {
    visitLetClauses(0, expr, variables);
    return null;
  }

  private void visitLetClauses(int index, LetExpression expr, Set<Variable> variables) {
    if (index == expr.getClauses().size()) {
      expr.getExpression().accept(this, variables);
      return;
    }

    Set<Variable> newSet = variables.isEmpty() ? variables : new HashSet<>();
    visitLetClauses(index + 1, expr, variables);
    LetClause clause = expr.getClauses().get(index);
    addFreeVariables(clause, newSet);
    newSet.remove(clause);
    if (variables != newSet) {
      variables.addAll(newSet);
    }
    clause.getExpression().accept(this, variables);
  }

  @Override
  public Void visitCase(CaseExpression expr, Set<Variable> variables) {
    for (Expression arg : expr.getArguments()) {
      arg.accept(this, variables);
    }
    visitParameters(expr.getParameters(), vars -> expr.getResultType().accept(this, vars), variables);
    visitParameters(expr.getElimTree().getParameters(), vars -> visitElimTree(expr.getElimTree(), vars), variables);
    return null;
  }

  private void visitElimTree(ElimTree elimTree, Set<Variable> variables) {
    if (elimTree instanceof LeafElimTree) {
      ((LeafElimTree) elimTree).getExpression().accept(this, variables);
    } else {
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        visitParameters(entry.getValue().getParameters(), vars -> visitElimTree(entry.getValue(), vars), variables);
      }
    }
  }

  @Override
  public Void visitOfType(OfTypeExpression expr, Set<Variable> variables) {
    expr.getExpression().accept(this, variables);
    expr.getTypeOf().accept(this, variables);
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Set<Variable> variables) {
    variables.add(expr.getDefinition());
    for (Expression arg : expr.getDefCallArguments()) {
      arg.accept(this, variables);
    }
    return null;
  }

  @Override
  public Void visitConCall(ConCallExpression expr, Set<Variable> variables) {
    visitDefCall(expr, variables);
    for (Expression arg : expr.getDataTypeArguments()) {
      arg.accept(this, variables);
    }
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Set<Variable> variables) {
    visitDefCall(expr, variables);
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : expr.getImplementedHere()) {
      entry.getValue().term.accept(this, variables);
    }
    return null;
  }
}
