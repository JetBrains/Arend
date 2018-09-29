package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;

import java.util.*;
import java.util.function.Consumer;

public class CollectFreeVariablesVisitor extends VoidExpressionVisitor<Set<Variable>> {
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
  public Void visitReference(ReferenceExpression expr, Set<Variable> variables) {
    variables.add(expr.getBinding());
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
    visitLetClauses(index + 1, expr, newSet);
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

  @Override
  protected void visitElimTree(ElimTree elimTree, Set<Variable> variables) {
    if (elimTree instanceof LeafElimTree) {
      ((LeafElimTree) elimTree).getExpression().accept(this, variables);
    } else {
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        visitParameters(entry.getValue().getParameters(), vars -> visitElimTree(entry.getValue(), vars), variables);
      }
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Set<Variable> variables) {
    variables.add(expr.getDefinition());
    return super.visitDefCall(expr, variables);
  }
}
