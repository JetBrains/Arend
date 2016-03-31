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

  private void checkType(Expression expr, Expression expectedType) {
    if (expectedType == null) {
      myErrorReporter.addError(expr, "Expected type is null");
      return;
    }
    expectedType = expectedType.normalize(NormalizeVisitor.Mode.NF);
    Expression actualType = expr.getType().normalize(NormalizeVisitor.Mode.NF);
    if (!typesCompatible(expectedType, actualType)) {
      myErrorReporter.addError(expr, "Expected type: " + expectedType + ", actual: " + actualType);
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Expression expectedType) {
    checkType(expr, expectedType);
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

  private void visitApp(Expression funType, List<? extends Expression> args,
                        List<? extends EnumSet<AppExpression.Flag>> flags, Expression expectedType) {
    if (args.isEmpty()) {
      if (!typesCompatible(expectedType, funType)) {
        myErrorReporter.addError(funType, "Result expected type: " + expectedType + ", actual: " + funType);
      }
      return;
    }
    if (!(funType instanceof PiExpression)) {
      myErrorReporter.addError(funType, "Function type " + funType + " is not Pi");
    } else {
      ArrayList<DependentLink> links = new ArrayList<>();
      Expression cod = funType.getPiParameters(links, true, false);
      Substitution subst = new Substitution();
      int size = Math.min(args.size(), links.size());
      for (int i = 0; i < size; i++) {
        DependentLink param = links.get(i);
        Expression arg = args.get(i);
        Expression argType = arg.getType().normalize(NormalizeVisitor.Mode.NF);
        Expression expectedArgType = param.getType().subst(subst).normalize(NormalizeVisitor.Mode.NF);
        boolean isArgExplicit = flags.get(i).contains(AppExpression.Flag.EXPLICIT);
        if (param.isExplicit() != isArgExplicit) {
          myErrorReporter.addError(funType, i + "th argument is expected to be" + (param.isExplicit() ? "explicit" : "implicit"));
        }
        if (!typesCompatible(expectedArgType, argType)) {
          myErrorReporter.addError(arg, "Expected type: " + expectedArgType + ", actual: " + argType);
        }
        subst.add(param, arg);
      }
      if (args.size() > links.size()) {
        cod = cod.subst(subst).normalize(NormalizeVisitor.Mode.WHNF);
        visitApp(cod, args.subList(size, args.size()), flags.subList(size, flags.size()), expectedType);
      }
    }
  }

  @Override
  public Void visitApp(AppExpression expr, Expression expectedType) {
    checkType(expr, expectedType);
//    List<? extends Expression> args = expr.normalize(NormalizeVisitor.Mode.WHNF).getArguments();
    List<? extends Expression> args = expr.getArguments();
    List<? extends EnumSet<AppExpression.Flag>> flags = expr.getFlags();
    Expression fun = expr.getFunction();
    Expression funType = fun.getType().normalize(NormalizeVisitor.Mode.NF);
    visitApp(funType, args, flags, expectedType);

    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Expression expectedType) {
    checkType(expr, expectedType);
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr, Expression expectedType) {
    checkType(expr, expectedType);
    visitDependentLink(expr.getParameters());
    Expression normType = expectedType.normalize(NormalizeVisitor.Mode.WHNF);
    if (!(normType instanceof PiExpression)) {
      myErrorReporter.addError(expr, "Expected type " + normType + " is expected to be Pi-type");
    } else {
      ArrayList<DependentLink> params = new ArrayList<>();
      expr.getLamParameters(params);
      ArrayList<DependentLink> piParams = new ArrayList<>();
      normType.getPiParameters(piParams, true, false);
      if (params.size() > piParams.size()) {
        myErrorReporter.addError(expr, "Type " + expectedType + " has less Pi-abstractions than term's Lam abstractions");
      }
      int size = Math.min(params.size(), piParams.size());
      Substitution subst = new Substitution();
      for (int i = 0; i < size; i++) {
        subst.add(piParams.get(i), Reference(params.get(i)));
      }
      Expression expectedBodyType = ((PiExpression) normType).getCodomain().subst(subst);
      expr.getBody().accept(this, expectedBodyType);
    }
    return null;
  }

  private void visitDependentLink(DependentLink link) {
    if (link.hasNext()) {
      // TODO use proper Universe
      link.getType().accept(this, Universe(0));
      visitDependentLink(link.getNext());
    }
  }

  @Override
  public Void visitPi(PiExpression expr, Expression expectedType) {
    checkType(expr, expectedType);
    visitDependentLink(expr.getParameters());
    expr.getCodomain().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Expression expectedType) {
    checkType(expr, expectedType);
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
    checkType(expr, expectedType);
    SigmaExpression type = expr.getType();
    type = (SigmaExpression) type.normalize(NormalizeVisitor.Mode.NF);
    DependentLink link = type.getParameters();
    Substitution subst = new Substitution();
    for (Expression field : expr.getFields()) {
      Expression expectedFieldType = link.getType().subst(subst).normalize(NormalizeVisitor.Mode.NF);
      Expression actualFieldType = field.getType().normalize(NormalizeVisitor.Mode.NF);
      field.accept(this, expectedFieldType);
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
    Expression tuple = expr.getExpression().normalize(NormalizeVisitor.Mode.NF);
    tuple.accept(this, tuple.getType());
    if (tuple instanceof TupleExpression) {
      List<Expression> fields = ((TupleExpression)tuple).getFields();
      if (fields.size() <= expr.getField()) {
        myErrorReporter.addError(tuple, "Too few fields (at least " + (expr.getField() + 1) + " expected)");
      } else {
        Expression field = fields.get(expr.getField());
        checkType(field, expectedType);
      }
    } else if (!(tuple instanceof AppExpression) && !(tuple instanceof ReferenceExpression)) {
      myErrorReporter.addError(tuple, "Tuple or App expected");
    }
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, Expression expectedType) {
    checkType(expr, expectedType);
    expr.getExpression().accept(this, expectedType.getType());
    return null;
  }

  @Override
  public Void visitLet(LetExpression letExpression, Expression expectedType) {
    checkType(letExpression, expectedType);
    // TODO subst
    Substitution subst = new Substitution();
    for (LetClause clause : letExpression.getClauses()) {
      clause.getElimTree().accept(this, clause.getResultType());
    }
    letExpression.getExpression().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitBranch(BranchElimTreeNode branchNode, Expression expectedType) {
    Collection<ConstructorClause> clauses = branchNode.getConstructorClauses();
    for (ConstructorClause clause : clauses) {
      Substitution subst = clause.getSubst();
      Expression expectedTypeHere = expectedType.subst(subst).normalize(NormalizeVisitor.Mode.NF);
      clause.getChild().subst(subst).accept(this, expectedTypeHere);
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
