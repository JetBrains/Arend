package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude extends SimpleNamespace {
  public static ModuleID moduleID = new ModuleID() {
    @Override
    public ModulePath getModulePath() {
      return new ModulePath("Prelude");
    }
  };

  public static ClassDefinition PRELUDE_CLASS;

  public static SimpleNamespace PRELUDE = new Prelude();

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT, ABSTRACT;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static DataDefinition CNAT;
  public static Constructor FIN, INF;

  public static DataDefinition LVL;

  public static FunctionDefinition MAX_NAT;
  public static FunctionDefinition PRED_NAT;
  public static FunctionDefinition MAX_CNAT;
  public static FunctionDefinition SUC_CNAT;
  public static FunctionDefinition PRED_CNAT;

  public static FunctionDefinition COERCE;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition AT;
  public static FunctionDefinition ISO;

  public static DataDefinition PROP_TRUNC;
  public static DataDefinition SET_TRUNC;

  public static Constructor PROP_TRUNC_PATH_CON;
  public static Constructor SET_TRUNC_PATH_CON;

  static {
    PRELUDE_CLASS = new ClassDefinition("Prelude", PRELUDE, EmptyNamespace.INSTANCE);

    /* Nat, zero, suc */
    DefinitionBuilder.Data nat = new DefinitionBuilder.Data(PRELUDE, "Nat", Abstract.Binding.DEFAULT_PRECEDENCE, new SortMax(Sort.SetOfLevel(0)), EmptyDependentLink.getInstance());
    NAT = nat.definition();
    ZERO = nat.addConstructor("zero", Abstract.Binding.DEFAULT_PRECEDENCE, EmptyDependentLink.getInstance());
    SUC = nat.addConstructor("suc", Abstract.Binding.DEFAULT_PRECEDENCE, param(DataCall(NAT)));

    /* Lvl */
    DefinitionBuilder.Data lvl = new DefinitionBuilder.Data(PRELUDE, "Lvl", Abstract.Binding.DEFAULT_PRECEDENCE, new SortMax(Sort.SetOfLevel(0)), EmptyDependentLink.getInstance());
    LVL = lvl.definition();

    /* CNat, inf, fin */
    DefinitionBuilder.Data cnat = new DefinitionBuilder.Data(PRELUDE, "CNat", Abstract.Binding.DEFAULT_PRECEDENCE, new SortMax(Sort.SetOfLevel(0)), EmptyDependentLink.getInstance());
    CNAT = cnat.definition();
    INF = cnat.addConstructor("inf", Abstract.Binding.DEFAULT_PRECEDENCE, EmptyDependentLink.getInstance());
    FIN = cnat.addConstructor("fin", Abstract.Binding.DEFAULT_PRECEDENCE, param("n", DataCall(NAT)));

    /* maxNat */
    DependentLink maxNatParameter1 = param(DataCall(NAT));
    DependentLink maxNatParameter2 = param(DataCall(NAT));
    maxNatParameter1.setNext(maxNatParameter2);
    DependentLink sucNatParameter = param("n", DataCall(NAT));
    DependentLink sucNatParameterPrime = param("n'", DataCall(NAT));
    MAX_NAT = new DefinitionBuilder.Function(PRELUDE, "maxNat", Abstract.Binding.DEFAULT_PRECEDENCE, maxNatParameter1, DataCall(NAT), null).definition();
    ElimTreeNode maxNatElimTree = top(maxNatParameter1, branch(maxNatParameter1, tail(maxNatParameter2),
            clause(ZERO, EmptyDependentLink.getInstance(), branch(maxNatParameter2, tail(),
                    clause(ZERO, EmptyDependentLink.getInstance(), ConCall(ZERO)),
                    clause(SUC, sucNatParameter, Apps(ConCall(SUC), Reference(sucNatParameter))))),
            clause(SUC, sucNatParameter, branch(maxNatParameter2, tail(),
                    clause(ZERO, EmptyDependentLink.getInstance(), Apps(ConCall(SUC), Reference(sucNatParameter))),
                    clause(SUC, sucNatParameterPrime, Apps(ConCall(SUC), Apps(FunCall(MAX_NAT), Reference(sucNatParameter), Reference(sucNatParameterPrime))))))));
    MAX_NAT.setElimTree(maxNatElimTree);

    /* predNat */
    DependentLink predNatParameter = param("n", DataCall(NAT));
    PRED_NAT = new DefinitionBuilder.Function(PRELUDE, "predNat", Abstract.Binding.DEFAULT_PRECEDENCE, predNatParameter, DataCall(NAT), null).definition();
    ElimTreeNode predNatElimTree = top(predNatParameter, branch(predNatParameter, tail(),
            clause(ZERO, EmptyDependentLink.getInstance(), Zero()),
            clause(SUC, sucNatParameter, Reference(sucNatParameter))));
    PRED_NAT.setElimTree(predNatElimTree);

    /* maxCNat */
    DependentLink maxCNatParameter1 = param(DataCall(CNAT));
    DependentLink maxCNatParameter2 = param(DataCall(CNAT));
    maxCNatParameter1.setNext(maxCNatParameter2);
    DependentLink finCNatParameter = param("n", DataCall(NAT));
    DependentLink finCNatParameterPrime = param("n'", DataCall(NAT));
    ElimTreeNode maxCNatElimTree = top(maxCNatParameter1, branch(maxCNatParameter1, tail(maxCNatParameter2),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, branch(maxCNatParameter2, tail(),
                    clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
                    // clause(FIN, finCNatParameterPrime, Apps(ConCall(FIN), Apps(FunCall(MAX_NAT), Reference(finCNatParameter), Reference(finCNatParameterPrime)))),
                    clause(FIN, finCNatParameterPrime, Apps(ConCall(FIN), Apps(FunCall(MAX_NAT), Reference(finCNatParameter), Reference(finCNatParameterPrime))))))));
    MAX_CNAT = new DefinitionBuilder.Function(PRELUDE, "maxCNat", Abstract.Binding.DEFAULT_PRECEDENCE, maxCNatParameter1, DataCall(CNAT), maxCNatElimTree).definition();

    /* sucCNat */
    DependentLink sucCNatParameter = param(DataCall(CNAT));
    ElimTreeNode sucCNatElimTree = top(sucCNatParameter, branch(sucCNatParameter, tail(),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, Apps(ConCall(FIN), Apps(ConCall(SUC), Reference(finCNatParameter))))));
    SUC_CNAT = new DefinitionBuilder.Function(PRELUDE, "sucCNat", Abstract.Binding.DEFAULT_PRECEDENCE, sucCNatParameter, DataCall(CNAT), sucCNatElimTree).definition();

    /* predCNat */
    DependentLink predCNatParameter = param("n", DataCall(CNAT));
    PRED_CNAT = new DefinitionBuilder.Function(PRELUDE, "predCNat", Abstract.Binding.DEFAULT_PRECEDENCE, predNatParameter, DataCall(CNAT), null).definition();
    ElimTreeNode predCNatElimTree = top(predCNatParameter, branch(predCNatParameter, tail(),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, Apps(ConCall(FIN), Apps(FunCall(PRED_NAT), Reference(finCNatParameter))))));
    PRED_CNAT.setElimTree(predCNatElimTree);

    /* I, left, right */
    DefinitionBuilder.Data interval = new DefinitionBuilder.Data(PRELUDE, "I", Abstract.Binding.DEFAULT_PRECEDENCE, new SortMax(Sort.PROP), EmptyDependentLink.getInstance());
    INTERVAL = interval.definition();
    LEFT = interval.addConstructor("left", Abstract.Binding.DEFAULT_PRECEDENCE, EmptyDependentLink.getInstance());
    RIGHT = interval.addConstructor("right", Abstract.Binding.DEFAULT_PRECEDENCE, EmptyDependentLink.getInstance());
    ABSTRACT = interval.addConstructor("<abstract>", Abstract.Binding.DEFAULT_PRECEDENCE, EmptyDependentLink.getInstance());

    /* Path */
    Level PathLp = new Level(new TypedBinding("lp", Lvl()));
    Level PathLh = new Level(new TypedBinding("lh", CNat()));
    DependentLink PathParameter1 = param("A", Pi(param(Interval()), Universe(PathLp, PathLh)));
    DependentLink PathParameter2 = param("a", Apps(Reference(PathParameter1), Left()));
    DependentLink PathParameter3 = param("a'", Apps(Reference(PathParameter1), Right()));
    PathParameter1.setNext(PathParameter2);
    PathParameter2.setNext(PathParameter3);
    DefinitionBuilder.Data path = new DefinitionBuilder.Data(PRELUDE, "Path", Abstract.Binding.DEFAULT_PRECEDENCE, new SortMax(new Sort(PathLp, PathLh)), PathParameter1, Arrays.asList((Binding) PathLp.getVar(), (Binding) PathLh.getVar()));
    PATH = path.definition();

    /* path */
    DependentLink piParam = param("i", Interval());
    DependentLink pathParameter = param(Pi(piParam, Apps(Reference(PathParameter1), Reference(piParam))));
    PATH_CON = path.addConstructor("path", Abstract.Binding.DEFAULT_PRECEDENCE, pathParameter);

    /* = */
    Level pathInfixLp = new Level(new TypedBinding("lp", Lvl()));
    Level pathInfixLh = new Level(new TypedBinding("lh", CNat()));
    DependentLink pathInfixParameter1 = param(false, "A", Universe(pathInfixLp, pathInfixLh));
    DependentLink pathInfixParameter2 = param(true, vars("a", "a'"), Reference(pathInfixParameter1));
    pathInfixParameter1.setNext(pathInfixParameter2);
    Expression pathInfixTerm = DataCall(PATH, pathInfixLp, pathInfixLh)
        .addArgument(Lam(param("_", Interval()), Reference(pathInfixParameter1)), AppExpression.DEFAULT)
        .addArgument(Reference(pathInfixParameter2), AppExpression.DEFAULT)
        .addArgument(Reference(pathInfixParameter2.getNext()), AppExpression.DEFAULT);
    PATH_INFIX = new DefinitionBuilder.Function(PRELUDE, "=", new Abstract.Binding.Precedence(Abstract.Binding.Associativity.NON_ASSOC, (byte) 0), pathInfixParameter1, Universe(pathInfixLp, pathInfixLh), top(pathInfixParameter1, leaf(pathInfixTerm)), Arrays.asList((Binding) pathInfixLp.getVar(), (Binding) pathInfixLh.getVar())).definition();

    /* @ */
    Level atLp = new Level(new TypedBinding("lp", Lvl()));
    Level atLh = new Level(new TypedBinding("lh", CNat()));
    DependentLink atParameter1 = param(false, "A", Pi(param(Interval()), Universe(atLp, atLh)));
    DependentLink atParameter2 = param(false, "a", Apps(Reference(atParameter1), Left()));
    DependentLink atParameter3 = param(false, "a'", Apps(Reference(atParameter1), Right()));
    DependentLink atParameter4 = param("p", Apps(DataCall(PATH, atLp, atLh), Reference(atParameter1), Reference(atParameter2), Reference(atParameter3)));
    DependentLink atParameter5 = param("i", Interval());
    atParameter1.setNext(atParameter2);
    atParameter2.setNext(atParameter3);
    atParameter3.setNext(atParameter4);
    atParameter4.setNext(atParameter5);
    DependentLink atPath = param("f", pathParameter.getType());
    Expression atResultType = Apps(Reference(atParameter1), Reference(atParameter5));
    ElimTreeNode atElimTree = top(atParameter1, branch(atParameter5, tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Reference(atParameter2)),
        clause(RIGHT, EmptyDependentLink.getInstance(), Reference(atParameter3)),
        clause(branch(atParameter4, tail(atParameter5),
            clause(PATH_CON, atPath, Apps(Reference(atPath), Reference(atParameter5)))))
    ));
    AT = new DefinitionBuilder.Function(PRELUDE, "@", new Abstract.Binding.Precedence(Abstract.Binding.Associativity.LEFT_ASSOC, (byte) 9), atParameter1, atResultType, atElimTree, Arrays.asList((Binding) atLp.getVar(), (Binding) atLh.getVar())).definition();

    /* coe */
    Level coerceLp = new Level(new TypedBinding("lp", Lvl()));
    Level coerceLh = new Level(new TypedBinding("lh", CNat()));
    DependentLink coerceParameter1 = param("type", Pi(param(Interval()), Universe(coerceLp, coerceLh)));
    DependentLink coerceParameter2 = param("elem", Apps(Reference(coerceParameter1), Left()));
    DependentLink coerceParameter3 = param("point", Interval());
    coerceParameter1.setNext(coerceParameter2);
    coerceParameter2.setNext(coerceParameter3);
    ElimTreeNode coerceElimTreeNode = top(coerceParameter1, branch(coerceParameter3, tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(coerceParameter2))));
    COERCE = new DefinitionBuilder.Function(PRELUDE, "coe", Abstract.Binding.DEFAULT_PRECEDENCE, coerceParameter1, Apps(Reference(coerceParameter1), Reference(coerceParameter3)), coerceElimTreeNode, Arrays.asList((Binding) coerceLp.getVar(), (Binding) coerceLh.getVar())).definition();

    /* iso */
    Level isoLp = new Level(new TypedBinding("lp", Lvl()));
    Level isoLh = new Level(new TypedBinding("lh", CNat()));
    DependentLink isoParameter1 = param(false, vars("A", "B"), Universe(isoLp, isoLh));
    DependentLink isoParameter2 = param("f", Pi(param(Reference(isoParameter1)), Reference(isoParameter1.getNext())));
    DependentLink isoParameter3 = param("g", Pi(param(Reference(isoParameter1.getNext())), Reference(isoParameter1)));
    DependentLink piParamA = param("a", Reference(isoParameter1));
    DependentLink piParamB = param("b", Reference(isoParameter1.getNext()));
    Expression isoParameters4type = FunCall(PATH_INFIX, isoLp, isoLh)
        .addArgument(Reference(isoParameter1), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Apps(Reference(isoParameter3), Apps(Reference(isoParameter2), Reference(piParamA))), AppExpression.DEFAULT)
        .addArgument(Reference(piParamA), AppExpression.DEFAULT);
    DependentLink isoParameter4 = param("linv", Pi(piParamA, isoParameters4type));
    Expression isoParameters5type = FunCall(PATH_INFIX, isoLp, isoLh)
        .addArgument(Reference(isoParameter1.getNext()), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Apps(Reference(isoParameter2), Apps(Reference(isoParameter3), Reference(piParamB))), AppExpression.DEFAULT)
        .addArgument(Reference(piParamB), AppExpression.DEFAULT);
    DependentLink isoParameter5 = param("rinv", Pi(piParamB, isoParameters5type));
    DependentLink isoParameter6 = param("i", Interval());
    isoParameter1.setNext(isoParameter2);
    isoParameter2.setNext(isoParameter3);
    isoParameter3.setNext(isoParameter4);
    isoParameter4.setNext(isoParameter5);
    isoParameter5.setNext(isoParameter6);
    Expression isoResultType = Universe(isoLp, isoLh);
    ElimTreeNode isoElimTree = top(isoParameter1, branch(isoParameter5, tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter1)),
        clause(RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter1.getNext()))
    ));
    ISO = new DefinitionBuilder.Function(PRELUDE, "iso", Abstract.Binding.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree, Arrays.asList((Binding) isoLp.getVar(), (Binding) isoLh.getVar())).definition();

    /* TrP, inP */
    Level truncPLp = new Level(new TypedBinding("lp", Lvl()));
    Level truncPLh = new Level(new TypedBinding("lh", CNat()));
    DependentLink truncPParameter = param("A", Universe(truncPLp, truncPLh));
    DefinitionBuilder.Data propTrunc = new DefinitionBuilder.Data(PRELUDE, "TrP", Abstract.Binding.DEFAULT_PRECEDENCE, new SortMax(Sort.PROP), truncPParameter, Arrays.asList((Binding) truncPLp.getVar(), (Binding) truncPLh.getVar()));
    PROP_TRUNC = propTrunc.definition();
    propTrunc.addConstructor("inP", Abstract.Binding.DEFAULT_PRECEDENCE, param("a", Reference(truncPParameter)));

    /* truncP */
    Expression propTruncConParameterType = DataCall(PROP_TRUNC, truncPLp, truncPLh)
        .addArgument(Reference(truncPParameter), AppExpression.DEFAULT);
    DependentLink propTruncConParameter1 = param("a", propTruncConParameterType);
    DependentLink propTruncConParameter2 = param("a'", propTruncConParameterType);
    DependentLink propTruncConParameter3 = param("i", Interval());
    propTruncConParameter1.setNext(propTruncConParameter2);
    propTruncConParameter2.setNext(propTruncConParameter3);
    PROP_TRUNC_PATH_CON = propTrunc.addConstructor("truncP", Abstract.Binding.DEFAULT_PRECEDENCE, propTruncConParameter1);
    Condition propTruncPathCond = new Condition(PROP_TRUNC_PATH_CON, top(propTruncConParameter1, branch(propTruncConParameter3, tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter1)),
        clause(RIGHT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter2)))));
    PROP_TRUNC.addCondition(propTruncPathCond);

    /* TrS, inS */
    Level truncSLp = new Level(new TypedBinding("lp", Lvl()));
    Level truncSLh = new Level(new TypedBinding("lh", CNat()));
    DependentLink truncSParameter = param("A", Universe(truncSLp, truncSLh));
    DefinitionBuilder.Data setTrunc = new DefinitionBuilder.Data(PRELUDE, "TrS", Abstract.Binding.DEFAULT_PRECEDENCE, new SortMax(Sort.SetOfLevel(truncSLp)), truncSParameter, Arrays.asList((Binding) truncSLp.getVar(), (Binding) truncSLh.getVar()));
    SET_TRUNC = setTrunc.definition();
    setTrunc.addConstructor("inS", Abstract.Binding.DEFAULT_PRECEDENCE, param("inS", Reference(truncSParameter)));

    /* truncS */
    Expression setTruncConParameterType = DataCall(SET_TRUNC, truncSLp, truncSLh);
    DependentLink setTruncConParameter1 = param("a", Apps(DataCall(SET_TRUNC, truncSLp, truncSLh), setTruncConParameterType));
    DependentLink setTruncConParameter2 = param("a'", Apps(DataCall(SET_TRUNC, truncSLp, truncSLh), setTruncConParameterType));
    Expression setTruncConParameter3type = FunCall(PATH_INFIX).applyLevelSubst(new LevelSubstitution(truncSLh.getVar(), new Level(0)))
        .addArgument(setTruncConParameterType, EnumSet.noneOf(AppExpression.Flag.class))
        .addArgument(Reference(setTruncConParameter1), AppExpression.DEFAULT)
        .addArgument(Reference(setTruncConParameter2), AppExpression.DEFAULT);
    DependentLink setTruncConParameter3 = param("p", setTruncConParameter3type);
    DependentLink setTruncConParameter4 = param("q", setTruncConParameter3type);
    DependentLink setTruncConParameter5 = param("i", Interval());
    DependentLink setTruncConParameter6 = param("j", Interval());
    setTruncConParameter1.setNext(setTruncConParameter2);
    setTruncConParameter2.setNext(setTruncConParameter3);
    setTruncConParameter3.setNext(setTruncConParameter4);
    setTruncConParameter4.setNext(setTruncConParameter5);
    setTruncConParameter5.setNext(setTruncConParameter6);
    SET_TRUNC_PATH_CON = setTrunc.addConstructor("truncS", Abstract.Binding.DEFAULT_PRECEDENCE, setTruncConParameter1);
    Condition setTruncPathCond = new Condition(SET_TRUNC_PATH_CON, top(setTruncConParameter1, branch(setTruncConParameter6, tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), FunCall(AT, truncSLp, truncSLh).addArgument(Reference(setTruncConParameter3), AppExpression.DEFAULT).addArgument(Reference(setTruncConParameter5), AppExpression.DEFAULT)),
        clause(RIGHT, EmptyDependentLink.getInstance(), FunCall(AT, truncSLp, truncSLh).addArgument(Reference(setTruncConParameter4), AppExpression.DEFAULT).addArgument(Reference(setTruncConParameter5), AppExpression.DEFAULT)),
        clause(branch(setTruncConParameter5, tail(setTruncConParameter6),
            clause(LEFT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter1)),
            clause(RIGHT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter2))))
    )));
    SET_TRUNC.addCondition(setTruncPathCond);
  }

  private Prelude() {
  }

  public static boolean isAt(Definition definition) {
    return AT == definition;
  }

  public static boolean isPathCon(Definition definition) {
    return PATH_CON == definition;
  }

  public static boolean isPath(Definition definition) {
    return PATH == definition;
  }

  public static boolean isIso(Definition definition) {
    return ISO == definition;
  }

  public static boolean isCoe(Definition definition) {
    return COERCE == definition;
  }

  public static boolean isTruncP(Definition definition) {
    return PROP_TRUNC_PATH_CON == definition;
  }

  public static boolean isTruncS(Definition definition) {
    return SET_TRUNC_PATH_CON == definition;
  }

  public static boolean isPolyParam(Abstract.TypeArgument arg) {
    if (arg.getType() instanceof Abstract.DefCallExpression) {
      String typeName = ((Abstract.DefCallExpression) arg.getType()).getName();
      return typeName.equals(LVL.getName()) || typeName.equals(CNAT.getName());
    }
    return false;
  }

  public static DefCallExpression levelTypeByName(String typeName) {
    if (typeName.equals("Lvl")) {
      return Lvl();
    } else if (typeName.equals("CNat")) {
      return CNat();
    }
    return null;
  }

  static class DefinitionBuilder {
    static class Data {
      private final SimpleNamespace myParentNs;
      private final DataDefinition myDefinition;
      private final SimpleNamespace myNs = new SimpleNamespace();

      Data(SimpleNamespace parentNs, String name, Abstract.Binding.Precedence precedence, SortMax sorts, DependentLink parameters, List<Binding> polyParams) {
        myParentNs = parentNs;
        myDefinition = new DataDefinition(name, precedence, sorts, parameters);
        myDefinition.setPolyParams(polyParams);
        myParentNs.addDefinition(myDefinition);
      }

      Data(SimpleNamespace parentNs, String name, Abstract.Binding.Precedence precedence, SortMax sorts, DependentLink parameters) {
        this(parentNs, name, precedence, sorts, parameters, new ArrayList<Binding>());
      }

      DataDefinition definition() {
        return myDefinition;
      }

      Constructor addConstructor(String name, Abstract.Binding.Precedence precedence, DependentLink parameters) {
        Constructor constructor = new Constructor(name, precedence, parameters, myDefinition);
        myDefinition.addConstructor(constructor);
        myNs.addDefinition(constructor);
        myParentNs.addDefinition(constructor);
        return constructor;
      }
    }

    static class Function {
      private final FunctionDefinition myDefinition;

      public Function(SimpleNamespace parentNs, String name, Abstract.Binding.Precedence precedence, DependentLink parameters, Expression resultType, ElimTreeNode elimTree, List<Binding> polyParams) {
        myDefinition = new FunctionDefinition(name, precedence, EmptyNamespace.INSTANCE, parameters, resultType, elimTree);
        myDefinition.setPolyParams(polyParams);
        parentNs.addDefinition(myDefinition);
      }

      public Function(SimpleNamespace parentNs, String name, Abstract.Binding.Precedence precedence, DependentLink parameters, Expression resultType, ElimTreeNode elimTree) {
        this(parentNs, name, precedence, parameters, resultType, elimTree, new ArrayList<Binding>());
      }

      FunctionDefinition definition() {
        return myDefinition;
      }
    }
  }
}
