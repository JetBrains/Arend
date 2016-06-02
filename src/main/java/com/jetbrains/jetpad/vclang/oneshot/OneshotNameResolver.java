package com.jetbrains.jetpad.vclang.oneshot;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.parser.BinOpParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ResolveListener;

import java.util.ArrayList;
import java.util.List;

public class OneshotNameResolver {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final ResolveListener myResolveListener;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final OneshotSourceInfoProvider mySourceInfoProvider = new OneshotSourceInfoProvider();
  private ModuleID myCurrentModuleID;

  public OneshotNameResolver(ErrorReporter errorReporter, NameResolver nameResolver, ResolveListener listener, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider) {
    myErrorReporter = errorReporter;
    myNameResolver = nameResolver;
    myResolveListener = listener;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
  }

  public void visitModule(ModuleID moduleID, Abstract.ClassDefinition module) {
    myCurrentModuleID = moduleID;
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(new SubScope(Preprelude.PRE_PRELUDE, Prelude.PRELUDE), null);
    visitor.visitClass(module, null);
  }

  public SourceInfoProvider getSourceInfoProvider() {
    return mySourceInfoProvider;
  }


  public enum Flag { MUST_BE_STATIC, MUST_BE_DYNAMIC }

  public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Boolean, Void> {
    private final Scope myParentScope;
    private final FullName myFullName;
    private List<String> myContext;

    public DefinitionResolveNameVisitor(Scope parentScope, FullName fullName) {
      this(parentScope, fullName, new ArrayList<String>());
    }

    public DefinitionResolveNameVisitor(Scope parentScope, FullName fullName, List<String> context) {
      myParentScope = parentScope;
      myFullName = fullName;
      myContext = context;
    }

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, Boolean isStatic) {
      if (myResolveListener == null) {
        return null;
      }
      final FunctionScope scope = new FunctionScope(myParentScope, myStaticNsProvider.forDefinition(def));

      ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, myContext);
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        for (Abstract.Argument argument : def.getArguments()) {
          if (argument instanceof Abstract.TypeArgument) {
            ((Abstract.TypeArgument) argument).getType().accept(exprVisitor, null);
          }
          if (argument instanceof Abstract.TelescopeArgument) {
            myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
          } else
          if (argument instanceof Abstract.NameArgument) {
            myContext.add(((Abstract.NameArgument) argument).getName());
          }
        }

        Abstract.Expression resultType = def.getResultType();
        if (resultType != null) {
          resultType.accept(exprVisitor, null);
        }

