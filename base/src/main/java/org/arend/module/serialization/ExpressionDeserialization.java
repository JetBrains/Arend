package org.arend.module.serialization;

import org.arend.core.constructor.ClassConstructor;
import org.arend.core.constructor.IdpConstructor;
import org.arend.core.constructor.TupleConstructor;
import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.*;
import org.arend.core.definition.*;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.let.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.pattern.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.ext.serialization.DeserializationException;
import org.arend.naming.reference.TCReferable;
import org.arend.prelude.Prelude;
import org.arend.typechecking.order.dependency.DependencyListener;

import java.math.BigInteger;
import java.util.*;

class ExpressionDeserialization {
  private final CallTargetProvider myCallTargetProvider;
  private final List<Binding> myBindings = new ArrayList<>();

  private final DependencyListener myDependencyListener;
  private final TCReferable myDefinition;

  ExpressionDeserialization(CallTargetProvider callTargetProvider, DependencyListener dependencyListener, TCReferable definition) {
    myCallTargetProvider = callTargetProvider;
    myDependencyListener = dependencyListener;
    myDefinition = definition;
  }

  // Bindings

  private void registerBinding(Binding binding) {
    myBindings.add(binding);
  }

  private Type readType(ExpressionProtos.Type proto) throws DeserializationException {
    Expression expr = readExpr(proto.getExpr());
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, readSort(proto.getSort()));
  }

  Binding readBindingRef(int index) throws DeserializationException {
    if (index == 0) {
      return null;
    } else {
      Binding binding = myBindings.get(index - 1);
      if (binding == null) {
        throw new DeserializationException("Trying to read a reference to an unregistered binding");
      }
      return binding;
    }
  }

  // Sorts and levels

  private Level readLevel(LevelProtos.Level proto) throws DeserializationException {
    LevelVariable var;
    switch (proto.getVariable()) {
      case NO_VAR:
        var = null;
        break;
      case PLVL:
        var = LevelVariable.PVAR;
        break;
      case HLVL:
        var = LevelVariable.HVAR;
        break;
      default: throw new DeserializationException("Unrecognized level variable");
    }

    int constant = proto.getConstant();
    if (var == null && constant == Level.INFINITY.getConstant()) {
      return Level.INFINITY;
    } else {
      return new Level(var, constant, proto.getMaxConstant());
    }
  }

  Sort readSort(LevelProtos.Sort proto) throws DeserializationException {
    return new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel()));
  }


  // Parameters

  DependentLink readParameters(List<ExpressionProtos.Telescope> protos) throws DeserializationException {
    LinkList list = new LinkList();
    for (ExpressionProtos.Telescope proto : protos) {
      List<String> unfixedNames = new ArrayList<>(proto.getNameList().size());
      for (String name : proto.getNameList()) {
        unfixedNames.add(name.isEmpty() ? null : name);
      }
      Type type = readType(proto.getType());
      DependentLink tele = proto.getIsHidden() && unfixedNames.size() == 1
        ? new TypedDependentLink(!proto.getIsNotExplicit(), unfixedNames.get(0), type, true, EmptyDependentLink.getInstance())
        : ExpressionFactory.parameter(!proto.getIsNotExplicit(), unfixedNames, type);
      for (DependentLink link = tele; link.hasNext(); link = link.getNext()) {
        registerBinding(link);
      }
      list.append(tele);
    }
    return list.getFirst();
  }

  private SingleDependentLink readSingleParameter(ExpressionProtos.Telescope proto) throws DeserializationException {
    List<String> unfixedNames = new ArrayList<>(proto.getNameList().size());
    for (String name : proto.getNameList()) {
      unfixedNames.add(name.isEmpty() ? null : name);
    }
    Type type = proto.getType() != null ? readType(proto.getType()) : null;
    SingleDependentLink tele = proto.getIsHidden() && unfixedNames.size() == 1
      ? new TypedSingleDependentLink(!proto.getIsNotExplicit(), unfixedNames.get(0), type, true)
      : ExpressionFactory.singleParams(!proto.getIsNotExplicit(), unfixedNames, type);
    for (DependentLink link = tele; link.hasNext(); link = link.getNext()) {
      registerBinding(link);
    }
    return tele;
  }

  DependentLink readParameter(ExpressionProtos.SingleParameter proto) throws DeserializationException {
    DependentLink link;
    if (proto.hasType()) {
      link = new TypedDependentLink(!proto.getIsNotExplicit(), proto.getName(), readType(proto.getType()), proto.getIsHidden(), EmptyDependentLink.getInstance());
    } else {
      link = new UntypedDependentLink(proto.getName());
    }
    registerBinding(link);
    return link;
  }

  private TypedBinding readBinding(ExpressionProtos.TypedBinding proto) throws DeserializationException {
    TypedBinding binding = new TypedBinding(proto.getName(), readExpr(proto.getType()));
    registerBinding(binding);
    return binding;
  }

  // Expressions and ElimTrees

  AbsExpression readAbsExpr(ExpressionProtos.Expression.Abs proto) throws DeserializationException {
    return new AbsExpression(proto.hasBinding() ? readBinding(proto.getBinding()) : null, readExpr(proto.getExpression()));
  }

  ElimBody readElimBody(ExpressionProtos.ElimBody proto) throws DeserializationException {
    List<ElimClause<Pattern>> clauses = new ArrayList<>();
    for (ExpressionProtos.ElimClause clause : proto.getClauseList()) {
      clauses.add(readElimClause(clause));
    }
    return new ElimBody(clauses, readElimTree(proto.getElimTree()));
  }

  private ElimClause<Pattern> readElimClause(ExpressionProtos.ElimClause proto) throws DeserializationException {
    List<Pattern> patterns = readPatterns(proto.getPatternList(), new LinkList());
    return new ElimClause<>(patterns, proto.hasExpression() ? readExpr(proto.getExpression()) : null);
  }

  List<Pattern> readPatterns(List<ExpressionProtos.Pattern> protos, LinkList list) throws DeserializationException {
    List<Pattern> patterns = new ArrayList<>(protos.size());
    for (ExpressionProtos.Pattern proto : protos) {
      patterns.add(readPattern(proto, list));
    }
    return patterns;
  }

  List<ExpressionPattern> readExpressionPatterns(List<ExpressionProtos.Pattern> protos, LinkList list) throws DeserializationException {
    List<ExpressionPattern> patterns = new ArrayList<>(protos.size());
    for (ExpressionProtos.Pattern proto : protos) {
      Pattern pattern = readPattern(proto, list);
      if (!(pattern instanceof ExpressionPattern)) {
        throw new DeserializationException("Expected an expression pattern");
      }
      patterns.add((ExpressionPattern) pattern);
    }
    return patterns;
  }

  private Pattern readPattern(ExpressionProtos.Pattern proto, LinkList list) throws DeserializationException {
    switch (proto.getKindCase()) {
      case BINDING:
        DependentLink param = readParameter(proto.getBinding().getVar());
        list.append(param);
        return new BindingPattern(param);
      case EMPTY:
        return EmptyPattern.INSTANCE;
      case CONSTRUCTOR: {
        ExpressionProtos.Pattern.Constructor conProto = proto.getConstructor();
        int def = conProto.getDefinition();
        List<Pattern> patterns = readPatterns(conProto.getPatternList(), list);
        return ConstructorPattern.make(def == 0 ? null : myCallTargetProvider.getCallTarget(def), patterns);
      }
      case EXPRESSION_CONSTRUCTOR: {
        ExpressionProtos.Pattern.ExpressionConstructor conProto = proto.getExpressionConstructor();
        Expression expression = readExpr(conProto.getExpression());
        List<ExpressionPattern> patterns = readExpressionPatterns(conProto.getPatternList(), list);
        if (expression instanceof SmallIntegerExpression && ((SmallIntegerExpression) expression).getInteger() == 0) {
          return new ConstructorExpressionPattern(new ConCallExpression(Prelude.ZERO, Sort.PROP, Collections.emptyList(), Collections.emptyList()), patterns);
        }
        if (expression instanceof ConCallExpression) {
          return new ConstructorExpressionPattern((ConCallExpression) expression, patterns);
        }
        if (expression instanceof ClassCallExpression) {
          return new ConstructorExpressionPattern((ClassCallExpression) expression, patterns);
        }
        if (expression instanceof SigmaExpression) {
          return new ConstructorExpressionPattern((SigmaExpression) expression, patterns);
        }
        if (expression instanceof FunCallExpression && ((FunCallExpression) expression).getDefinition() instanceof DConstructor) {
          return new ConstructorExpressionPattern((FunCallExpression) expression, patterns);
        }
        throw new DeserializationException("Wrong pattern expression");
      }
      default:
        throw new DeserializationException("Unknown Pattern kind: " + proto.getKindCase());
    }
  }

  private ElimTree readElimTree(ExpressionProtos.ElimTree proto) throws DeserializationException {
    switch (proto.getKindCase()) {
      case BRANCH: {
        ExpressionProtos.ElimTree.Branch branchProto = proto.getBranch();
        BranchElimTree result = new BranchElimTree(proto.getSkip(), branchProto.getKeepConCall());
        for (Map.Entry<Integer, ExpressionProtos.ElimTree> entry : branchProto.getClausesMap().entrySet()) {
          result.addChild(entry.getKey() == 0 ? null : myCallTargetProvider.getCallTarget(entry.getKey(), Constructor.class), readElimTree(entry.getValue()));
        }
        if (branchProto.hasSingleClause()) {
          ExpressionProtos.ElimTree.Branch.SingleConstructorClause singleClause = branchProto.getSingleClause();
          ElimTree elimTree = readElimTree(singleClause.getElimTree());
          if (singleClause.hasTuple()) {
            result.addChild(new TupleConstructor(singleClause.getTuple().getLength()), elimTree);
          }
          if (singleClause.hasIdp()) {
            result.addChild(new IdpConstructor(), elimTree);
          }
          if (singleClause.hasClass_()) {
            ExpressionProtos.ElimTree.Branch.SingleConstructorClause.Class classProto = singleClause.getClass_();
            Set<ClassField> fields = new HashSet<>();
            for (Integer fieldRef : classProto.getFieldList()) {
              fields.add(myCallTargetProvider.getCallTarget(fieldRef, ClassField.class));
            }
            result.addChild(new ClassConstructor(myCallTargetProvider.getCallTarget(classProto.getClassRef(), ClassDefinition.class), readSort(classProto.getSort()), fields), elimTree);
          }
        }
        return result;
      }
      case LEAF: {
        ExpressionProtos.ElimTree.Leaf leaf = proto.getLeaf();
        return new LeafElimTree(proto.getSkip(), leaf.getHasIndices() ? leaf.getIndexList() : null, leaf.getClauseIndex());
      }
      default:
        throw new DeserializationException("Unknown ElimTreeNode kind: " + proto.getKindCase());
    }
  }

  Expression readExpr(ExpressionProtos.Expression proto) throws DeserializationException {
    switch (proto.getKindCase()) {
      case APP:
        return readApp(proto.getApp());
      case FUN_CALL:
        return readFunCall(proto.getFunCall());
      case CON_CALL:
        return readConCall(proto.getConCall());
      case DATA_CALL:
        return readDataCall(proto.getDataCall());
      case CLASS_CALL:
        return readClassCall(proto.getClassCall());
      case REFERENCE:
        return readReference(proto.getReference());
      case LAM:
        return readLam(proto.getLam());
      case PI:
        return readPi(proto.getPi());
      case UNIVERSE:
        return readUniverse(proto.getUniverse());
      case ERROR:
        return readError(proto.getError());
      case TUPLE:
        return readTuple(proto.getTuple());
      case SIGMA:
        return readSigma(proto.getSigma());
      case PROJ:
        return readProj(proto.getProj());
      case NEW:
        return readNew(proto.getNew());
      case PEVAL:
        return readPEval(proto.getPEval());
      case LET:
        return readLet(proto.getLet());
      case CASE:
        return readCase(proto.getCase());
      case FIELD_CALL:
        return readFieldCall(proto.getFieldCall());
      case SMALL_INTEGER:
        return readSmallInteger(proto.getSmallInteger());
      case BIG_INTEGER:
        return readBigInteger(proto.getBigInteger());
      default:
        throw new DeserializationException("Unknown Expression kind: " + proto.getKindCase());
    }
  }

  private List<Expression> readExprList(List<ExpressionProtos.Expression> protos) throws DeserializationException {
    List<Expression> result = new ArrayList<>(protos.size());
    for (ExpressionProtos.Expression proto : protos) {
      result.add(readExpr(proto));
    }
    return result;
  }


  private Expression readApp(ExpressionProtos.Expression.App proto) throws DeserializationException {
    return AppExpression.make(readExpr(proto.getFunction()), readExpr(proto.getArgument()), proto.getIsExplicit());
  }

  private Expression readFunCall(ExpressionProtos.Expression.FunCall proto) throws DeserializationException {
    FunctionDefinition functionDefinition = myCallTargetProvider.getCallTarget(proto.getFunRef(), FunctionDefinition.class);
    myDependencyListener.dependsOn(myDefinition, functionDefinition.getReferable());
    return FunCallExpression.make(functionDefinition, new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readExprList(proto.getArgumentList()));
  }

  private Expression readConCall(ExpressionProtos.Expression.ConCall proto) throws DeserializationException {
    Constructor constructor = myCallTargetProvider.getCallTarget(proto.getConstructorRef(), Constructor.class);
    myDependencyListener.dependsOn(myDefinition, constructor.getDataType().getReferable());
    return ConCallExpression.make(constructor, new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())),
        readExprList(proto.getDatatypeArgumentList()), readExprList(proto.getArgumentList()));
  }

  private DataCallExpression readDataCall(ExpressionProtos.Expression.DataCall proto) throws DeserializationException {
    DataDefinition dataDefinition = myCallTargetProvider.getCallTarget(proto.getDataRef(), DataDefinition.class);
    myDependencyListener.dependsOn(myDefinition, dataDefinition.getReferable());
    return new DataCallExpression(dataDefinition, new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readExprList(proto.getArgumentList()));
  }

  private ClassCallExpression readClassCall(ExpressionProtos.Expression.ClassCall proto) throws DeserializationException {
    ClassDefinition classDefinition = myCallTargetProvider.getCallTarget(proto.getClassRef(), ClassDefinition.class);
    myDependencyListener.dependsOn(myDefinition, classDefinition.getReferable());

    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassCallExpression classCall = new ClassCallExpression(classDefinition, new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), fieldSet, readSort(proto.getSort()), readUniverseKind(proto.getUniverseKind()));
    registerBinding(classCall.getThisBinding());
    for (Map.Entry<Integer, ExpressionProtos.Expression> entry : proto.getFieldSetMap().entrySet()) {
      fieldSet.put(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), readExpr(entry.getValue()));
    }
    return classCall;
  }

  UniverseKind readUniverseKind(ExpressionProtos.UniverseKind kind) throws DeserializationException {
    switch (kind) {
      case NO_UNIVERSES: return UniverseKind.NO_UNIVERSES;
      case ONLY_COVARIANT: return UniverseKind.ONLY_COVARIANT;
      case WITH_UNIVERSES: return UniverseKind.WITH_UNIVERSES;
      default: throw new DeserializationException("Unrecognized universe kind: " + kind);
    }
  }

  private ReferenceExpression readReference(ExpressionProtos.Expression.Reference proto) throws DeserializationException {
    return new ReferenceExpression(readBindingRef(proto.getBindingRef()));
  }

  private LamExpression readLam(ExpressionProtos.Expression.Lam proto) throws DeserializationException {
    return new LamExpression(readSort(proto.getResultSort()), readSingleParameter(proto.getParam()), readExpr(proto.getBody()));
  }

  PiExpression readPi(ExpressionProtos.Expression.Pi proto) throws DeserializationException {
    return new PiExpression(readSort(proto.getResultSort()), readSingleParameter(proto.getParam()), readExpr(proto.getCodomain()));
  }

  private UniverseExpression readUniverse(ExpressionProtos.Expression.Universe proto) throws DeserializationException {
    return new UniverseExpression(readSort(proto.getSort()));
  }

  private ErrorExpression readError(ExpressionProtos.Expression.Error proto) throws DeserializationException {
    final Expression expr;
    if (proto.hasExpression()) {
      expr = readExpr(proto.getExpression());
    } else {
      expr = null;
    }
    return new ErrorExpression(expr, proto.getIsGoal(), proto.getUseExpression());
  }

  private TupleExpression readTuple(ExpressionProtos.Expression.Tuple proto) throws DeserializationException {
    return new TupleExpression(readExprList(proto.getFieldList()), readSigma(proto.getType()));
  }

  private SigmaExpression readSigma(ExpressionProtos.Expression.Sigma proto) throws DeserializationException {
    return new SigmaExpression(new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readParameters(proto.getParamList()));
  }

  private Expression readProj(ExpressionProtos.Expression.Proj proto) throws DeserializationException {
    return ProjExpression.make(readExpr(proto.getExpression()), proto.getField());
  }

  private NewExpression readNew(ExpressionProtos.Expression.New proto) throws DeserializationException {
    return new NewExpression(proto.hasRenew() ? readExpr(proto.getRenew()) : null, readClassCall(proto.getClassCall()));
  }

  private PEvalExpression readPEval(ExpressionProtos.Expression.PEval proto) throws DeserializationException {
    return new PEvalExpression(readExpr(proto.getExpression()));
  }

  private String validName(String name) {
    return name.isEmpty() ? null : name;
  }

  private LetExpression readLet(ExpressionProtos.Expression.Let proto) throws DeserializationException {
    List<LetClause> clauses = new ArrayList<>();
    for (ExpressionProtos.Expression.Let.Clause cProto : proto.getClauseList()) {
      LetClause clause = new LetClause(validName(cProto.getName()), readLetClausePattern(cProto.getPattern()), readExpr(cProto.getExpression()));
      registerBinding(clause);
      clauses.add(clause);
    }
    return new LetExpression(proto.getIsStrict(), clauses, readExpr(proto.getExpression()));
  }

  private LetClausePattern readLetClausePattern(ExpressionProtos.Expression.Let.Pattern proto) throws DeserializationException {
    switch (proto.getKind()) {
      case RECORD: {
        List<ClassField> fields = new ArrayList<>();
        for (Integer fieldIndex : proto.getFieldList()) {
          fields.add(myCallTargetProvider.getCallTarget(fieldIndex, ClassField.class));
        }
        List<LetClausePattern> patterns = new ArrayList<>();
        for (ExpressionProtos.Expression.Let.Pattern pattern : proto.getPatternList()) {
          patterns.add(readLetClausePattern(pattern));
        }
        return new RecordLetClausePattern(fields, patterns);
      }
      case TUPLE: {
        List<LetClausePattern> patterns = new ArrayList<>();
        for (ExpressionProtos.Expression.Let.Pattern pattern : proto.getPatternList()) {
          patterns.add(readLetClausePattern(pattern));
        }
        return new TupleLetClausePattern(patterns);
      }
      case NAME:
        return new NameLetClausePattern(validName(proto.getName()));
      default:
        throw new DeserializationException("Unrecognized \\let pattern kind: " + proto.getKind());
    }
  }

  private CaseExpression readCase(ExpressionProtos.Expression.Case proto) throws DeserializationException {
    List<Expression> arguments = new ArrayList<>(proto.getArgumentCount());
    ElimBody elimBody = readElimBody(proto.getElimBody());
    DependentLink parameters = readParameters(proto.getParamList());
    Expression type = readExpr(proto.getResultType());
    Expression typeLevel = proto.hasResultTypeLevel() ? readExpr(proto.getResultTypeLevel()) : null;
    for (ExpressionProtos.Expression argument : proto.getArgumentList()) {
      arguments.add(readExpr(argument));
    }
    return new CaseExpression(proto.getIsSFunc(), parameters, type, typeLevel, elimBody, arguments);
  }

  private Expression readFieldCall(ExpressionProtos.Expression.FieldCall proto) throws DeserializationException {
    ClassField classField = myCallTargetProvider.getCallTarget(proto.getFieldRef(), ClassField.class);
    myDependencyListener.dependsOn(myDefinition, classField.getParentClass().getReferable());
    return FieldCallExpression.make(classField, new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readExpr(proto.getExpression()));
  }

  private SmallIntegerExpression readSmallInteger(ExpressionProtos.Expression.SmallInteger proto) {
    return new SmallIntegerExpression(proto.getValue());
  }

  private BigIntegerExpression readBigInteger(ExpressionProtos.Expression.BigInteger proto) {
    return new BigIntegerExpression(new BigInteger(proto.getValue().toByteArray()));
  }
}
