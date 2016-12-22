package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.binding.*;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.PiTypeOmega;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.*;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class DefinitionDeserialization {
  private final CalltargetProvider myCalltargetProvider;
  private final List<Binding> myBindings = new ArrayList<>();  // de Bruijn indices
  private final List<LevelBinding> myLvlBindings = new ArrayList<>();  // de Bruijn indices for level bindings

  DefinitionDeserialization(CalltargetProvider calltargetProvider) {
    myCalltargetProvider = calltargetProvider;
  }


  // Bindings

  private RollbackBindings checkpointBindings() {
    return new RollbackBindings(myBindings.size(), myLvlBindings.size());
  }

  private class RollbackBindings implements AutoCloseable {
    private final int myTargetSize;
    private final int myLvlTargetSize;

    private RollbackBindings(int targetSize, int lvlTargetSize) {
      myTargetSize = targetSize;
      myLvlTargetSize = lvlTargetSize;
    }

    @Override
    public void close() {
      for (int i = myBindings.size() - 1; i >= myTargetSize; i--) {
        myBindings.remove(i);
      }
      for (int i = myLvlBindings.size() - 1; i >= myLvlTargetSize; i--) {
        myLvlBindings.remove(i);
      }
    }
  }

  private void registerBinding(Binding binding) {
    myBindings.add(binding);
  }

  private void registerLevelBinding(LevelBinding binding) {
    myLvlBindings.add(binding);
  }

  TypedBinding readTypedBinding(ExpressionProtos.Binding.TypedBinding proto) throws DeserializationError {
    TypedBinding typedBinding = new TypedBinding(proto.getName(), readType(proto.getType()));
    registerBinding(typedBinding);
    return typedBinding;
  }

  LevelBinding readLevelBinding(ExpressionProtos.Binding.LevelBinding proto) throws DeserializationError {
    LevelBinding lvlBinding = new LevelBinding(proto.getName(), proto.getType() == ExpressionProtos.Binding.LevelBinding.LvlType.PLVL ? LevelVariable.LvlType.PLVL : LevelVariable.LvlType.HLVL);
    registerLevelBinding(lvlBinding);
    return lvlBinding;
  }

  private Variable readBindingRef(int index, boolean isLevel) throws DeserializationError {
    if (index == 0) {
      return null;
    } else {
      Variable binding = isLevel ? myLvlBindings.get(index - 1) : myBindings.get(index - 1);
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
        DependentLink param1 = readParameter(proto.getAnyConstructor().getVar());
        if (prev[0] != null) prev[0].setNext(param1);
        prev[0] = param1;
        return new AnyConstructorPattern(param1);
      case CONSTRUCTOR:
        return new ConstructorPattern(myCalltargetProvider.<Constructor>getCalltarget(proto.getConstructor().getConstructorRef()), readPatterns(proto.getConstructor().getPatterns(), prev));
      default:
        throw new DeserializationError("Unknown Pattern kind: " + proto.getKindCase());
    }
  }



  // Sorts and levels

  private Level readLevel(LevelProtos.Level proto) throws DeserializationError {
    Variable var = readBindingRef(proto.getBindingRef(), true);
    int constant = proto.getConstant();
    if (var == null && constant == -1) {
      return Level.INFINITY;
    } else {
      return new Level((LevelBinding)var, constant);
    }
  }

  private LevelMax readLevelMax(LevelProtos.LevelMax proto) throws DeserializationError {
    LevelMax result = new LevelMax();
    for (LevelProtos.Level levelProto : proto.getLevelList()) {
      result.add(readLevel(levelProto));
    }
    return result;
  }

  private Sort readSort(LevelProtos.Sort proto) throws DeserializationError {
    return new Sort(readLevel(proto.getPLevel()), readLevel(proto.getHLevel()));
  }

  private SortMax readSortMax(LevelProtos.SortMax proto) throws DeserializationError {
    return new SortMax(readLevelMax(proto.getPLevel()), readLevelMax(proto.getHLevel()));
  }

  private LevelArguments readPolyArguments(List<LevelProtos.Level> protos) throws DeserializationError {
    List<Level> levels = new ArrayList<>();
    for (LevelProtos.Level proto : protos) {
      levels.add(readLevel(proto));
    }
    return new LevelArguments(levels);
  }


  // Parameters

  DependentLink readParameters(List<ExpressionProtos.Telescope> protos) throws DeserializationError {
    DependentLink first = null;
    DependentLink cur = null;
    for (ExpressionProtos.Telescope proto : protos) {
      List<String> unfixedNames = new ArrayList<>(proto.getNameList().size());
      for (String name : proto.getNameList()) {
        unfixedNames.add(name.isEmpty() ? null : name);
      }
      DependentLink tele = ExpressionFactory.param(!proto.getIsNotExplicit(), unfixedNames, readType(proto.getType()));
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

  DependentLink readParameter(ExpressionProtos.SingleParameter proto) throws DeserializationError {
    TypedDependentLink link = new TypedDependentLink(!proto.getIsNotExplicit(), proto.getName(), readType(proto.getType()), EmptyDependentLink.getInstance());
    registerBinding(link);
    return link;
  }


  // FieldSet

  FieldSet readFieldSet(ExpressionProtos.FieldSet proto) throws DeserializationError {
    FieldSet result = new FieldSet();
    for (int classFieldRef : proto.getClassFieldRefList()) {
      result.addField(myCalltargetProvider.<ClassField>getCalltarget(classFieldRef));
    }
    for (Map.Entry<Integer, ExpressionProtos.FieldSet.Implementation> entry : proto.getImplementationsMap().entrySet()) {
      final DependentLink thisParam;
      if (entry.getValue().hasThisParam()) {
        thisParam = readParameter(entry.getValue().getThisParam());
      } else {
        thisParam = null;
      }
      FieldSet.Implementation impl = new FieldSet.Implementation(thisParam, readExpr(entry.getValue().getTerm()));
      result.implementField(myCalltargetProvider.<ClassField>getCalltarget(entry.getKey()), impl);
    }
    return result;
  }


  // Types, Expressions and ElimTrees

  TypeMax readTypeMax(ExpressionProtos.Type proto) throws DeserializationError {
    switch (proto.getKindCase()) {
      case PI_UNIVERSE:
        DependentLink parameters = readParameters(proto.getPiUniverse().getParamList());
        SortMax sorts = readSortMax(proto.getPiUniverse().getSorts());
        if (sorts.isOmega()) {
          return new PiTypeOmega(parameters);
        } else {
          return new PiUniverseType(parameters, sorts);
        }
      case EXPR:
        return readExpr(proto.getExpr());
      default:
        throw new DeserializationError("Unknown TypeMax kind: " + proto.getKindCase());
    }
  }

  private Type readType(ExpressionProtos.Type proto) throws DeserializationError {
    TypeMax typeMax = readTypeMax(proto);
    if (typeMax instanceof Type) {
      return (Type) typeMax;
    } else {
      throw new DeserializationError("Deserialized Type is not a TypeMax");
    }
  }

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
    return new FunCallExpression(myCalltargetProvider.<FunctionDefinition>getCalltarget(proto.getFunRef()), readPolyArguments(proto.getPolyArgumentsList()), readExprList(proto.getArgumentList()));
  }

  private ConCallExpression readConCall(ExpressionProtos.Expression.ConCall proto) throws DeserializationError {
    return new ConCallExpression(myCalltargetProvider.<Constructor>getCalltarget(proto.getConstructorRef()), readPolyArguments(proto.getPolyArgumentsList()),
        readExprList(proto.getDatatypeArgumentList()), readExprList(proto.getArgumentList()));
  }

  private DataCallExpression readDataCall(ExpressionProtos.Expression.DataCall proto) throws DeserializationError {
    return new DataCallExpression(myCalltargetProvider.<DataDefinition>getCalltarget(proto.getDataRef()), readPolyArguments(proto.getPolyArgumentsList()), readExprList(proto.getArgumentList()));
  }

  private ClassCallExpression readClassCall(ExpressionProtos.Expression.ClassCall proto) throws DeserializationError {
    return new ClassCallExpression(myCalltargetProvider.<ClassDefinition>getCalltarget(proto.getClassRef()), readPolyArguments(proto.getPolyArgumentsList()), readFieldSet(proto.getFieldSet()));
  }

  private ReferenceExpression readReference(ExpressionProtos.Expression.Reference proto) throws DeserializationError {
    return new ReferenceExpression((Binding)readBindingRef(proto.getBindingRef(), false));
  }

  private LamExpression readLam(ExpressionProtos.Expression.Lam proto) throws DeserializationError {
    return new LamExpression(readParameters(proto.getParamList()), readExpr(proto.getBody()));
  }

  private PiExpression readPi(ExpressionProtos.Expression.Pi proto) throws DeserializationError {
    return new PiExpression(readParameters(proto.getParamList()), readExpr(proto.getCodomain()));
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
    return new SigmaExpression(readParameters(proto.getParamList()));
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
      LetClause clause = new LetClause(cProto.getName(), readParameters(cProto.getParamList()), readExpr(cProto.getResultType()), readElimTree(cProto.getElimTree()));
      registerBinding(clause);
      clauses.add(clause);
    }
    return new LetExpression(clauses, readExpr(proto.getExpression()));
  }

  private FieldCallExpression readFieldCall(ExpressionProtos.Expression.FieldCall proto) throws DeserializationError {
    return new FieldCallExpression(myCalltargetProvider.<ClassField>getCalltarget(proto.getFieldRef()), readExpr(proto.getExpression()));
  }


  private BranchElimTreeNode readBranch(ExpressionProtos.ElimTreeNode.Branch proto) throws DeserializationError {
    List<Binding> contextTail = new ArrayList<>();
    for (int ref : proto.getContextTailItemRefList()) {
      contextTail.add((Binding)readBindingRef(ref, false));
    }
    BranchElimTreeNode result = new BranchElimTreeNode((Binding)readBindingRef(proto.getReferenceRef(), false), contextTail);
    for (Map.Entry<Integer, ExpressionProtos.ElimTreeNode.ConstructorClause> entry : proto.getConstructorClausesMap().entrySet()) {
      ExpressionProtos.ElimTreeNode.ConstructorClause cProto = entry.getValue();
      List<LevelBinding> polyParams = new ArrayList<>();
      for (ExpressionProtos.Binding.LevelBinding levelBinding : cProto.getPolyParamList()) {
        polyParams.add(readLevelBinding(levelBinding));
      }
      DependentLink constructorParams = readParameters(cProto.getParamList());
      List<TypedBinding> tailBindings = new ArrayList<>();
      for (ExpressionProtos.Binding.TypedBinding bProto : cProto.getTailBindingList()) {
        tailBindings.add(readTypedBinding(bProto));
      }
      result.addClause(myCalltargetProvider.<Constructor>getCalltarget(entry.getKey()), constructorParams, polyParams, tailBindings, readElimTree(cProto.getChild()));
    }
    if (proto.hasOtherwiseClause()) {
      result.addOtherwiseClause(readElimTree(proto.getOtherwiseClause()));
    }
    return result;
  }

  private LeafElimTreeNode readLeaf(ExpressionProtos.ElimTreeNode.Leaf proto) throws DeserializationError {
    List<Binding> context = new ArrayList<>();
    for (int ref : proto.getMatchedRefList()) {
      context.add((Binding)readBindingRef(ref, false));
    }
    LeafElimTreeNode result = new LeafElimTreeNode(proto.getArrowLeft() ? Abstract.Definition.Arrow.LEFT : Abstract.Definition.Arrow.RIGHT, readExpr(proto.getExpr()));
    result.setMatched(context);
    return result;
  }

  private EmptyElimTreeNode readEmpty(ExpressionProtos.ElimTreeNode.Empty proto) {
    return EmptyElimTreeNode.getInstance();
  }
}