        Abstract.Expression term = def.getTerm();
        if (term != null) {
          term.accept(exprVisitor, null);
        }
      }

      StatementResolveNameVisitor statementVisitor = new StatementResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, scope, myFullName, myContext);
      for (Abstract.Statement statement : def.getStatements()) {
        statement.accept(statementVisitor, null);
      }

      return null;
    }

    @Override
    public Void visitAbstract(Abstract.AbstractDefinition def, Boolean isStatic) {
      if (myResolveListener == null) {
        return null;
      }

      ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myParentScope, myContext);
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        for (Abstract.Argument argument : def.getArguments()) {
          if (argument instanceof Abstract.TypeArgument) {
            ((Abstract.TypeArgument) argument).getType().accept(exprVisitor, null);
          }
          if (argument instanceof Abstract.TelescopeArgument) {
            myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
          } else
          if (argument instanceof Abstract.NameArgument) {
            myContext.add(((Abstract.NameArgument) argument).getName());
          }
        }

        Abstract.Expression resultType = def.getResultType();
        if (resultType != null) {
          resultType.accept(exprVisitor, null);
        }
      }
      return null;
    }

    @Override
    public Void visitData(Abstract.DataDefinition def, Boolean isStatic) {
      if (myResolveListener == null) {
        return null;
      }

      Scope scope = new DataScope(myParentScope, myStaticNsProvider.forDefinition(def));

      ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, myContext);
      try (Utils.CompleteContextSaver<String> saver = new Utils.CompleteContextSaver<>(myContext)) {
        for (Abstract.TypeArgument parameter : def.getParameters()) {
          parameter.getType().accept(exprVisitor, null);
          if (parameter instanceof Abstract.TelescopeArgument) {
            myContext.addAll(((Abstract.TelescopeArgument) parameter).getNames());
          }
        }

        for (Abstract.Constructor constructor : def.getConstructors()) {
          if (constructor.getPatterns() == null) {
            visitConstructor(constructor, null);
          } else {
            myContext = saver.getOldContext();
            visitConstructor(constructor, null);
            myContext = saver.getCurrentContext();
          }
        }

        if (def.getConditions() != null) {
          for (Abstract.Condition cond : def.getConditions()) {
            try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
              for (Abstract.PatternArgument patternArgument : cond.getPatterns()) {
                if (exprVisitor.visitPattern(patternArgument.getPattern())) {
                  myResolveListener.replaceWithConstructor(patternArgument);
                }
              }
              cond.getTerm().accept(exprVisitor, null);
            }
          }
        }
      }

      return null;
    }

    @Override
    public Void visitConstructor(Abstract.Constructor def, Boolean isStatic) {
      if (myResolveListener == null) {
        return null;
      }

      ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myParentScope, myContext);
      try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
        if (def.getPatterns() != null) {
          for (Abstract.PatternArgument patternArg : def.getPatterns()) {
            if (exprVisitor.visitPattern(patternArg.getPattern())) {
              myResolveListener.replaceWithConstructor(patternArg);
            }
          }
        }

        for (Abstract.TypeArgument argument : def.getArguments()) {
          argument.getType().accept(exprVisitor, null);
          if (argument instanceof Abstract.TelescopeArgument) {
            myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
          }
        }
      }

      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, Boolean isStatic) {
      Scope staticScope  = new StaticClassScope(myParentScope, myStaticNsProvider.forDefinition(def));
      StatementResolveNameVisitor stVisitor = new StatementResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, staticScope, myFullName, myContext);
      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement && Abstract.DefineStatement.StaticMod.DYNAMIC.equals(((Abstract.DefineStatement) statement).getStaticMod())) continue;  // FIXME[where]
        statement.accept(stVisitor, null);
      }

      Scope dynamicScope = new DynamicClassScope(myParentScope, myStaticNsProvider.forDefinition(def), myDynamicNsProvider.forClass(def));
      StatementResolveNameVisitor dyVisitor = new StatementResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, dynamicScope, myFullName, myContext);
      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement && Abstract.DefineStatement.StaticMod.STATIC.equals(((Abstract.DefineStatement) statement).getStaticMod())) continue;  //  FIXME[where]
        statement.accept(dyVisitor, null);
      }
      return null;
    }
  }

  public class ExpressionResolveNameVisitor implements AbstractExpressionVisitor<Void, Void> {
    private final Scope myParentScope;
    private final List<String> myContext;

    public ExpressionResolveNameVisitor(Scope parentScope, List<String> context) {
      assert myResolveListener != null;
      myParentScope = parentScope;
      myContext = context;
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
        if (expression != null || !myContext.contains(expr.getName())) {
          Referable ref = myNameResolver.resolveDefCall(myParentScope, expr);
          if (ref != null) {
            myResolveListener.nameResolved(expr, ref);
          } else if (expression == null) {
            // TODO: uncomment when patterns add to context
            //myErrorReporter.report(new NotInScopeError(expr, expr.getName()));
          }
        }
      }
      return null;
    }

    @Override
    public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
      if (expr.getModule() == null) {
        Referable ref = myNameResolver.resolveModuleCall(myParentScope, expr);
        if (ref != null) {
          myResolveListener.moduleResolved(expr, ref);
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
          Referable ref = myParentScope.resolveName(name);
          if (ref != null) {
            parser.pushOnStack(stack, expression, ref, ref.getPrecedence(), elem.binOp);
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
        if (name == null) return false;
        Referable ref = myParentScope.resolveName(name);
        if (ref != null && (ref instanceof Constructor || ref instanceof Abstract.Constructor)) {
          return true;
        } else {
          myContext.add(name);
          return false;
        }
      } else if (pattern instanceof Abstract.ConstructorPattern) {
        for (Abstract.PatternArgument patternArg : ((Abstract.ConstructorPattern) pattern).getArguments()) {
          if (visitPattern(patternArg.getPattern())) {
            myResolveListener.replaceWithConstructor(patternArg);
          }
        }
        return false;
      } else if (pattern instanceof Abstract.AnyConstructorPattern) {
        return false;
      } else {
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

  public class StatementResolveNameVisitor implements AbstractStatementVisitor<Flag, Object> {
    private final StaticNamespaceProvider myStaticNsProvider;
    private final DynamicNamespaceProvider myDynamicNsProvider;
    private final List<String> myContext;
    private final FullName myFullName;
    private Scope myScope;

    public StatementResolveNameVisitor(StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, Scope parentScope, FullName fullName, List<String> context) {
      myStaticNsProvider = staticNsProvider;
      myDynamicNsProvider = dynamicNsProvider;
      myScope = parentScope;
      myFullName = fullName;
      myContext = context;
    }

    @Override
    public Void visitDefine(Abstract.DefineStatement stat, Flag flag) {
      if (stat.getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC && flag == Flag.MUST_BE_STATIC) {
        myErrorReporter.report(new GeneralError("Non-static definition in a static context", stat));
        return null;
      } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && flag == Flag.MUST_BE_DYNAMIC) {
        myErrorReporter.report(new TypeCheckingError("Static definitions are not allowed in this context", stat));
        return null;
      } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && stat.getDefinition() instanceof Abstract.AbstractDefinition) {
        myErrorReporter.report(new TypeCheckingError("Abstract definitions cannot be static", stat));
        return null;
      }
      DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(myScope, myFullName, myContext);
      stat.getDefinition().accept(visitor, stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC);
      mySourceInfoProvider.registerDefinition(stat.getDefinition(), new FullName(myFullName, stat.getDefinition().getName()), myCurrentModuleID);
      return null;
    }

    @Override
    public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Flag flag) {
      if (flag == Flag.MUST_BE_DYNAMIC) {
        myErrorReporter.report(new TypeCheckingError("Namespace commands are not allowed in this context", stat));
        return null;
      }

      if (Abstract.NamespaceCommandStatement.Kind.EXPORT.equals(stat.getKind())) {
        throw new UnsupportedOperationException();
      }

      ModuleNamespace moduleNamespace = myNameResolver.resolveModuleNamespace(stat.getModulePath());
      if (moduleNamespace == null || moduleNamespace.getRegisteredClass() == null) {
        myErrorReporter.report(new NotInScopeError(null, new ModulePath(stat.getModulePath()).toString()));  // FIXME: null? really?
        return null;
      }

      Referable ref = stat.getPath().isEmpty() ? moduleNamespace.getRegisteredClass() : myNameResolver.resolveDefinition(myNameResolver.staticNamespaceFor(moduleNamespace.getRegisteredClass()), stat.getPath());
      if (ref == null) {
        myErrorReporter.report(new NotInScopeError(null, stat.getPath().toString()));  // FIXME: report proper error
        return null;
      }

      if (stat.getKind().equals(Abstract.NamespaceCommandStatement.Kind.OPEN)) {
        myScope = new MergeScope(myScope, myNameResolver.staticNamespaceFor(ref));
      }

      return null;
    }

    @Override
    public Object visitDefaultStaticCommand(Abstract.DefaultStaticStatement stat, Flag params) {
      return null;
    }

    /*
    private void processNamespaceCommand(NamespaceMember member, boolean export, boolean remove, Abstract.SourceNode sourceNode) {
      boolean ok;
      if (export) {
        ok = myNamespace.addMember(member) == null;
      } else
      if (remove) {
        ok = myPrivateNameResolver.locateName(member.namespace.getName()) != null;
        myPrivateNameResolver.remove(member.namespace.getName());
      } else {
        ok = myPrivateNameResolver.locateName(member.namespace.getName()) == null;
        myPrivateNameResolver.add(member);
      }

      if (!ok) {
        GeneralError error = new NameDefinedError(!remove, sourceNode, member.namespace.getName(), null);
        error.setLevel(GeneralError.Level.WARNING);
        myErrorReporter.report(error);
      }
    }
    */
  }
}
