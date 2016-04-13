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

    DependentLink PathParameter1 = param(false, "lvl", Level());
    DependentLink PathParameter2 = param("A", Pi(param(Interval()), Universe(Reference(PathParameter1))));
    DependentLink PathParameter3 = param("a", Apps(Reference(PathParameter2), Left()));
    DependentLink PathParameter4 = param("a'", Apps(Reference(PathParameter2), Right()));
    PathParameter1.setNext(PathParameter2);
    PathParameter2.setNext(PathParameter3);
    PathParameter3.setNext(PathParameter4);
    PATH = new DataDefinition(new DefinitionResolvedName(PRELUDE, "Path"), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(PathParameter1))), PathParameter1);
    PRELUDE.addDefinition(PATH);

    DependentLink piParam = param("i", Interval());
    //DependentLink pathParameter1 = param(false, "lvl", Level());
    DependentLink pathParameter = param(Pi(piParam, Apps(Reference(PathParameter2), Reference(piParam))));
    //pathParameter1.setNext(pathParameter2);
    PATH_CON = new Constructor(new DefinitionResolvedName(PRELUDE.getChild(PATH.getName()), "path"), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(PathParameter1))), pathParameter, PATH);
    PRELUDE.addMember(PATH.addConstructor(PATH_CON));

    DependentLink pathInfixParameter1 = param(false, "lvl", Level());
    DependentLink pathInfixParameter2 = param(false, "A", Universe(Reference(pathInfixParameter1)));
    DependentLink pathInfixParameter3 = param(true, vars("a", "a'"), Reference(pathInfixParameter2));
    pathInfixParameter1.setNext(pathInfixParameter2);
    pathInfixParameter2.setNext(pathInfixParameter3);
    Expression pathInfixTerm = DataCall(PATH)
            .addArgument(Reference(pathInfixParameter1), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Lam(param("_", Interval()), Reference(pathInfixParameter2)), AppExpression.DEFAULT)
            .addArgument(Reference(pathInfixParameter3), AppExpression.DEFAULT)
            .addArgument(Reference(pathInfixParameter3.getNext()), AppExpression.DEFAULT);
    PATH_INFIX = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "="), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixParameter1, Universe(Reference(pathInfixParameter1)), top(pathInfixParameter1, leaf(pathInfixTerm)));

    PRELUDE.addDefinition(PATH_INFIX);

    DependentLink atParameter1 = param(false, "lvl", Level());
    DependentLink atParameter2 = param(false, "A", Pi(param(Interval()), Universe(Reference(atParameter1))));
    DependentLink atParameter3 = param(false, "a", Apps(Reference(atParameter2), Left()));
    DependentLink atParameter4 = param(false, "a'", Apps(Reference(atParameter2), Right()));
    DependentLink atParameter5 = param("p", Apps(DataCall(PATH), Reference(atParameter1), Reference(atParameter2), Reference(atParameter3), Reference(atParameter4)));
    DependentLink atParameter6 = param("i", Interval());
    atParameter1.setNext(atParameter2);
    atParameter2.setNext(atParameter3);
    atParameter3.setNext(atParameter4);
    atParameter4.setNext(atParameter5);
    atParameter5.setNext(atParameter6);
    DependentLink atPath = param("f", pathParameter.getType());
    Expression atResultType = Apps(Reference(atParameter2), Reference(atParameter6));
    ElimTreeNode atElimTree = top(atParameter1, branch(atParameter6, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(atParameter3)),
            clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(atParameter4)),
            clause(branch(atParameter5, tail(atParameter6),
                    clause(PATH_CON, atPath, Apps(Reference(atPath), Reference(atParameter6)))))
    ));
    AT = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "@"), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atParameter1, atResultType, atElimTree);
    PRELUDE.addDefinition(AT);

    DependentLink coerceParameter1 = param(false, "lvl", Level());
    DependentLink coerceParameter2 = param("type", Pi(param(Interval()), Universe(Reference(coerceParameter1))));
    DependentLink coerceParameter3 = param("elem", Apps(Reference(coerceParameter2), Left()));
    DependentLink coerceParameter4 = param("point", Interval());
    coerceParameter1.setNext(coerceParameter2);
    coerceParameter2.setNext(coerceParameter3);
    coerceParameter3.setNext(coerceParameter4);
    ElimTreeNode coerceElimTreeNode = top(coerceParameter1, branch(coerceParameter4, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(coerceParameter3))));
    COERCE = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "coe"), Abstract.Definition.DEFAULT_PRECEDENCE, coerceParameter1, Apps(Reference(coerceParameter2), Reference(coerceParameter4)), coerceElimTreeNode);
    PRELUDE.addDefinition(COERCE);

    DependentLink isoParameter1 = param(false, "lvl", Level());
    DependentLink isoParameter2 = param(false, vars("A", "B"), Universe(Reference(isoParameter1)));
    DependentLink isoParameter3 = param("f", Pi(param(Reference(isoParameter2)), Reference(isoParameter2.getNext())));
    DependentLink isoParameter4 = param("g", Pi(param(Reference(isoParameter2.getNext())), Reference(isoParameter2)));
    DependentLink piParamA = param("a", Reference(isoParameter2));
    DependentLink piParamB = param("b", Reference(isoParameter2.getNext()));
    Expression isoParameters5type = FunCall(PATH_INFIX)
            .addArgument(Reference(isoParameter1), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(isoParameter2), EnumSet.of(AppExpression.Flag.VISIBLE))
            .addArgument(Apps(Reference(isoParameter4), Apps(Reference(isoParameter3), Reference(piParamA))), AppExpression.DEFAULT)
            .addArgument(Reference(piParamA), AppExpression.DEFAULT);
    DependentLink isoParameter5 = param("linv", Pi(piParamA, isoParameters5type));
    Expression isoParameters6type = FunCall(PATH_INFIX)
            .addArgument(Reference(isoParameter1), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(isoParameter2.getNext()), EnumSet.of(AppExpression.Flag.VISIBLE))
            .addArgument(Apps(Reference(isoParameter3), Apps(Reference(isoParameter4), Reference(piParamB))), AppExpression.DEFAULT)
            .addArgument(Reference(piParamB), AppExpression.DEFAULT);
    DependentLink isoParameter6 = param("rinv", Pi(piParamB, isoParameters6type));
    DependentLink isoParameter7 = param("i", Interval());
    isoParameter1.setNext(isoParameter2);
    isoParameter2.setNext(isoParameter3);
    isoParameter3.setNext(isoParameter4);
    isoParameter4.setNext(isoParameter5);
    isoParameter5.setNext(isoParameter6);
    isoParameter6.setNext(isoParameter7);
    Expression isoResultType = Universe(Reference(isoParameter1));
    ElimTreeNode isoElimTree = top(isoParameter1, branch(isoParameter7, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter2)),
            clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter2.getNext()))
    ));
    ISO = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "iso"), Abstract.Definition.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree);
    PRELUDE.addDefinition(ISO);

    DependentLink truncParameter1 = param(false, "lvl", Level());
    DependentLink truncParameter2 = param("A", Universe(Reference(truncParameter1)));
    truncParameter1.setNext(truncParameter2);
    PROP_TRUNC = new DataDefinition(PRELUDE.getChild("TrP").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, TypeUniverse.PROP, truncParameter1);
    PRELUDE.addDefinition(PROP_TRUNC);

    Constructor propTruncInCon = new Constructor(PRELUDE.getChild(PROP_TRUNC.getName()).getChild("inP").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(truncParameter1))), param("inP", Reference(truncParameter2)), PROP_TRUNC);
    DependentLink propTruncConParameter1 = param("a", Apps(DataCall(PROP_TRUNC), Reference(truncParameter2)));
    DependentLink propTruncConParameter2 = param("a'", Apps(DataCall(PROP_TRUNC), Reference(truncParameter2)));
    DependentLink propTruncConParameter3 = param("i", Interval());
    propTruncConParameter1.setNext(propTruncConParameter2);
    propTruncConParameter2.setNext(propTruncConParameter3);
    PROP_TRUNC_PATH_CON = new Constructor(PRELUDE.getChild(PROP_TRUNC.getName()).getChild("truncP").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(truncParameter1))), propTruncConParameter1, PROP_TRUNC);
    PRELUDE.addMember(PROP_TRUNC.addConstructor(propTruncInCon));
    PRELUDE.addMember(PROP_TRUNC.addConstructor(PROP_TRUNC_PATH_CON));
    Condition propTruncPathCond = new Condition(PROP_TRUNC_PATH_CON, top(propTruncConParameter1, branch(propTruncConParameter3, tail(),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter1)),
            clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter2)))));
    PROP_TRUNC.addCondition(propTruncPathCond);

    SET_TRUNC = new DataDefinition(PRELUDE.getChild("TrS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(PLevel().applyThis(Reference(truncParameter1)), TypeUniverse.HomotopyLevel.SET.getValue())), truncParameter1);
    PRELUDE.addDefinition(SET_TRUNC);

    Constructor setTruncInCon = new Constructor(PRELUDE.getChild(SET_TRUNC.getName()).getChild("inS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(truncParameter1))), param("inS", Reference(truncParameter2)), SET_TRUNC);
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
    SET_TRUNC_PATH_CON = new Constructor(PRELUDE.getChild(SET_TRUNC.getName()).getChild("truncS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(truncParameter1))), setTruncConParameter1, SET_TRUNC);
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
