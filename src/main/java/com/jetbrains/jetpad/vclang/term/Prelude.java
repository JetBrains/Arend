package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.DefinitionResolvedName;
import com.jetbrains.jetpad.vclang.naming.ModuleResolvedName;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.Collection;
import java.util.EnumSet;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude extends Namespace {
  public static ModuleID moduleID = new ModuleID() {
    @Override
    public ModulePath getModulePath() {
      return new ModulePath("Prelude");
    }
  };

  public static ClassDefinition PRELUDE_CLASS;

  public static Namespace PRELUDE = new Prelude();

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
    PRELUDE_CLASS = new ClassDefinition(new ModuleResolvedName(moduleID), null);
    Preprelude.setUniverses();

    DependentLink PathParameter1 = param(false, "lp", Lvl());
    DependentLink PathParameter2 = param(false, "lh", CNat());
    DependentLink PathParameter3 = param("A", Pi(param(Interval()), Universe(Reference(PathParameter1), Reference(PathParameter2))));
    DependentLink PathParameter4 = param("a", Apps(Reference(PathParameter3), Left()));
    DependentLink PathParameter5 = param("a'", Apps(Reference(PathParameter3), Right()));
    PathParameter1.setNext(PathParameter2);
    PathParameter2.setNext(PathParameter3);
    PathParameter3.setNext(PathParameter4);
    PathParameter4.setNext(PathParameter5);
    PATH = new DataDefinition(new DefinitionResolvedName(PRELUDE, "Path"), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverseNew(Reference(PathParameter1), Reference(PathParameter2)), PathParameter1);
    PRELUDE.addDefinition(PATH);

    DependentLink piParam = param("i", Interval());
    //DependentLink pathParameter1 = param(false, "lvl", Level());
    DependentLink pathParameter = param(Pi(piParam, Apps(Reference(PathParameter3), Reference(piParam))));
    //pathParameter1.setNext(pathParameter2);
    PATH_CON = new Constructor(new DefinitionResolvedName(PRELUDE.getChild(PATH.getName()), "path"), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverseNew(Reference(PathParameter1), Reference(PathParameter2)), pathParameter, PATH);
    PRELUDE.addMember(PATH.addConstructor(PATH_CON));

    DependentLink pathInfixParameter1 = param(false, "lp", Lvl());
    DependentLink pathInfixParameter2 = param(false, "lh", CNat());
    DependentLink pathInfixParameter3 = param(false, "A", Universe(Reference(pathInfixParameter1), Reference(pathInfixParameter2)));
    DependentLink pathInfixParameter4 = param(true, vars("a", "a'"), Reference(pathInfixParameter3));
    pathInfixParameter1.setNext(pathInfixParameter2);
    pathInfixParameter2.setNext(pathInfixParameter3);
    pathInfixParameter3.setNext(pathInfixParameter4);
    Expression pathInfixTerm = DataCall(PATH)
            .addArgument(Reference(pathInfixParameter1), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(pathInfixParameter2), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Lam(param("_", Interval()), Reference(pathInfixParameter3)), AppExpression.DEFAULT)
            .addArgument(Reference(pathInfixParameter4), AppExpression.DEFAULT)
            .addArgument(Reference(pathInfixParameter4.getNext()), AppExpression.DEFAULT);
    PATH_INFIX = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "="), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixParameter1, Universe(Reference(pathInfixParameter1), Reference(pathInfixParameter2)), top(pathInfixParameter1, leaf(pathInfixTerm)));

    PRELUDE.addDefinition(PATH_INFIX);

    DependentLink atParameter1 = param(false, "lp", Lvl());
    DependentLink atParameter2 = param(false, "lh", CNat());
    DependentLink atParameter3 = param(false, "A", Pi(param(Interval()), Universe(Reference(atParameter1), Reference(atParameter2))));
    DependentLink atParameter4 = param(false, "a", Apps(Reference(atParameter3), Left()));
    DependentLink atParameter5 = param(false, "a'", Apps(Reference(atParameter3), Right()));
    DependentLink atParameter6 = param("p", Apps(DataCall(PATH), Reference(atParameter1), Reference(atParameter2), Reference(atParameter3), Reference(atParameter4), Reference(atParameter5)));
    DependentLink atParameter7 = param("i", Interval());
    atParameter1.setNext(atParameter2);
    atParameter2.setNext(atParameter3);
    atParameter3.setNext(atParameter4);
    atParameter4.setNext(atParameter5);
    atParameter5.setNext(atParameter6);
    atParameter6.setNext(atParameter7);
    DependentLink atPath = param("f", pathParameter.getType());
    Expression atResultType = Apps(Reference(atParameter3), Reference(atParameter7));
    ElimTreeNode atElimTree = top(atParameter1, branch(atParameter7, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(atParameter4)),
            clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(atParameter5)),
            clause(branch(atParameter5, tail(atParameter7),
                    clause(PATH_CON, atPath, Apps(Reference(atPath), Reference(atParameter7)))))
    ));
    AT = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "@"), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atParameter1, atResultType, atElimTree);
    PRELUDE.addDefinition(AT);

    DependentLink coerceParameter1 = param(false, "lp", Lvl());
    DependentLink coerceParameter2 = param(false, "lh", CNat());
    DependentLink coerceParameter3 = param("type", Pi(param(Interval()), Universe(Reference(coerceParameter1), Reference(coerceParameter2))));
    DependentLink coerceParameter4 = param("elem", Apps(Reference(coerceParameter3), Left()));
    DependentLink coerceParameter5 = param("point", Interval());
    coerceParameter1.setNext(coerceParameter2);
    coerceParameter2.setNext(coerceParameter3);
    coerceParameter3.setNext(coerceParameter4);
    coerceParameter4.setNext(coerceParameter5);
    ElimTreeNode coerceElimTreeNode = top(coerceParameter1, branch(coerceParameter5, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(coerceParameter4))));
    COERCE = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "coe"), Abstract.Definition.DEFAULT_PRECEDENCE, coerceParameter1, Apps(Reference(coerceParameter3), Reference(coerceParameter5)), coerceElimTreeNode);
    PRELUDE.addDefinition(COERCE);

    DependentLink isoParameter1 = param(false, "lp", Lvl());
    DependentLink isoParameter2 = param(false, "lh", CNat());
    DependentLink isoParameter3 = param(false, vars("A", "B"), Universe(Reference(isoParameter1), Reference(isoParameter2)));
    DependentLink isoParameter4 = param("f", Pi(param(Reference(isoParameter3)), Reference(isoParameter3.getNext())));
    DependentLink isoParameter5 = param("g", Pi(param(Reference(isoParameter3.getNext())), Reference(isoParameter3)));
    DependentLink piParamA = param("a", Reference(isoParameter3));
    DependentLink piParamB = param("b", Reference(isoParameter3.getNext()));
    Expression isoParameters6type = FunCall(PATH_INFIX)
            .addArgument(Reference(isoParameter1), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(isoParameter2), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(isoParameter3), EnumSet.of(AppExpression.Flag.VISIBLE))
            .addArgument(Apps(Reference(isoParameter5), Apps(Reference(isoParameter4), Reference(piParamA))), AppExpression.DEFAULT)
            .addArgument(Reference(piParamA), AppExpression.DEFAULT);
    DependentLink isoParameter6 = param("linv", Pi(piParamA, isoParameters6type));
    Expression isoParameters7type = FunCall(PATH_INFIX)
            .addArgument(Reference(isoParameter1), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(isoParameter2), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(isoParameter3.getNext()), EnumSet.of(AppExpression.Flag.VISIBLE))
            .addArgument(Apps(Reference(isoParameter4), Apps(Reference(isoParameter5), Reference(piParamB))), AppExpression.DEFAULT)
            .addArgument(Reference(piParamB), AppExpression.DEFAULT);
    DependentLink isoParameter7 = param("rinv", Pi(piParamB, isoParameters7type));
    DependentLink isoParameter8 = param("i", Interval());
    isoParameter1.setNext(isoParameter2);
    isoParameter2.setNext(isoParameter3);
    isoParameter3.setNext(isoParameter4);
    isoParameter4.setNext(isoParameter5);
    isoParameter5.setNext(isoParameter6);
    isoParameter6.setNext(isoParameter7);
    isoParameter7.setNext(isoParameter8);
    Expression isoResultType = Universe(Reference(isoParameter1), Reference(isoParameter2));
    ElimTreeNode isoElimTree = top(isoParameter1, branch(isoParameter7, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter3)),
            clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter3.getNext()))
    ));
    ISO = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "iso"), Abstract.Definition.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree);
    PRELUDE.addDefinition(ISO);

    DependentLink truncParameter1 = param(false, "lp", Lvl());
    DependentLink truncParameter2 = param(false, "lh", CNat());
    DependentLink truncParameter3 = param("A", Universe(Reference(truncParameter1), Reference(truncParameter2)));
    truncParameter1.setNext(truncParameter2);
    truncParameter2.setNext(truncParameter3);
    PROP_TRUNC = new DataDefinition(PRELUDE.getChild("TrP").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverseNew(Reference(truncParameter1), Reference(truncParameter2)), truncParameter1);
    PRELUDE.addDefinition(PROP_TRUNC);

    Constructor propTruncInCon = new Constructor(PRELUDE.getChild(PROP_TRUNC.getName()).getChild("inP").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverseNew(Reference(truncParameter1), Reference(truncParameter2)), param("inP", Reference(truncParameter2)), PROP_TRUNC);
    DependentLink propTruncConParameter1 = param("a", Apps(DataCall(PROP_TRUNC), Reference(truncParameter2)));
    DependentLink propTruncConParameter2 = param("a'", Apps(DataCall(PROP_TRUNC), Reference(truncParameter2)));
    DependentLink propTruncConParameter3 = param("i", Interval());
    propTruncConParameter1.setNext(propTruncConParameter2);
    propTruncConParameter2.setNext(propTruncConParameter3);
    PROP_TRUNC_PATH_CON = new Constructor(PRELUDE.getChild(PROP_TRUNC.getName()).getChild("truncP").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverseNew(Reference(truncParameter1), Reference(truncParameter2)), propTruncConParameter1, PROP_TRUNC);
    PRELUDE.addMember(PROP_TRUNC.addConstructor(propTruncInCon));
    PRELUDE.addMember(PROP_TRUNC.addConstructor(PROP_TRUNC_PATH_CON));
    Condition propTruncPathCond = new Condition(PROP_TRUNC_PATH_CON, top(propTruncConParameter1, branch(propTruncConParameter3, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter1)),
            clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter2)))));
    PROP_TRUNC.addCondition(propTruncPathCond);

    SET_TRUNC = new DataDefinition(PRELUDE.getChild("TrS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverseNew(Reference(truncParameter1), Reference(truncParameter2)), truncParameter1);
    PRELUDE.addDefinition(SET_TRUNC);

    Constructor setTruncInCon = new Constructor(PRELUDE.getChild(SET_TRUNC.getName()).getChild("inS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverseNew(Reference(truncParameter1), Reference(truncParameter2)), param("inS", Reference(truncParameter2)), SET_TRUNC);
    DependentLink setTruncConParameter1 = param("a", Apps(DataCall(SET_TRUNC), Reference(truncParameter2)));
    DependentLink setTruncConParameter2 = param("a'", Apps(DataCall(SET_TRUNC), Reference(truncParameter2)));;
    Expression setTruncConParameter3type = FunCall(PATH_INFIX)
            .addArgument(DataCall(SET_TRUNC).addArgument(Reference(truncParameter1), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(Reference(truncParameter2), AppExpression.DEFAULT), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(setTruncConParameter1), AppExpression.DEFAULT)
            .addArgument(Reference(setTruncConParameter2), AppExpression.DEFAULT);
    DependentLink setTruncConParameter3 = param("p", setTruncConParameter3type);
    Expression setTruncConParameter4type = FunCall(PATH_INFIX)
            .addArgument(DataCall(SET_TRUNC).addArgument(Reference(truncParameter1), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(Reference(truncParameter2), AppExpression.DEFAULT), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(setTruncConParameter1), AppExpression.DEFAULT)
            .addArgument(Reference(setTruncConParameter2), AppExpression.DEFAULT);
    DependentLink setTruncConParameter4 = param("q", setTruncConParameter4type);
    DependentLink setTruncConParameter5 = param("i", Interval());
    DependentLink setTruncConParameter6 = param("j", Interval());
    setTruncConParameter1.setNext(setTruncConParameter2);
    setTruncConParameter2.setNext(setTruncConParameter3);
    setTruncConParameter3.setNext(setTruncConParameter4);
    setTruncConParameter4.setNext(setTruncConParameter5);
    setTruncConParameter5.setNext(setTruncConParameter6);
    SET_TRUNC_PATH_CON = new Constructor(PRELUDE.getChild(SET_TRUNC.getName()).getChild("truncS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverseNew(Reference(truncParameter1), Reference(truncParameter2)), setTruncConParameter1, SET_TRUNC);
    PRELUDE.addMember(SET_TRUNC.addConstructor(setTruncInCon));
    PRELUDE.addMember(SET_TRUNC.addConstructor(SET_TRUNC_PATH_CON));
    Condition setTruncPathCond = new Condition(SET_TRUNC_PATH_CON, top(setTruncConParameter1, branch(setTruncConParameter6, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), FunCall(AT).addArgument(Reference(truncParameter1), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(Reference(setTruncConParameter3), AppExpression.DEFAULT).addArgument(Reference(setTruncConParameter5), AppExpression.DEFAULT)),
            clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), FunCall(AT).addArgument(Reference(truncParameter1), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(Reference(setTruncConParameter4), AppExpression.DEFAULT).addArgument(Reference(setTruncConParameter5), AppExpression.DEFAULT)),
            clause(branch(setTruncConParameter5, tail(setTruncConParameter6),
                    clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter1)),
                    clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter2))))
    )));

    SET_TRUNC.addCondition(setTruncPathCond);

  }

  /*
  private static Expression hlevelMinusOne(Expression level) {
    DependentLink minusOneParameter = param("n", CNat());
    ElimTreeNode minusOneElimTree = top(minusOneParameter, branch(minusOneParameter, tail(),
            clause(INF, EmptyDependentLink.getInstance(), Inf()),
            clause(FIN, finCNatParameter, branch(maxCNatParameter2, tail(),
                    clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
                    clause(FIN, finCNatParameterPrime, Apps(ConCall(FIN), Apps(FunCall(MAX_NAT), Reference(finCNatParameter), Reference(finCNatParameterPrime))))))));
    FunctionDefinition minusOne = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "iso" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree);
  }/**/

  private Prelude() {
    super(moduleID);
  }

  @Override
  public Collection<NamespaceMember> getMembers() {
    throw new IllegalStateException();
  }

  /*private static void generateLevel(int i) {
    String suffix = i == 0 ? "" : Integer.toString(i);

    defToLevel.put(setTruncInCon, i);
    defToLevel.put(setTruncPathCon, i);

    levels.put(i, new LevelPoly(path, pathCon, pathInfix, at, coe, iso, propTruncPathCon, setTruncPathCon));
  }/**/

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

}
