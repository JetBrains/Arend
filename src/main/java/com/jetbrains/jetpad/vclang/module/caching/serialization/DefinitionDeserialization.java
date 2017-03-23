package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeExpression;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class DefinitionDeserialization {
  private final CalltargetProvider.Typed myCalltargetProvider;
  private final List<Binding> myBindings = new ArrayList<>();  // de Bruijn indices

  DefinitionDeserialization(CalltargetProvider.Typed calltargetProvider) {
    myCalltargetProvider = calltargetProvider;
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

  TypedBinding readTypedBinding(ExpressionProtos.Binding.TypedBinding proto) throws DeserializationError {
    TypedBinding typedBinding = new TypedBinding(proto.getName(), readExpr(proto.getType()));
    registerBinding(typedBinding);
    return typedBinding;
  }

  private Variable readBindingRef(int index) throws DeserializationError {
    if (index == 0) {
      return null;
    } else {
      Variable binding = myBindings.get(index - 1);
      if (binding == null) {
        throw new DeserializationError("Trying to read a reference to an unregistered binding");
      }
      return binding;
    }
  }


  // Patterns

  Patterns readPatterns(DefinitionProtos.Definition.DataData.Constructor.Patterns proto) throws DeserializationError {
    return readPatterns(proto, new DependentLink[] {null});
  }

  private Patterns readPatterns(DefinitionProtos.Definition.DataData.Constructor.Patterns proto, DependentLink[] prev) throws DeserializationError {
    List<PatternArgument> args = new ArrayList<>();
    for (DefinitionProtos.Definition.DataData.Constructor.PatternArgument argProto : proto.getPatternArgumentList()) {
      args.add(new PatternArgument(readPattern(argProto.getPattern(), prev), !argProto.getNotExplicit(), argProto.getHidden()));
    }
    return new Patterns(args);
  }

  private Pattern readPattern(DefinitionProtos.Definition.DataData.Constructor.Pattern proto, DependentLink[] prev) throws DeserializationError {
    switch (proto.getKindCase()) {
      case NAME:
        DependentLink param = readParameter(proto.getName().getVar());
        if (prev[0] != null) prev[0].setNext(param);
        prev[0] = param;
        return new NamePattern(param);
      case ANY_CONSTRUCTOR:
        TypedDependentLink param1 = readParameter(proto.getAnyConstructor().getVar());
        if (prev[0] != null) prev[0].setNext(param1);
        prev[0] = param1;
        return new AnyConstructorPattern(param1);
      case CONSTRUCTOR:
        return new ConstructorPattern(myCalltargetProvider.getCalltarget(proto.getConstructor().getConstructorRef(), Constructor.class), readPatterns(proto.getConstructor().getPatterns(), prev));
      default:
        throw new DeserializationError("Unknown Pattern kind: " + proto.getKindCase());
    }
  }



  // Sorts and levels

  private Level readLevel(LevelProtos.Level proto) throws DeserializationError {
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
      default: throw new DeserializationError("Unrecognized level variable");
    }

    int constant = proto.getConstant();
    if (var == null && constant == -1) {
      return Level.INFINITY;
    } else {
      return new Level((LevelVariable) var, constant);
    }
  }

  Sort readSort(LevelProtos.Sort proto) throws DeserializationError {
    return new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel()));
  }


  // Parameters

  DependentLink readParameters(List<ExpressionProtos.Telescope> protos) throws DeserializationError {
    DependentLink first = null;
    DependentLink cur = null;
    for (ExpressionProtos.Telescope proto : protos) {
      List<String> unfixedNames = new ArrayList<>(proto.getNameList().size());
      unfixedNames.addAll(proto.getNameList().stream().map(name -> name.isEmpty() ? null : name).collect(Collectors.toList()));
      Expression expr = readExpr(proto.getType());
      Type type = expr instanceof Type ? (Type) expr : new TypeExpression(expr, readSort(proto.getSort()));
      DependentLink tele = ExpressionFactory.param(!proto.getIsNotExplicit(), unfixedNames, type);
      for (DependentLink link = tele; link.hasNext(); link = link.getNext()) {
        registerBinding(link);
      }
      if (first == null) {
        cur = first = tele;
      } else {
        cur.setNext(tele);
        cur = tele;
      }
    }
    return first != null ? first : EmptyDependentLink.getInstance();
  }

  SingleDependentLink readSingleParameter(ExpressionProtos.Telescope proto) throws DeserializationError {
    List<String> unfixedNames = new ArrayList<>(proto.getNameList().size());
    unfixedNames.addAll(proto.getNameList().stream().map(name -> name.isEmpty() ? null : name).collect(Collectors.toList()));
    Expression expr = readExpr(proto.getType());
    Type type = expr instanceof Type ? (Type) expr : new TypeExpression(expr, readSort(proto.getSort()));
    SingleDependentLink tele = ExpressionFactory.singleParam(!proto.getIsNotExplicit(), unfixedNames, type);
    for (DependentLink link = tele; link.hasNext(); link = link.getNext()) {
      registerBinding(link);
    }
    return tele;
  }

  TypedDependentLink readParameter(ExpressionProtos.SingleParameter proto) throws DeserializationError {
    Expression expr = readExpr(proto.getType());
    Type type = expr instanceof Type ? (Type) expr : new TypeExpression(expr, readSort(proto.getSort()));
    TypedDependentLink link = new TypedDependentLink(!proto.getIsNotExplicit(), proto.getName(), type, EmptyDependentLink.getInstance());
    registerBinding(link);
    return link;
  }


  // FieldSet

  FieldSet readFieldSet(ExpressionProtos.FieldSet proto) throws DeserializationError {
    FieldSet result = new FieldSet();
    for (int classFieldRef : proto.getClassFieldRefList()) {
      result.addField(myCalltargetProvider.getCalltarget(classFieldRef, ClassField.class), null);
    }
    for (Map.Entry<Integer, ExpressionProtos.FieldSet.Implementation> entry : proto.getImplementationsMap().entrySet()) {
      final TypedDependentLink thisParam;
      if (entry.getValue().hasThisParam()) {
        thisParam = readParameter(entry.getValue().getThisParam());
      } else {
        thisParam = null;
      }
      FieldSet.Implementation impl = new FieldSet.Implementation(thisParam, readExpr(entry.getValue().getTerm()));
      result.implementField(myCalltargetProvider.getCalltarget(entry.getKey(), ClassField.class), impl);
    }
    result.setSorts(readSort(proto.getSort()));
    return result;
  }


  // Expressions and ElimTrees

  ElimTreeNode readElimTree(ExpressionProtos.ElimTreeNode proto) throws DeserializationError {
    switch (proto.getKindCase()) {
      case BRANCH:
        return readBranch(proto.getBranch());
      case LEAF:
        return readLeaf(proto.getLeaf());
      case EMPTY:
        return readEmpty(proto.getEmpty());
      default:
        throw new DeserializationError("Unknown ElimTreeNode kind: " + proto.getKindCase());
    }
  }

  Expression readExpr(ExpressionProtos.Expression proto) throws DeserializationError {
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
      case LET_CLAUSE_CALL:
        return readLetClauseCall(proto.getLetClauseCall());
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
      case FIELD_CALL:
        return readFieldCall(proto.getFieldCall());
      default:
        throw new DeserializationError("Unknown Expression kind: " + proto.getKindCase());
    }
  }

  private List<Expression> readExprList(List<ExpressionProtos.Expression> protos) throws DeserializationError {
    List<Expression> result = new ArrayList<>();
    for (ExpressionProtos.Expression proto : protos) {
      result.add(readExpr(proto));
    }
    return result;
  }


  private AppExpression readApp(ExpressionProtos.Expression.App proto) throws DeserializationError {
    return new AppExpression(readExpr(proto.getFunction()), readExprList(proto.getArgumentList()));
  }

  private FunCallExpression readFunCall(ExpressionProtos.Expression.FunCall proto) throws DeserializationError {
    return new FunCallExpression(myCalltargetProvider.getCalltarget(proto.getFunRef(), FunctionDefinition.class), new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readExprList(proto.getArgumentList()));
  }

  private LetClauseCallExpression readLetClauseCall(ExpressionProtos.Expression.LetClauseCall proto) throws DeserializationError {
    return new LetClauseCallExpression((LetClause) readBindingRef(proto.getLetClauseRef()), readExprList(proto.getArgumentList()));
  }

  private ConCallExpression readConCall(ExpressionProtos.Expression.ConCall proto) throws DeserializationError {
    return new ConCallExpression(myCalltargetProvider.getCalltarget(proto.getConstructorRef(), Constructor.class), new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())),
        readExprList(proto.getDatatypeArgumentList()), readExprList(proto.getArgumentList()));
  }

  private DataCallExpression readDataCall(ExpressionProtos.Expression.DataCall proto) throws DeserializationError {
    return new DataCallExpression(myCalltargetProvider.getCalltarget(proto.getDataRef(), DataDefinition.class), new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readExprList(proto.getArgumentList()));
  }

  private ClassCallExpression readClassCall(ExpressionProtos.Expression.ClassCall proto) throws DeserializationError {
    return new ClassCallExpression(myCalltargetProvider.getCalltarget(proto.getClassRef(), ClassDefinition.class), new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readFieldSet(proto.getFieldSet()));
  }

  private ReferenceExpression readReference(ExpressionProtos.Expression.Reference proto) throws DeserializationError {
    return new ReferenceExpression((Binding)readBindingRef(proto.getBindingRef()));
  }

  private LamExpression readLam(ExpressionProtos.Expression.Lam proto) throws DeserializationError {
    return new LamExpression(readLevel(proto.getPLevel()), readSingleParameter(proto.getParam()), readExpr(proto.getBody()));
  }

  private PiExpression readPi(ExpressionProtos.Expression.Pi proto) throws DeserializationError {
    return new PiExpression(readLevel(proto.getPLevel()), readSingleParameter(proto.getParam()), readExpr(proto.getCodomain()));
  }

  private UniverseExpression readUniverse(ExpressionProtos.Expression.Universe proto) throws DeserializationError {
    return new UniverseExpression(readSort(proto.getSort()));
  }

  private ErrorExpression readError(ExpressionProtos.Expression.Error proto) throws DeserializationError {
    final Expression expr;
    if (proto.hasExpression()) {
      expr = readExpr(proto.getExpression());
    } else {
      expr = null;
    }
    return new ErrorExpression(expr, null);
  }

  private TupleExpression readTuple(ExpressionProtos.Expression.Tuple proto) throws DeserializationError {
    return new TupleExpression(readExprList(proto.getFieldList()), readSigma(proto.getType()));
  }

  private SigmaExpression readSigma(ExpressionProtos.Expression.Sigma proto) throws DeserializationError {
    return new SigmaExpression(new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel())), readParameters(proto.getParamList()));
  }

  private ProjExpression readProj(ExpressionProtos.Expression.Proj proto) throws DeserializationError {
    return new ProjExpression(readExpr(proto.getExpression()), proto.getField());
  }

  private NewExpression readNew(ExpressionProtos.Expression.New proto) throws DeserializationError {
    return new NewExpression(readClassCall(proto.getClassCall()));
  }

  private LetExpression readLet(ExpressionProtos.Expression.Let proto) throws DeserializationError {
    List<LetClause> clauses = new ArrayList<>();
    for (ExpressionProtos.Expression.Let.Clause cProto : proto.getClauseList()) {
      List<Level> pLevels = new ArrayList<>(cProto.getPLevelCount());
      for (LevelProtos.Level pLevel : cProto.getPLevelList()) {
        pLevels.add(readLevel(pLevel));
      }
      List<SingleDependentLink> parameters = new ArrayList<>(cProto.getParamCount());
      for (ExpressionProtos.Telescope telescope : cProto.getParamList()) {
        parameters.add(readSingleParameter(telescope));
      }
      LetClause clause = new LetClause(cProto.getName(), pLevels, parameters, readExpr(cProto.getResultType()), readElimTree(cProto.getElimTree()));
      registerBinding(clause);
      clauses.add(clause);
    }
    return new LetExpression(clauses, readExpr(proto.getExpression()));
  }

  private FieldCallExpression readFieldCall(ExpressionProtos.Expression.FieldCall proto) throws DeserializationError {
    return new FieldCallExpression(myCalltargetProvider.getCalltarget(proto.getFieldRef(), ClassField.class), readExpr(proto.getExpression()));
  }


  private BranchElimTreeNode readBranch(ExpressionProtos.ElimTreeNode.Branch proto) throws DeserializationError {
    List<Binding> contextTail = new ArrayList<>();
    for (int ref : proto.getContextTailItemRefList()) {
      contextTail.add((Binding)readBindingRef(ref));
    }
    BranchElimTreeNode result = new BranchElimTreeNode((Binding)readBindingRef(proto.getReferenceRef()), contextTail);
    for (Map.Entry<Integer, ExpressionProtos.ElimTreeNode.ConstructorClause> entry : proto.getConstructorClausesMap().entrySet()) {
      ExpressionProtos.ElimTreeNode.ConstructorClause cProto = entry.getValue();
      DependentLink constructorParams = readParameters(cProto.getParamList());
      List<TypedBinding> tailBindings = new ArrayList<>();
      for (ExpressionProtos.Binding.TypedBinding bProto : cProto.getTailBindingList()) {
        tailBindings.add(readTypedBinding(bProto));
      }
      result.addClause(myCalltargetProvider.getCalltarget(entry.getKey(), Constructor.class), constructorParams, tailBindings, readElimTree(cProto.getChild()));
    }
    if (proto.hasOtherwiseClause()) {
      result.addOtherwiseClause(readElimTree(proto.getOtherwiseClause()));
    }
    return result;
  }

  private LeafElimTreeNode readLeaf(ExpressionProtos.ElimTreeNode.Leaf proto) throws DeserializationError {
    List<Binding> context = new ArrayList<>();
    for (int ref : proto.getMatchedRefList()) {
      context.add((Binding)readBindingRef(ref));
    }
    LeafElimTreeNode result = new LeafElimTreeNode(proto.getArrowLeft() ? Abstract.Definition.Arrow.LEFT : Abstract.Definition.Arrow.RIGHT, readExpr(proto.getExpr()));
    result.setMatched(context);
    return result;
  }

  private EmptyElimTreeNode readEmpty(ExpressionProtos.ElimTreeNode.Empty proto) {
    return EmptyElimTreeNode.getInstance();
  }
}
