package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.LinkList;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.*;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeExpression;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ExpressionDeserialization {
  private final CallTargetProvider myCallTargetProvider;
  private final List<Binding> myBindings = new ArrayList<>();  // de Bruijn indices

  ExpressionDeserialization(CallTargetProvider callTargetProvider) {
    myCallTargetProvider = callTargetProvider;
  }


  // Bindings

  private RollbackBindings checkpointBindings() {
    return new RollbackBindings(myBindings.size());
  }

  private class RollbackBindings implements AutoCloseable {
    private final int myTargetSize;

    private RollbackBindings(int targetSize) {
      myTargetSize = targetSize;
    }

    @Override
    public void close() {
      for (int i = myBindings.size() - 1; i >= myTargetSize; i--) {
        myBindings.remove(i);
      }
    }
  }

  private void registerBinding(Binding binding) {
    myBindings.add(binding);
  }

  Type readType(ExpressionProtos.Type proto) throws DeserializationException {
    Expression expr = readExpr(proto.getExpr());
    return expr instanceof Type ? (Type) expr : new TypeExpression(expr, readSort(proto.getSort()));
  }

  private Variable readBindingRef(int index) throws DeserializationException {
    if (index == 0) {
      return null;
    } else {
      Variable binding = myBindings.get(index - 1);
      if (binding == null) {
        throw new DeserializationException("Trying to read a reference to an unregistered binding");
      }
      return binding;
    }
  }

  // Sorts and levels

  private Level readLevel(LevelProtos.Level proto) throws DeserializationException {
    Variable var;
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
    if (var == null && constant == -10) {
      return Level.INFINITY;
    } else {
      return new Level((LevelVariable) var, constant, proto.getMaxConstant());
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
      DependentLink tele = ExpressionFactory.parameter(!proto.getIsNotExplicit(), unfixedNames, type);
      for (DependentLink link = tele; link.hasNext(); link = link.getNext()) {
        registerBinding(link);
      }
      list.append(tele);
    }
    return list.getFirst();
  }

  SingleDependentLink readSingleParameter(ExpressionProtos.Telescope proto) throws DeserializationException {
    List<String> unfixedNames = new ArrayList<>(proto.getNameList().size());
    for (String name : proto.getNameList()) {
      unfixedNames.add(name.isEmpty() ? null : name);
    }
    Type type = proto.getType() != null ? readType(proto.getType()) : null;
    SingleDependentLink tele = ExpressionFactory.singleParams(!proto.getIsNotExplicit(), unfixedNames, type);
    for (DependentLink link = tele; link.hasNext(); link = link.getNext()) {
      registerBinding(link);
    }
    return tele;
  }

  DependentLink readParameter(ExpressionProtos.SingleParameter proto) throws DeserializationException {
    Type type = readType(proto.getType());
    DependentLink link;
    if (proto.hasType()) {
      link = new TypedDependentLink(!proto.getIsNotExplicit(), proto.getName(), type, EmptyDependentLink.getInstance());
    } else {
      link = new UntypedDependentLink(proto.getName(), EmptyDependentLink.getInstance());
    }
    registerBinding(link);
    return link;
  }


  // Expressions and ElimTrees

  ElimTree readElimTree(ExpressionProtos.ElimTree proto) throws DeserializationException {
    DependentLink parameters = readParameters(proto.getParamList());
    switch (proto.getKindCase()) {
      case BRANCH: {
        Map<Constructor, ElimTree> children = new HashMap<>();
        for (Map.Entry<Integer, ExpressionProtos.ElimTree> entry : proto.getBranch().getClausesMap().entrySet()) {
          children.put(myCallTargetProvider.getCallTarget(entry.getKey(), Constructor.class), readElimTree(entry.getValue()));
        }
        return new BranchElimTree(parameters, children);
      }
      case LEAF:
        return new LeafElimTree(parameters, readExpr(proto.getLeaf().getExpr()));
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
      case LET:
        return readLet(proto.getLet());
      case CASE:
        return readCase(proto.getCase());
      case FIELD_CALL:
        return readFieldCall(proto.getFieldCall());
      default:
        throw new DeserializationException("Unknown Expression kind: " + proto.getKindCase());
    }
  }

  List<Expression> readExprList(List<ExpressionProtos.Expression> protos) throws DeserializationException {
    List<Expression> result = new ArrayList<>(protos.size());
    for (ExpressionProtos.Expression proto : protos) {
      result.add(readExpr(proto));
    }
    return result;
  }


  private AppExpression readApp(ExpressionProtos.Expression.App proto) throws DeserializationException {
    return new AppExpression(readExpr(proto.getFunction()), readExpr(proto.getArgument()));
  }

  private FunCallExpression readFunCall(ExpressionProtos.Expression.FunCall proto) throws DeserializationException {
    return new FunCallExpression(myCallTargetProvider.getCallTarget(proto.getFunRef(), FunctionDefinition.class), new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readExprList(proto.getArgumentList()));
  }

  private ConCallExpression readConCall(ExpressionProtos.Expression.ConCall proto) throws DeserializationException {
    return new ConCallExpression(myCallTargetProvider.getCallTarget(proto.getConstructorRef(), Constructor.class), new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())),
        readExprList(proto.getDatatypeArgumentList()), readExprList(proto.getArgumentList()));
  }

  private DataCallExpression readDataCall(ExpressionProtos.Expression.DataCall proto) throws DeserializationException {
    return new DataCallExpression(myCallTargetProvider.getCallTarget(proto.getDataRef(), DataDefinition.class), new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readExprList(proto.getArgumentList()));
  }

  private ClassCallExpression readClassCall(ExpressionProtos.Expression.ClassCall proto) throws DeserializationException {
    Map<ClassField, Expression> fieldSet = new HashMap<>();
    for (Map.Entry<Integer, ExpressionProtos.Expression> entry : proto.getFieldSetMap().entrySet()) {
      fieldSet.put(myCallTargetProvider.getCallTarget(entry.getKey(), ClassField.class), readExpr(entry.getValue()));
    }
    return new ClassCallExpression(myCallTargetProvider.getCallTarget(proto.getClassRef(), ClassDefinition.class), new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), fieldSet, readSort(proto.getSort()));
  }

  private ReferenceExpression readReference(ExpressionProtos.Expression.Reference proto) throws DeserializationException {
    return new ReferenceExpression((Binding)readBindingRef(proto.getBindingRef()));
  }

  private LamExpression readLam(ExpressionProtos.Expression.Lam proto) throws DeserializationException {
    return new LamExpression(readSort(proto.getResultSort()), readSingleParameter(proto.getParam()), readExpr(proto.getBody()));
  }

  private PiExpression readPi(ExpressionProtos.Expression.Pi proto) throws DeserializationException {
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
    return new ErrorExpression(expr, null);
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
    return new NewExpression(readClassCall(proto.getClassCall()));
  }

  private LetExpression readLet(ExpressionProtos.Expression.Let proto) throws DeserializationException {
    List<LetClause> clauses = new ArrayList<>();
    for (ExpressionProtos.Expression.Let.Clause cProto : proto.getClauseList()) {
      LetClause clause = new LetClause(cProto.getName(), readExpr(cProto.getExpression()));
      registerBinding(clause);
      clauses.add(clause);
    }
    return new LetExpression(clauses, readExpr(proto.getExpression()));
  }

  private CaseExpression readCase(ExpressionProtos.Expression.Case proto) throws DeserializationException {
    List<Expression> arguments = new ArrayList<>(proto.getArgumentCount());
    ElimTree elimTree = readElimTree(proto.getElimTree());
    DependentLink parameters = readParameters(proto.getParamList());
    Expression type = readExpr(proto.getResultType());
    for (ExpressionProtos.Expression argument : proto.getArgumentList()) {
      arguments.add(readExpr(argument));
    }
    return new CaseExpression(parameters, type, elimTree, arguments);
  }

  private Expression readFieldCall(ExpressionProtos.Expression.FieldCall proto) throws DeserializationException {
    return FieldCallExpression.make(myCallTargetProvider.getCallTarget(proto.getFieldRef(), ClassField.class), readExpr(proto.getExpression()));
  }
}
