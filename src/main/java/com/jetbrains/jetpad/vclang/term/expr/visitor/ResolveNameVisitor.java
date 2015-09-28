package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.parser.BinOpParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.statement.visitor.StatementResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NamespaceNameResolver;

import java.util.ArrayList;
import java.util.List;

public class ResolveNameVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final List<String> myContext;
  private final boolean myStatic;

  public ResolveNameVisitor(ErrorReporter errorReporter, NameResolver nameResolver, List<String> context, boolean isStatic) {
    myErrorReporter = errorReporter;
    CompositeNameResolver compositeNameResolver = new CompositeNameResolver();
    compositeNameResolver.pushNameResolver(nameResolver);
    compositeNameResolver.pushNameResolver(new NamespaceNameResolver(Prelude.PRELUDE, null));
    myNameResolver = compositeNameResolver;
    myContext = context;
    myStatic = isStatic;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Void params) {
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, null);
    }

    if (expr.getDefinitionPair() != null) {
      // TODO
    } else {
      if (expr.getExpression() != null) {
        if (expr.getExpression() instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr.getExpression()).getExpression() == null) {
          if (((Abstract.DefCallExpression) expr.getExpression()).getDefinitionPair() == null) {
            return null;
          }
          Namespace namespace = ((Abstract.DefCallExpression) expr.getExpression()).getDefinitionPair().namespace;
          DefinitionPair member = myNameResolver.getMember(namespace, expr.getName().name);
          if (member != null) {
            expr.replaceWithDefCall(member);
          }
        }
      } else {
        visitVar(expr, null);
      }
    }
    return null;
  }

  @Override
  public Void visitIndex(Abstract.IndexExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(this, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else if (argument instanceof Abstract.NameArgument) {
          myContext.add(((Abstract.NameArgument) argument).getName());
        }
      }

      expr.getBody().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(this, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else if (argument instanceof Abstract.NameArgument) {
          myContext.add(((Abstract.NameArgument) argument).getName());
        }
      }

      expr.getCodomain().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitVar(Abstract.VarExpression expr, Void params) {
    if (expr.getName().fixity == Abstract.Definition.Fixity.INFIX || !myContext.contains(expr.getName().name)) {
      DefinitionPair member = NameResolver.Helper.locateName(myNameResolver, expr.getName().name, expr, myStatic, myErrorReporter);
      if (member != null) {
        expr.replaceWithDefCall(member);
      }
    }
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Void params) {
    if (expr.getExpr() != null)
      expr.getExpr().accept(this, null);
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Void params) {
    for (Abstract.Expression expression : expr.getFields()) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(this, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else if (argument instanceof Abstract.NameArgument) {
          myContext.add(((Abstract.NameArgument) argument).getName());
        }
      }
    }
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Void params) {
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void params) {
    if (expr.getSequence().isEmpty()) {
      expr.getLeft().accept(this, null);
      expr.replace(expr.getLeft());
    } else {
      BinOpParser parser = new BinOpParser(myErrorReporter, expr);
      List<BinOpParser.StackElem> stack = new ArrayList<>(expr.getSequence().size());
      Abstract.Expression expression = expr.getLeft();
      expression.accept(this, null);
      for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
        DefinitionPair member = NameResolver.Helper.locateName(myNameResolver, elem.binOp.getName().name, elem.binOp, myStatic, myErrorReporter);
        if (member != null) {
          parser.pushOnStack(stack, expression, member, elem.binOp);
          expression = elem.argument;
          expression.accept(this, null);
        }
      }
      expr.replace(parser.rollUpStack(stack, expression));
    }
    return null;
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Void params) {
    visitElimCase(expr);
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void params) {
    visitElimCase(expr);
    return null;
  }

  private void visitElimCase(Abstract.ElimCaseExpression expr) {
    for (Abstract.Expression expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Clause clause : expr.getClauses()) {
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          visitPattern(clause, i);
        }

        if (clause.getExpression() != null)
          clause.getExpression().accept(this, null);
      }
    }
  }

  public void visitPattern(Abstract.PatternContainer con, int index) {
    Abstract.Pattern pattern = con.getPatterns().get(index);
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      if (name == null)
        return;
      DefinitionPair member = myNameResolver.locateName(name, myStatic);
      if (member != null && (member.definition instanceof Constructor || member.abstractDefinition instanceof Abstract.Constructor)) {
        List<Abstract.Argument> args = new ArrayList<>();
        args.addAll(member.definition != null ? ((Constructor) member.definition).getArguments() : ((Abstract.Constructor) member.abstractDefinition).getArguments());
        boolean hasExplicit = false;
        for (Abstract.Argument arg : args) {
          if (arg.getExplicit())
            hasExplicit = true;
        }
        if (!hasExplicit) {
          con.replacePatternWithConstructor(index);
          return;
        }
      }
      myContext.add(name);
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      List<? extends Abstract.Pattern> patterns = ((Abstract.ConstructorPattern) pattern).getPatterns();
      for (int i = 0; i < patterns.size(); ++i) {
        visitPattern((Abstract.ConstructorPattern) pattern, i);
      }
    } else if (!(pattern instanceof Abstract.AnyConstructorPattern)) {
      throw new IllegalStateException();
    }
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Void params) {
    expr.getBaseClassExpression().accept(this, null);
    CompositeNameResolver nameResolver = new CompositeNameResolver();
    nameResolver.pushNameResolver(myNameResolver);
    StatementResolveNameVisitor visitor = new StatementResolveNameVisitor(myErrorReporter, null, new Namespace("<anonymous>"), nameResolver, myContext);
    for (Abstract.Statement statement : expr.getStatements()) {
      statement.accept(visitor, null);
    }
    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Abstract.LetExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.LetClause clause : expr.getClauses()) {
        for (Abstract.Argument argument : clause.getArguments()) {
          if (argument instanceof Abstract.TypeArgument) {
            ((Abstract.TypeArgument) argument).getType().accept(this, null);
          }
          if (argument instanceof Abstract.TelescopeArgument) {
            myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
          } else
          if (argument instanceof Abstract.NameArgument) {
            myContext.add(((Abstract.NameArgument) argument).getName());
          }
        }

        if (clause.getResultType() != null) {
          clause.getResultType().accept(this, null);
        }
        clause.getTerm().accept(this, null);
        myContext.add(clause.getName().name);
      }

      expr.getExpression().accept(this, null);
    }
    return null;
  }
}
