package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.PersistentEvaluatingBinding;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.ext.variable.Variable;
import org.arend.ext.variable.VariableImpl;
import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.ext.module.LongName;
import org.arend.ext.prettyprinting.DefinitionRenamer;

import java.util.*;
import java.util.function.Consumer;

public class CollectFreeVariablesVisitor extends VoidExpressionVisitor<Set<Variable>> {
  private final DefinitionRenamer myDefinitionRenamer;
  private final Map<Binding, Set<Variable>> myFreeVariables = new HashMap<>();

  CollectFreeVariablesVisitor(DefinitionRenamer definitionRenamer) {
    myDefinitionRenamer = definitionRenamer;
  }

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
    if (expr.getBinding() instanceof PersistentEvaluatingBinding) {
      ((PersistentEvaluatingBinding) expr.getBinding()).getExpression().accept(this, variables);
    } else {
      variables.add(expr.getBinding());
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

    Set<Variable> newSet = new HashSet<>();
    DependentLink link1 = link.getNextTyped(null);
    visitParameters(link1.getNext(), body, newSet);
    addFreeVariables(link1, newSet);

    for (; link != link1; link = link.getNext()) {
      newSet.remove(link);
    }
    newSet.remove(link1);
    variables.addAll(newSet);
    link1.getTypeExpr().accept(this, variables);
  }

  @Override
  public void visitParameters(DependentLink link, Set<Variable> variables) {
    visitParameters(link, null, variables);
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
  public Void visitLet(LetExpression expr, Set<Variable> variables) {
    visitLetClauses(0, expr, variables);
    return null;
  }

  private void visitLetClauses(int index, LetExpression expr, Set<Variable> variables) {
    if (index == expr.getClauses().size()) {
      expr.getExpression().accept(this, variables);
      return;
    }

    Set<Variable> newSet = new HashSet<>();
    visitLetClauses(index + 1, expr, newSet);
    HaveClause clause = expr.getClauses().get(index);
    addFreeVariables(clause, newSet);
    newSet.remove(clause);
    variables.addAll(newSet);
    clause.getExpression().accept(this, variables);
  }

  @Override
  public Void visitCase(CaseExpression expr, Set<Variable> variables) {
    for (Expression arg : expr.getArguments()) {
      arg.accept(this, variables);
    }
    visitParameters(expr.getParameters(), vars -> {
      expr.getResultType().accept(this, vars);
      if (expr.getResultTypeLevel() != null) {
        expr.getResultTypeLevel().accept(this, vars);
      }
    }, variables);
    for (var clause : expr.getElimBody().getClauses()) {
      visitParameters(clause.getParameters(), vars -> {
        if (clause.getExpression() != null) {
          clause.getExpression().accept(this, vars);
        }
      }, variables);
    }
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Set<Variable> variables) {
    LongName longName = myDefinitionRenamer.renameDefinition(expr.getDefinition().getRef());
    if (longName != null) {
      variables.add(new VariableImpl(longName.getFirstName()));
    } else {
      String alias = expr.getDefinition().getReferable().getAliasName();
      variables.add(alias != null ? new VariableImpl(alias) : expr.getDefinition());
    }
    return super.visitDefCall(expr, variables);
  }

  @Override
  public Void visitFunction(FunctionDefinition def, Set<Variable> variables) {
    visitParameters(def.getParameters(), vars -> {
      def.getResultType().accept(this, vars);
      if (def.getResultTypeLevel() != null) def.getResultTypeLevel().accept(this, vars);
      visitBody(def.getReallyActualBody(), vars);
    }, variables);
    return null;
  }
}
