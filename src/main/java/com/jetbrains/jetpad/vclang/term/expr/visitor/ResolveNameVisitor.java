package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.parser.BinOpParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NamespaceNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ResolveListener;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.module.ModuleResolver;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.naming.NamespaceMember.toNamespaceMember;

public class ResolveNameVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final ModuleResolver myModuleResolver;
  private final List<String> myContext;
  private final ResolveListener myResolveListener;

  public ResolveNameVisitor(ErrorReporter errorReporter, NameResolver nameResolver, ModuleResolver moduleResolver, List<String> context, ResolveListener resolveListener) {
    myModuleResolver = moduleResolver;
    assert resolveListener != null;
    myErrorReporter = errorReporter;
    CompositeNameResolver compositeNameResolver = new CompositeNameResolver();
    compositeNameResolver.pushNameResolver(nameResolver);
    compositeNameResolver.pushNameResolver(new NamespaceNameResolver(Prelude.PRELUDE));
    compositeNameResolver.pushNameResolver(new NamespaceNameResolver(Preprelude.PRE_PRELUDE));
    myNameResolver = compositeNameResolver;
    myContext = context;
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Void params) {
    Abstract.Expression expression = expr.getExpression();
    if (expression != null) {
      expression.accept(this, null);
    }

    if (expr.getReferent() == null) {
      if (expression != null) {
        if (expression instanceof Abstract.DefCallExpression || expression instanceof Abstract.ModuleCallExpression) {
          Referable parent;
          if (expression instanceof Abstract.DefCallExpression) {
            parent = ((Abstract.DefCallExpression) expression).getReferent();
          } else {
            parent = ((Abstract.ModuleCallExpression) expression).getModule();
          }

          if (parent == null) {
            return null;
          }

          Namespace parentNamespace = toNamespaceMember(parent).namespace;
          if (parentNamespace == null) {
            return null;
          }
          
          NamespaceMember member = myNameResolver.getMember(parentNamespace, expr.getName());
          if (member != null) {
            myResolveListener.nameResolved(expr, member.getResolvedDefinition());
          }
        }
      } else {
        String name = expr.getName();
        if (!myContext.contains(name)) {
          NamespaceMember member = NameResolver.Helper.locateName(myNameResolver, name, false);
          if (member != null) {
            myResolveListener.nameResolved(expr, member.getResolvedDefinition());
          }
        }
      }
    }
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    if (expr.getModule() == null) {
      NamespaceMember member = myModuleResolver.locateModule(new ModulePath(expr.getPath()));
      if (member != null) {
        myResolveListener.moduleResolved(expr, member.getResolvedDefinition());
      }
    }
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
  public Void visitPolyUniverse(Abstract.PolyUniverseExpression expr, Void params) {
    expr.getPLevel().accept(this, null);
    return expr.getHLevel().accept(this, null);
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Void params) {
    Abstract.Expression expression = expr.getExpr();
    if (expression != null) {
      expression.accept(this, null);
    }
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
      Abstract.Expression left = expr.getLeft();
      left.accept(this, null);
      myResolveListener.replaceBinOp(expr, left);
    } else {
      BinOpParser parser = new BinOpParser(myErrorReporter, expr, myResolveListener);
      List<Abstract.BinOpSequenceElem> sequence = expr.getSequence();
      List<BinOpParser.StackElem> stack = new ArrayList<>(sequence.size());
      Abstract.Expression expression = expr.getLeft();
      expression.accept(this, null);
      NotInScopeError error = null;
      for (Abstract.BinOpSequenceElem elem : sequence) {
        String name = elem.binOp.getName();
        NamespaceMember member = NameResolver.Helper.locateName(myNameResolver, name, true);
        if (member != null) {
          parser.pushOnStack(stack, expression, member.getResolvedDefinition(), member.getResolvedDefinition().getPrecedence(), elem.binOp);
          expression = elem.argument;
          expression.accept(this, null);
        } else {
          error = new NotInScopeError(elem.binOp, name);
          myErrorReporter.report(error);
        }
      }
      if (error == null) {
        myResolveListener.replaceBinOp(expr, parser.rollUpStack(stack, expression));
      } else {
        myResolveListener.replaceBinOp(expr, myResolveListener.makeError(expr, error.getCause()));
      }
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
          if (visitPattern(clause.getPatterns().get(i))) {
            myResolveListener.replaceWithConstructor(clause, i);
          }
        }

        if (clause.getExpression() != null)
          clause.getExpression().accept(this, null);
      }
    }
  }

  public boolean visitPattern(Abstract.Pattern pattern) {
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      if (name == null)
        return false;
      NamespaceMember namespaceMember = myNameResolver.locateName(name);
      if (namespaceMember != null && (namespaceMember.definition instanceof Constructor || namespaceMember.abstractDefinition instanceof Abstract.Constructor)) {
        boolean hasExplicit = false;
        if (namespaceMember.definition instanceof Constructor && !namespaceMember.definition.hasErrors()) {
          for (DependentLink link = ((Constructor) namespaceMember.definition).getParameters(); link.hasNext(); link = link.getNext()) {
            if (link.isExplicit()) {
              hasExplicit = true;
              break;
            }
          }
        } else {
          if (namespaceMember.abstractDefinition instanceof Abstract.Constructor) {
            for (Abstract.TypeArgument argument : ((Abstract.Constructor) namespaceMember.abstractDefinition).getArguments()) {
              if (argument.getExplicit()) {
                hasExplicit = true;
                break;
              }
            }
          }
        }
        if (!hasExplicit) {
          return true;
        }
      }
      myContext.add(name);
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      for (Abstract.PatternArgument patternArg : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        if (visitPattern(patternArg.getPattern())) {
          myResolveListener.replaceWithConstructor(patternArg);
        }
      }
    } else if (!(pattern instanceof Abstract.AnyConstructorPattern)) {
      throw new IllegalStateException();
    }
    return false;
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Void params) {
    expr.getBaseClassExpression().accept(this, null);
    for (Abstract.ImplementStatement statement : expr.getStatements()) {
      statement.getExpression().accept(this, null);
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
        myContext.add(clause.getName());
      }

      expr.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitNumericLiteral(Abstract.NumericLiteral expr, Void params) {
    return null;
  }
}
