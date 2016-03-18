package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class ValidateTypeVisitor extends BaseExpressionVisitor<Expression, Void>
        implements ElimTreeNodeVisitor<Expression, Void> {

  public static class ErrorReporter {
    private final ArrayList<Expression> expressions = new ArrayList<>();
    private final ArrayList<String> reasons = new ArrayList<>();

    public void addError(Expression expr, String reason) {
      expressions.add(expr);
      reasons.add(reason);
    }

    public ArrayList<Expression> getExpressions() {
      return expressions;
    }

    public ArrayList<String> getReasons() {
      return reasons;
    }

    public int errors() {
      return expressions.size();
    }

    @Override
    public String toString() {
      return expressions + " " + reasons;
    }
  }

  public final ErrorReporter myErrorReporter;

  public ValidateTypeVisitor() {
    this.myErrorReporter = new ErrorReporter();
  }

  public ValidateTypeVisitor(ErrorReporter errorReporter) {
    this.myErrorReporter = errorReporter;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Expression expectedType) {
    return null;
  }

  private boolean typesCompatible(Expression expected, Expression actual) {
    if (expected instanceof UniverseExpression) {
      if (actual instanceof UniverseExpression) {
        Universe expectedU = ((UniverseExpression) expected).getUniverse();
        Universe actualU = ((UniverseExpression) actual).getUniverse();
        return actualU.lessOrEquals(expectedU);
      } else {
        return false;
      }
    } else if (expected instanceof PiExpression) {
      if (actual instanceof PiExpression) {
        Expression expectedCod = ((PiExpression) expected).getCodomain();
        if (expectedCod instanceof UniverseExpression) {
          Expression actualCod = ((PiExpression) actual).getCodomain();
          return typesCompatible(expectedCod, actualCod)
                  && expected.equals(Pi(((PiExpression) actual).getParameters(), expectedCod));
        }
      } else {
        return false;
      }
    }
    return expected.equals(actual);
  }

  @Override
  public Void visitApp(AppExpression expr, Expression expectedType) {
    List<? extends Expression> args = expr.getArguments();
    List<? extends EnumSet<AppExpression.Flag>> flags = expr.getFlags();
    Expression fun = expr.getFunction();
    Expression funType = fun.getType().normalize(NormalizeVisitor.Mode.NF);
    if (!(funType instanceof PiExpression)) {
      myErrorReporter.addError(expr, "Function " + fun + " doesn't have Pi-type");
    } else {
      ArrayList<DependentLink> links = new ArrayList<>();
      funType.getPiParameters(links, true, false);
      Substitution subst = new Substitution();
      if (args.size() > links.size()) {
        myErrorReporter.addError(expr, "Too few Pi abstractions");
      }
      int size = Math.min(args.size(), links.size());
      for (int i = 0; i < size; i++) {
        DependentLink param = links.get(i);
        Expression arg = args.get(i);
        Expression argType = arg.getType().normalize(NormalizeVisitor.Mode.NF);
        Expression expectedArgType = param.getType().subst(subst).normalize(NormalizeVisitor.Mode.NF);
        boolean isArgExplicit = flags.get(i).contains(AppExpression.Flag.EXPLICIT);
        if (param.isExplicit() != isArgExplicit) {
          myErrorReporter.addError(expr, i + "th argument is expected to be" + (param.isExplicit() ? "explicit" : "implicit"));
        }
        if (!typesCompatible(expectedArgType, argType)) {
          myErrorReporter.addError(arg, "Expected type: " + expectedArgType + ", actual: " + argType);
        }
        subst.add(param, arg);
      }
    }

    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Expression expectedType) {
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr, Expression expectedType) {
    visitDependentLink(expr.getParameters());
    expr.getBody().accept(this, null);
    return null;
  }

  private void visitDependentLink(DependentLink link) {
    if (link.hasNext()) {
      link.getType().accept(this, null);
      visitDependentLink(link.getNext());
    }
  }

  @Override
  public Void visitPi(PiExpression expr, Expression expectedType) {
    visitDependentLink(expr.getParameters());
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Expression expectedType) {
    visitDependentLink(expr.getParameters());
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Expression expectedType) {
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr, Expression expectedType) {
    myErrorReporter.addError(expr, expr.getError().toString());
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr, Expression expectedType) {
    SigmaExpression type = expr.getType();
    type = (SigmaExpression) type.normalize(NormalizeVisitor.Mode.NF);
    DependentLink link = type.getParameters();
    type.accept(this, expectedType);
    Substitution subst = new Substitution();
    for (Expression field : expr.getFields()) {
      field.accept(this, expectedType);
      Expression expectedFieldType = link.getType().subst(subst).normalize(NormalizeVisitor.Mode.NF);
      Expression actualFieldType = field.getType().normalize(NormalizeVisitor.Mode.NF);
      if (!actualFieldType.equals(expectedFieldType)) {
        myErrorReporter.addError(field, "Expected type: " + expectedFieldType + ", found: " + actualFieldType);
      }
      if (!link.hasNext()) {
        myErrorReporter.addError(expr, "Too few abstractions in Sigma");
      }
      subst.add(link, field);
      link = link.getNext();
    }
    return null;
  }

  @Override
  public Void visitProj(ProjExpression expr, Expression expectedType) {
    Expression tuple = expr.getExpression().normalize(NormalizeVisitor.Mode.WHNF);
    tuple.accept(this, expectedType);
    if (!(tuple instanceof TupleExpression)) {
      myErrorReporter.addError(tuple, "Tuple expected");
    } else {
      List<Expression> fields = ((TupleExpression)tuple).getFields();
      if (fields.size() <= expr.getField()) {
        myErrorReporter.addError(tuple, "Too few fields (at least " + (expr.getField() + 1) + " expected)");
      }
    }
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, Expression expectedType) {
    expr.getExpression().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitLet(LetExpression letExpression, Expression expectedType) {
    // TODO subst
    Substitution subst = new Substitution();
    for (LetClause clause : letExpression.getClauses()) {
      clause.getElimTree().accept(this, clause.getType());
    }
    return null;
  }

  @Override
  public Void visitBranch(BranchElimTreeNode branchNode, Expression expectedType) {
    Collection<ConstructorClause> clauses = branchNode.getConstructorClauses();
    for (ConstructorClause clause : clauses) {
      clause.getChild().accept(this, expectedType);
    }
    OtherwiseClause otherwiseClause = branchNode.getOtherwiseClause();
    if (otherwiseClause != null) {
      otherwiseClause.getChild().accept(this, expectedType);
    }
    return null;
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode leafNode, Expression expectedType) {
    leafNode.getExpression().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitEmpty(EmptyElimTreeNode emptyNode, Expression expectedType) {
    return null;
  }

}
