package org.arend.module.serialization;

import com.google.protobuf.ByteString;
import org.arend.core.constructor.*;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.ParamLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.let.LetClausePattern;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.prelude.Prelude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ExpressionSerialization implements ExpressionVisitor<Void, ExpressionProtos.Expression> {
  private final CallTargetIndexProvider myCallTargetIndexProvider;
  private final Map<Binding, Integer> myBindingsMap = new HashMap<>();
  private int myIndex;

  ExpressionSerialization(CallTargetIndexProvider callTargetIndexProvider) {
    myCallTargetIndexProvider = callTargetIndexProvider;
  }

  // Bindings

  @SuppressWarnings("UnusedReturnValue")
  private int registerBinding(Binding binding) {
    int index = myIndex++;
    myBindingsMap.put(binding, index);
    return index;
  }

  private ExpressionProtos.Type writeType(Type type) {
    ExpressionProtos.Type.Builder builder = ExpressionProtos.Type.newBuilder();
    builder.setExpr(writeExpr(type.getExpr()));
    if (type instanceof TypeExpression) {
      builder.setSort(writeSort(type.getSortOfType()));
    }
    return builder.build();
  }

  int writeBindingRef(Binding binding) {
    if (binding == null) {
      return 0;
    } else {
      Integer ref = myBindingsMap.get(binding);
      if (ref == null) {
        throw new IllegalStateException();
      }
      return ref + 1;  // zero is reserved for null
    }
  }

  // Sorts and levels

  private LevelProtos.Level writeLevel(Level level) {
    // Level.INFINITY should be read with great care
    LevelProtos.Level.Builder builder = LevelProtos.Level.newBuilder();
    LevelVariable var = level.getVar();
    if (var == null) {
      builder.setVariable(-2);
    } else if (var == LevelVariable.PVAR || var == LevelVariable.HVAR) {
      builder.setVariable(-1);
    } else if (var instanceof ParamLevelVariable) {
      builder.setVariable(((ParamLevelVariable) var).getSize());
    } else {
      throw new IllegalStateException();
    }
    builder.setConstant(level.getConstant());
    builder.setMaxConstant(level.getMaxConstant());
    return builder.build();
  }

  LevelProtos.Sort writeSort(Sort sort) {
    LevelProtos.Sort.Builder builder = LevelProtos.Sort.newBuilder();
    builder.setPLevel(writeLevel(sort.getPLevel()));
    builder.setHLevel(writeLevel(sort.getHLevel()));
    return builder.build();
  }

  LevelProtos.Levels writeLevels(Levels levels, Definition def) {
    LevelProtos.Levels.Builder builder = LevelProtos.Levels.newBuilder();
    if (levels instanceof LevelPair) {
      builder.addPLevel(writeLevel(((LevelPair) levels).get(LevelVariable.PVAR)));
      builder.addHLevel(writeLevel(((LevelPair) levels).get(LevelVariable.HVAR)));
      builder.setIsStd(true);
    } else {
      List<? extends Level> list = levels.toList();
      for (int i = 0; i < list.size(); i++) {
        Level level = list.get(i);
        if (i < def.getNumberOfPLevelParameters()) {
          builder.addPLevel(writeLevel(level));
        } else {
          builder.addHLevel(writeLevel(level));
        }
      }
      builder.setIsStd(false);
    }
    return builder.build();
  }


  // Parameters

  List<ExpressionProtos.Telescope> writeParameters(DependentLink link) {
    List<ExpressionProtos.Telescope> out = new ArrayList<>();
    for (; link.hasNext(); link = link.getNext()) {
      out.add(writeSingleParameter(link));
      link = link.getNextTyped(null);
    }
    return out;
  }

  private ExpressionProtos.Telescope writeSingleParameter(DependentLink link) {
    ExpressionProtos.Telescope.Builder tBuilder = ExpressionProtos.Telescope.newBuilder();
    List<String> names = new ArrayList<>();
    TypedDependentLink typed = link.getNextTyped(names);
    List<String> fixedNames = new ArrayList<>(names.size());
    for (String name : names) {
      if (name != null && name.isEmpty()) {
        throw new IllegalArgumentException();
      }
      fixedNames.add(name == null ? "" : name);
    }
    tBuilder.addAllName(fixedNames);
    tBuilder.setIsNotExplicit(!typed.isExplicit());
    tBuilder.setIsHidden(typed.isHidden());
    tBuilder.setType(writeType(typed.getType()));
    for (; link != typed; link = link.getNext()) {
      registerBinding(link);
    }
    registerBinding(typed);
    return tBuilder.build();
  }

  ExpressionProtos.SingleParameter writeParameter(DependentLink link) {
    ExpressionProtos.SingleParameter.Builder builder = ExpressionProtos.SingleParameter.newBuilder();
    if (link.getName() != null) {
      builder.setName(link.getName());
    }
    builder.setIsNotExplicit(!link.isExplicit());
    if (link instanceof TypedDependentLink) {
      builder.setType(writeType(link.getType()));
    }
    builder.setIsHidden(link.isHidden());
    registerBinding(link);
    return builder.build();
  }

  private ExpressionProtos.TypedBinding writeBinding(Binding binding) {
    ExpressionProtos.TypedBinding.Builder builder = ExpressionProtos.TypedBinding.newBuilder();
    builder.setName(binding.getName());
    builder.setType(writeExpr(binding.getTypeExpr()));
    registerBinding(binding);
    return builder.build();
  }

  // Types, Expressions and ElimTrees

  ExpressionProtos.Expression.Abs writeAbsExpr(AbsExpression expr) {
    ExpressionProtos.Expression.Abs.Builder builder = ExpressionProtos.Expression.Abs.newBuilder();
    if (expr.getBinding() != null) {
      builder.setBinding(writeBinding(expr.getBinding()));
    }
    builder.setExpression(writeExpr(expr.getExpression()));
    return builder.build();
  }

  ExpressionProtos.Expression writeExpr(Expression expr) {
    return expr.accept(this, null);
  }

  ExpressionProtos.Pattern writePattern(Pattern pattern) {
    ExpressionProtos.Pattern.Builder builder = ExpressionProtos.Pattern.newBuilder();
    if (pattern instanceof BindingPattern) {
      builder.setBinding(ExpressionProtos.Pattern.Binding.newBuilder()
        .setVar(writeParameter(((BindingPattern) pattern).getBinding())));
    } else if (pattern instanceof EmptyPattern) {
      builder.setEmpty(ExpressionProtos.Pattern.Empty.newBuilder()
        .setVar(writeParameter(pattern.getFirstBinding())));
    } else if (pattern instanceof ConstructorExpressionPattern) {
      ExpressionProtos.Pattern.ExpressionConstructor.Builder pBuilder = ExpressionProtos.Pattern.ExpressionConstructor.newBuilder();
      pBuilder.setExpression(writeExpr(((ConstructorExpressionPattern) pattern).getDataExpression()));
      for (ExpressionPattern subPattern : ((ConstructorExpressionPattern) pattern).getSubPatterns()) {
        pBuilder.addPattern(writePattern(subPattern));
      }
      builder.setExpressionConstructor(pBuilder.build());
    } else if (pattern instanceof ConstructorPattern) {
      ExpressionProtos.Pattern.Constructor.Builder pBuilder = ExpressionProtos.Pattern.Constructor.newBuilder();
      pBuilder.setDefinition(pattern.getDefinition() == null ? 0 : myCallTargetIndexProvider.getDefIndex(pattern.getDefinition()));
      for (Pattern subPattern : pattern.getSubPatterns()) {
        pBuilder.addPattern(writePattern(subPattern));
      }
      builder.setConstructor(pBuilder.build());
    } else {
      throw new IllegalArgumentException();
    }
    return builder.build();
  }

  private ExpressionProtos.ElimClause writeElimClause(ElimClause<Pattern> elimClause) {
    ExpressionProtos.ElimClause.Builder builder = ExpressionProtos.ElimClause.newBuilder();
    for (Pattern pattern : elimClause.getPatterns()) {
      builder.addPattern(writePattern(pattern));
    }
    if (elimClause.getExpression() != null) {
      builder.setExpression(writeExpr(elimClause.getExpression()));
    }
    return builder.build();
  }

  private ExpressionProtos.ElimTree writeElimTree(ElimTree elimTree) {
    ExpressionProtos.ElimTree.Builder builder = ExpressionProtos.ElimTree.newBuilder();
    builder.setSkip(elimTree.getSkip());
    if (elimTree instanceof LeafElimTree) {
      ExpressionProtos.ElimTree.Leaf.Builder leafBuilder = ExpressionProtos.ElimTree.Leaf.newBuilder();
      List<? extends Integer> indices = ((LeafElimTree) elimTree).getArgumentIndices();
      leafBuilder.setHasIndices(indices != null);
      if (indices != null) {
        for (Integer index : indices) {
          leafBuilder.addIndex(index);
        }
      }
      leafBuilder.setClauseIndex(((LeafElimTree) elimTree).getClauseIndex());
      builder.setLeaf(leafBuilder.build());
    } else {
      BranchElimTree branchElimTree = (BranchElimTree) elimTree;
      ExpressionProtos.ElimTree.Branch.Builder branchBuilder = ExpressionProtos.ElimTree.Branch.newBuilder();
      branchBuilder.setKeepConCall(branchElimTree.keepConCall());

      if (branchElimTree.isArray()) {
        ExpressionProtos.ElimTree.Branch.ArrayClause.Builder arrayBuilder = ExpressionProtos.ElimTree.Branch.ArrayClause.newBuilder();
        boolean withElementsType = true;
        for (Map.Entry<BranchKey, ElimTree> entry : branchElimTree.getChildren()) {
          if (entry.getKey() instanceof ArrayConstructor) {
            withElementsType = ((ArrayConstructor) entry.getKey()).withElementsType();
            if (((ArrayConstructor) entry.getKey()).getConstructor() == Prelude.EMPTY_ARRAY) {
              arrayBuilder.setEmptyElimTree(writeElimTree(entry.getValue()));
            } else {
              arrayBuilder.setConsElimTree(writeElimTree(entry.getValue()));
            }
          }
        }
        arrayBuilder.setWithElementsType(withElementsType);
        branchBuilder.setArrayClause(arrayBuilder.build());
      } else {
        for (Map.Entry<BranchKey, ElimTree> entry : branchElimTree.getChildren()) {
          if (entry.getKey() == null) {
            branchBuilder.putClauses(0, writeElimTree(entry.getValue()));
          } else if (entry.getKey() instanceof SingleConstructor) {
            ExpressionProtos.ElimTree.Branch.SingleConstructorClause.Builder singleClauseBuilder = ExpressionProtos.ElimTree.Branch.SingleConstructorClause.newBuilder();
            if (entry.getKey() instanceof TupleConstructor) {
              singleClauseBuilder.setTuple(ExpressionProtos.ElimTree.Branch.SingleConstructorClause.Tuple.newBuilder().setLength(entry.getKey().getNumberOfParameters()).build());
            } else if (entry.getKey() instanceof IdpConstructor) {
              singleClauseBuilder.setIdp(ExpressionProtos.ElimTree.Branch.SingleConstructorClause.Idp.newBuilder());
            } else if (entry.getKey() instanceof ClassConstructor) {
              ClassConstructor classCon = (ClassConstructor) entry.getKey();
              ExpressionProtos.ElimTree.Branch.SingleConstructorClause.Class.Builder conBuilder = ExpressionProtos.ElimTree.Branch.SingleConstructorClause.Class.newBuilder();
              conBuilder.setClassRef(myCallTargetIndexProvider.getDefIndex(classCon.getClassDefinition()));
              conBuilder.setLevels(writeLevels(classCon.getLevels(), classCon.getClassDefinition()));
              for (ClassField field : classCon.getImplementedFields()) {
                conBuilder.addField(myCallTargetIndexProvider.getDefIndex(field));
              }
              singleClauseBuilder.setClass_(conBuilder.build());
            } else {
              throw new IllegalStateException("Unknown SingleConstructor type: " + entry.getKey().getClass());
            }
            singleClauseBuilder.setElimTree(writeElimTree(entry.getValue()));
            branchBuilder.setSingleClause(singleClauseBuilder.build());
          } else if (entry.getKey() instanceof Constructor) {
            branchBuilder.putClauses(myCallTargetIndexProvider.getDefIndex((Constructor) entry.getKey()), writeElimTree(entry.getValue()));
          } else {
            throw new IllegalStateException();
          }
        }
      }

      builder.setBranch(branchBuilder);
    }
    return builder.build();
  }

  ExpressionProtos.ElimBody writeElimBody(ElimBody elimBody) {
    ExpressionProtos.ElimBody.Builder builder = ExpressionProtos.ElimBody.newBuilder();
    for (ElimClause<Pattern> clause : elimBody.getClauses()) {
      builder.addClause(writeElimClause(clause));
    }
    builder.setElimTree(writeElimTree(elimBody.getElimTree()));
    return builder.build();
  }

  @Override
  public ExpressionProtos.Expression visitApp(AppExpression expr, Void params) {
    ExpressionProtos.Expression.App.Builder builder = ExpressionProtos.Expression.App.newBuilder();
    builder.setFunction(expr.getFunction().accept(this, null));
    builder.setArgument(expr.getArgument().accept(this, null));
    builder.setIsExplicit(expr.isExplicit());
    return ExpressionProtos.Expression.newBuilder().setApp(builder).build();
  }

  private ExpressionProtos.Expression.FunCall writeFunCall(FunCallExpression expr) {
    ExpressionProtos.Expression.FunCall.Builder builder = ExpressionProtos.Expression.FunCall.newBuilder();
    builder.setFunRef(myCallTargetIndexProvider.getDefIndex(expr.getDefinition()));
    builder.setLevels(writeLevels(expr.getLevels(), expr.getDefinition()));
    for (Expression arg : expr.getDefCallArguments()) {
      builder.addArgument(arg.accept(this, null));
    }
    return builder.build();
  }

  @Override
  public ExpressionProtos.Expression visitFunCall(FunCallExpression expr, Void params) {
    return ExpressionProtos.Expression.newBuilder().setFunCall(writeFunCall(expr)).build();
  }

  @Override
  public ExpressionProtos.Expression visitConCall(ConCallExpression expr, Void params) {
    ExpressionProtos.Expression.ConCalls.Builder builders = ExpressionProtos.Expression.ConCalls.newBuilder();

    while (true) {
      ExpressionProtos.Expression.ConCall.Builder builder = ExpressionProtos.Expression.ConCall.newBuilder();
      builder.setConstructorRef(myCallTargetIndexProvider.getDefIndex(expr.getDefinition()));
      builder.setRecursiveParam(expr.getDefinition().getRecursiveParameter());
      builder.setLevels(writeLevels(expr.getLevels(), expr.getDefinition()));
      for (Expression arg : expr.getDataTypeArguments()) {
        builder.addDatatypeArgument(arg.accept(this, null));
      }

      int recursiveParam = expr.getDefinition().getRecursiveParameter();
      List<Expression> defCallArgs = expr.getDefCallArguments();
      for (int i = 0; i < defCallArgs.size(); i++) {
        Expression arg = defCallArgs.get(i);
        if (i == recursiveParam) {
          if (!(arg instanceof ConCallExpression)) {
            recursiveParam = -1;
          } else {
            expr = (ConCallExpression) arg;
          }
        }
        if (i != recursiveParam) {
          builder.addArgument(arg.accept(this, null));
        }
      }

      builders.addConCall(builder);
      if (recursiveParam < 0 || defCallArgs.isEmpty()) {
        break;
      }
    }

    return ExpressionProtos.Expression.newBuilder().setConCalls(builders).build();
  }

  @Override
  public ExpressionProtos.Expression visitDataCall(DataCallExpression expr, Void params) {
    ExpressionProtos.Expression.DataCall.Builder builder = ExpressionProtos.Expression.DataCall.newBuilder();
    builder.setDataRef(myCallTargetIndexProvider.getDefIndex(expr.getDefinition()));
    builder.setLevels(writeLevels(expr.getLevels(), expr.getDefinition()));
    for (Expression arg : expr.getDefCallArguments()) {
      builder.addArgument(arg.accept(this, null));
    }
    return ExpressionProtos.Expression.newBuilder().setDataCall(builder).build();
  }

  private ExpressionProtos.Expression.ClassCall writeClassCall(ClassCallExpression expr) {
    ExpressionProtos.Expression.ClassCall.Builder builder = ExpressionProtos.Expression.ClassCall.newBuilder();
    builder.setClassRef(myCallTargetIndexProvider.getDefIndex(expr.getDefinition()));
    builder.setLevels(writeLevels(expr.getLevels(), expr.getDefinition()));
    registerBinding(expr.getThisBinding());
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      builder.putFieldSet(myCallTargetIndexProvider.getDefIndex(entry.getKey()), writeExpr(entry.getValue()));
    }
    builder.setSort(writeSort(expr.getSort()));
    builder.setUniverseKind(writeUniverseKind(expr.getUniverseKind()));
    return builder.build();
  }

  ExpressionProtos.UniverseKind writeUniverseKind(UniverseKind kind) {
    switch (kind) {
      case NO_UNIVERSES:
        return ExpressionProtos.UniverseKind.NO_UNIVERSES;
      case ONLY_COVARIANT:
        return ExpressionProtos.UniverseKind.ONLY_COVARIANT;
      case WITH_UNIVERSES:
        return ExpressionProtos.UniverseKind.WITH_UNIVERSES;
    }
    throw new IllegalStateException();
  }

  @Override
  public ExpressionProtos.Expression visitClassCall(ClassCallExpression expr, Void params) {
    return ExpressionProtos.Expression.newBuilder().setClassCall(writeClassCall(expr)).build();
  }

  @Override
  public ExpressionProtos.Expression visitReference(ReferenceExpression expr, Void params) {
    ExpressionProtos.Expression.Reference.Builder builder = ExpressionProtos.Expression.Reference.newBuilder();
    builder.setBindingRef(writeBindingRef(expr.getBinding()));
    return ExpressionProtos.Expression.newBuilder().setReference(builder).build();
  }

  @Override
  public ExpressionProtos.Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public ExpressionProtos.Expression visitSubst(SubstExpression expr, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public ExpressionProtos.Expression visitLam(LamExpression expr, Void params) {
    ExpressionProtos.Expression.Lam.Builder builder = ExpressionProtos.Expression.Lam.newBuilder();
    builder.setResultSort(writeSort(expr.getResultSort()));
    builder.setParam(writeSingleParameter(expr.getParameters()));
    builder.setBody(expr.getBody().accept(this, null));
    return ExpressionProtos.Expression.newBuilder().setLam(builder).build();
  }

  ExpressionProtos.Expression.Pi visitPi(PiExpression expr) {
    ExpressionProtos.Expression.Pi.Builder builder = ExpressionProtos.Expression.Pi.newBuilder();
    builder.setResultSort(LevelProtos.Sort.newBuilder(writeSort(expr.getResultSort())));
    builder.setParam(writeSingleParameter(expr.getParameters()));
    builder.setCodomain(expr.getCodomain().accept(this, null));
    return builder.build();
  }

  @Override
  public ExpressionProtos.Expression visitPi(PiExpression expr, Void params) {
    return ExpressionProtos.Expression.newBuilder().setPi(visitPi(expr)).build();
  }

  @Override
  public ExpressionProtos.Expression visitUniverse(UniverseExpression expr, Void params) {
    ExpressionProtos.Expression.Universe.Builder builder = ExpressionProtos.Expression.Universe.newBuilder();
    builder.setSort(writeSort(expr.getSort()));
    return ExpressionProtos.Expression.newBuilder().setUniverse(builder).build();
  }

  @Override
  public ExpressionProtos.Expression visitError(ErrorExpression expr, Void params) {
    ExpressionProtos.Expression.Error.Builder builder = ExpressionProtos.Expression.Error.newBuilder();
    if (expr.getExpression() != null && expr.isGoal()) {
      builder.setExpression(expr.getExpression().accept(this, null));
    }
    builder.setIsGoal(expr.isGoal());
    builder.setUseExpression(expr.useExpression());
    return ExpressionProtos.Expression.newBuilder().setError(builder).build();
  }

  @Override
  public ExpressionProtos.Expression visitTuple(TupleExpression expr, Void params) {
    ExpressionProtos.Expression.Tuple.Builder builder = ExpressionProtos.Expression.Tuple.newBuilder();
    for (Expression field : expr.getFields()) {
      builder.addField(field.accept(this, null));
    }
    builder.setType(writeSigma(expr.getSigmaType()));
    return ExpressionProtos.Expression.newBuilder().setTuple(builder).build();
  }

  private ExpressionProtos.Expression.Sigma writeSigma(SigmaExpression sigma) {
    ExpressionProtos.Expression.Sigma.Builder builder = ExpressionProtos.Expression.Sigma.newBuilder();
    builder.setPLevel(LevelProtos.Level.newBuilder(writeLevel(sigma.getSort().getPLevel())).build());
    builder.setHLevel(LevelProtos.Level.newBuilder(writeLevel(sigma.getSort().getHLevel())).build());
    builder.addAllParam(writeParameters(sigma.getParameters()));
    return builder.build();
  }

  @Override
  public ExpressionProtos.Expression visitSigma(SigmaExpression expr, Void params) {
    return ExpressionProtos.Expression.newBuilder().setSigma(writeSigma(expr)).build();
  }

  @Override
  public ExpressionProtos.Expression visitProj(ProjExpression expr, Void params) {
    ExpressionProtos.Expression.Proj.Builder builder = ExpressionProtos.Expression.Proj.newBuilder();
    builder.setExpression(expr.getExpression().accept(this, null));
    builder.setField(expr.getField());
    return ExpressionProtos.Expression.newBuilder().setProj(builder).build();
  }

  @Override
  public ExpressionProtos.Expression visitNew(NewExpression expr, Void params) {
    ExpressionProtos.Expression.New.Builder builder = ExpressionProtos.Expression.New.newBuilder();
    if (expr.getRenewExpression() != null) {
      builder.setRenew(writeExpr(expr.getRenewExpression()));
    }
    builder.setClassCall(writeClassCall(expr.getClassCall()));
    return ExpressionProtos.Expression.newBuilder().setNew(builder).build();
  }

  @Override
  public ExpressionProtos.Expression visitPEval(PEvalExpression expr, Void params) {
    ExpressionProtos.Expression.PEval.Builder builder = ExpressionProtos.Expression.PEval.newBuilder();
    builder.setExpression(expr.getExpression().accept(this, null));
    return ExpressionProtos.Expression.newBuilder().setPEval(builder).build();
  }

  @Override
  public ExpressionProtos.Expression visitLet(LetExpression letExpression, Void params) {
    ExpressionProtos.Expression.Let.Builder builder = ExpressionProtos.Expression.Let.newBuilder();
    builder.setIsStrict(letExpression.isStrict());
    for (HaveClause letClause : letExpression.getClauses()) {
      ExpressionProtos.Expression.Let.Clause.Builder letBuilder = ExpressionProtos.Expression.Let.Clause.newBuilder()
        .setIsLet(letClause instanceof LetClause)
        .setPattern(writeLetClausePattern(letClause.getPattern()))
        .setExpression(writeExpr(letClause.getExpression()));
      if (letClause.getName() != null) {
        letBuilder.setName(letClause.getName());
      }
      builder.addClause(letBuilder);
      registerBinding(letClause);
    }
    builder.setExpression(letExpression.getExpression().accept(this, null));
    return ExpressionProtos.Expression.newBuilder().setLet(builder).build();
  }

  private ExpressionProtos.Expression.Let.Pattern writeLetClausePattern(LetClausePattern pattern) {
    ExpressionProtos.Expression.Let.Pattern.Builder builder = ExpressionProtos.Expression.Let.Pattern.newBuilder();
    if (pattern.isMatching()) {
      List<? extends ClassField> fields = pattern.getFields();
      if (fields != null) {
        builder.setKind(ExpressionProtos.Expression.Let.Pattern.Kind.RECORD);
        for (ClassField field : fields) {
          builder.addField(myCallTargetIndexProvider.getDefIndex(field));
        }
      } else {
        builder.setKind(ExpressionProtos.Expression.Let.Pattern.Kind.TUPLE);
      }
      for (LetClausePattern subPattern : pattern.getPatterns()) {
        builder.addPattern(writeLetClausePattern(subPattern));
      }
    } else {
      builder.setKind(ExpressionProtos.Expression.Let.Pattern.Kind.NAME);
      if (pattern.getName() != null) {
        builder.setName(pattern.getName());
      }
    }
    return builder.build();
  }

  @Override
  public ExpressionProtos.Expression visitCase(CaseExpression expr, Void params) {
    ExpressionProtos.Expression.Case.Builder builder = ExpressionProtos.Expression.Case.newBuilder();
    builder.setIsSFunc(expr.isSCase());
    builder.setElimBody(writeElimBody(expr.getElimBody()));
    builder.addAllParam(writeParameters(expr.getParameters()));
    builder.setResultType(writeExpr(expr.getResultType()));
    if (expr.getResultTypeLevel() != null) {
      builder.setResultTypeLevel(writeExpr(expr.getResultTypeLevel()));
    }
    for (Expression argument : expr.getArguments()) {
      builder.addArgument(writeExpr(argument));
    }
    return ExpressionProtos.Expression.newBuilder().setCase(builder).build();
  }

  @Override
  public ExpressionProtos.Expression visitOfType(OfTypeExpression expr, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public ExpressionProtos.Expression visitInteger(IntegerExpression expr, Void params) {
    if (expr instanceof SmallIntegerExpression) {
      ExpressionProtos.Expression.SmallInteger.Builder builder = ExpressionProtos.Expression.SmallInteger.newBuilder();
      builder.setValue(((SmallIntegerExpression) expr).getInteger());
      return ExpressionProtos.Expression.newBuilder().setSmallInteger(builder).build();
    } else {
      ExpressionProtos.Expression.BigInteger.Builder builder = ExpressionProtos.Expression.BigInteger.newBuilder();
      builder.setValue(ByteString.copyFrom(expr.getBigInteger().toByteArray()));
      return ExpressionProtos.Expression.newBuilder().setBigInteger(builder).build();
    }
  }

  @Override
  public ExpressionProtos.Expression visitTypeCoerce(TypeCoerceExpression expr, Void params) {
    ExpressionProtos.Expression.TypeCoerce.Builder builder = ExpressionProtos.Expression.TypeCoerce.newBuilder();
    builder.setFunRef(myCallTargetIndexProvider.getDefIndex(expr.getDefinition()));
    builder.setLevels(writeLevels(expr.getLevels(), expr.getDefinition()));
    builder.setClauseIndex(expr.getClauseIndex());
    for (Expression arg : expr.getClauseArguments()) {
      builder.addClauseArgument(arg.accept(this, null));
    }
    builder.setArgument(expr.getArgument().accept(this, null));
    builder.setFromLeftToRight(expr.isFromLeftToRight());
    return ExpressionProtos.Expression.newBuilder().setTypeCoerce(builder.build()).build();
  }

  @Override
  public ExpressionProtos.Expression visitArray(ArrayExpression expr, Void params) {
    ExpressionProtos.Expression.Array.Builder builder = ExpressionProtos.Expression.Array.newBuilder();
    LevelPair levelPair = expr.getLevels().toLevelPair();
    builder.setPLevel(writeLevel(levelPair.get(LevelVariable.PVAR)));
    builder.setHLevel(writeLevel(levelPair.get(LevelVariable.HVAR)));
    builder.setElementsType(writeExpr(expr.getElementsType()));
    for (Expression element : expr.getElements()) {
      builder.addElement(writeExpr(element));
    }
    if (expr.getTail() != null) {
      builder.setTail(writeExpr(expr.getTail()));
    }
    return ExpressionProtos.Expression.newBuilder().setArray(builder.build()).build();
  }

  @Override
  public ExpressionProtos.Expression visitFieldCall(FieldCallExpression expr, Void params) {
    ExpressionProtos.Expression.FieldCall.Builder builder = ExpressionProtos.Expression.FieldCall.newBuilder();
    builder.setFieldRef(myCallTargetIndexProvider.getDefIndex(expr.getDefinition()));
    builder.setLevels(writeLevels(expr.getLevels(), expr.getDefinition()));
    builder.setExpression(expr.getArgument().accept(this, null));
    return ExpressionProtos.Expression.newBuilder().setFieldCall(builder).build();
  }
}
