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
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT, ABSTRACT;

  public static FunctionDefinition COERCE;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition AT;

  public static FunctionDefinition ISO;

  public static DataDefinition LVL;
  public static Constructor ZERO_LVL;
  public static Constructor SUC_LVL;
  public static FunctionDefinition MAX_LVL;

  public static DataDefinition CNAT;
  public static Constructor FIN, INF;

  public static FunctionDefinition MAX_NAT;
  public static FunctionDefinition MAX_CNAT;
  public static FunctionDefinition SUC_CNAT;

  public static ClassDefinition LEVEL;
  public static ClassField PLEVEL;
  public static ClassField HLEVEL;

  public static DataDefinition PROP_TRUNC;
  public static DataDefinition SET_TRUNC;

  static {
    PRELUDE_CLASS = new ClassDefinition(new ModuleResolvedName(moduleID), null);

    NAT = new DataDefinition(new DefinitionResolvedName(PRELUDE, "Nat"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    Namespace natNamespace = PRELUDE.getChild(NAT.getName());
    ZERO = new Constructor(new DefinitionResolvedName(natNamespace, "zero"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), NAT);
    SUC = new Constructor(new DefinitionResolvedName(natNamespace, "suc"), Abstract.Definition.DEFAULT_PRECEDENCE, null, param(DataCall(NAT)), NAT);

    PRELUDE.addDefinition(NAT);
    PRELUDE.addMember(NAT.addConstructor(ZERO));
    PRELUDE.addMember(NAT.addConstructor(SUC));

    INTERVAL = new DataDefinition(new DefinitionResolvedName(PRELUDE, "I"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    Namespace intervalNamespace = PRELUDE.getChild(INTERVAL.getName());
    LEFT = new Constructor(new DefinitionResolvedName(intervalNamespace, "left"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), INTERVAL);
    RIGHT = new Constructor(new DefinitionResolvedName(intervalNamespace, "right"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), INTERVAL);
    ABSTRACT = new Constructor(new DefinitionResolvedName(intervalNamespace, "<abstract>"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), INTERVAL);

    PRELUDE.addDefinition(INTERVAL);
    PRELUDE.addMember(INTERVAL.addConstructor(LEFT));
    PRELUDE.addMember(INTERVAL.addConstructor(RIGHT));
    INTERVAL.addConstructor(ABSTRACT);

    /*DependentLink isoParameter1 = param(false, vars("A", "B"), Universe(0, UniverseOld.Type.PROP));
    DependentLink isoParameter2 = param("f", Pi(param(Reference(isoParameter1)), Reference(isoParameter1.getNext())));
    DependentLink isoParameter3 = param("g", Pi(param(Reference(isoParameter1.getNext())), Reference(isoParameter1)));
    DependentLink isoParameter4 = param("i", DataCall(INTERVAL));
    isoParameter1.setNext(isoParameter2);
    isoParameter2.setNext(isoParameter3);
    isoParameter3.setNext(isoParameter4);
    Expression isoResultType = Universe(0, UniverseOld.Type.PROP);
    ElimTreeNode isoElimTree = top(isoParameter1, branch(isoParameter4, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter1)),
            clause(RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter1.getNext()))
    ));
    FunctionDefinition iso = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "isoP"), Abstract.Definition.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree);
    PRELUDE.addDefinition(iso); /**/

    LVL = new DataDefinition(new DefinitionResolvedName(PRELUDE, "Lvl"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    PRELUDE.addDefinition(LVL);

    ZERO_LVL = new Constructor(new DefinitionResolvedName(PRELUDE.getChild(LVL.getName()), "zeroLvl"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), LVL);
    DependentLink sucLvlParameter = param("l", DataCall(LVL));
    SUC_LVL = new Constructor(new DefinitionResolvedName(PRELUDE.getChild(LVL.getName()), "sucLvl"), Abstract.Definition.DEFAULT_PRECEDENCE, null, sucLvlParameter, LVL);
    PRELUDE.addMember(LVL.addConstructor(ZERO_LVL));
    PRELUDE.addMember(LVL.addConstructor(SUC_LVL));

    DependentLink maxLvlParameter1 = param(DataCall(LVL));
    DependentLink maxLvlParameter2 = param(DataCall(LVL));
    maxLvlParameter1.setNext(maxLvlParameter2);
    DependentLink sucLvlParameterPrime = param("l'", DataCall(LVL));
    MAX_LVL = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "maxLvl"), Abstract.Definition.DEFAULT_PRECEDENCE, maxLvlParameter1, DataCall(LVL), null, null);
    ElimTreeNode maxLvlElimTree = top(maxLvlParameter1, branch(maxLvlParameter1, tail(maxLvlParameter2),
            clause(ZERO_LVL, EmptyDependentLink.getInstance(), branch(maxLvlParameter2, tail(),
                    clause(ZERO_LVL, EmptyDependentLink.getInstance(), ConCall(ZERO_LVL)),
                    clause(SUC_LVL, sucLvlParameter, Apps(ConCall(SUC_LVL), Reference(sucLvlParameter))))),
            clause(SUC_LVL, sucLvlParameter, branch(maxLvlParameter2, tail(),
                    clause(ZERO_LVL, EmptyDependentLink.getInstance(), Apps(ConCall(SUC_LVL), Reference(sucLvlParameter))),
                    clause(SUC_LVL, sucLvlParameterPrime, Apps(ConCall(SUC_LVL), Apps(FunCall(MAX_LVL), Reference(sucLvlParameter), Reference(sucLvlParameterPrime))))))));
    MAX_LVL.setElimTree(maxLvlElimTree);
    PRELUDE.addDefinition(MAX_LVL);

    CNAT = new DataDefinition(new DefinitionResolvedName(PRELUDE, "CNat"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    PRELUDE.addDefinition(CNAT);

    INF = new Constructor(new DefinitionResolvedName(PRELUDE.getChild(CNAT.getName()), "inf"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), CNAT);
    DependentLink finParameter = param("n", DataCall(CNAT));
    FIN = new Constructor(new DefinitionResolvedName(PRELUDE.getChild(CNAT.getName()), "fin"), Abstract.Definition.DEFAULT_PRECEDENCE, null, finParameter, CNAT);
    PRELUDE.addMember(CNAT.addConstructor(FIN));
    PRELUDE.addMember(CNAT.addConstructor(INF));

    DependentLink maxNatParameter1 = param(DataCall(NAT));
    DependentLink maxNatParameter2 = param(DataCall(NAT));
    maxNatParameter1.setNext(maxNatParameter2);
    DependentLink sucNatParameter = param("n", DataCall(NAT));
    DependentLink sucNatParameterPrime = param("n'", DataCall(NAT));
    MAX_NAT = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "maxNat"), Abstract.Definition.DEFAULT_PRECEDENCE, maxNatParameter1, DataCall(NAT), null, null);
    ElimTreeNode maxNatElimTree = top(maxNatParameter1, branch(maxNatParameter1, tail(maxNatParameter2),
            clause(ZERO, EmptyDependentLink.getInstance(), branch(maxNatParameter2, tail(),
                    clause(ZERO, EmptyDependentLink.getInstance(), ConCall(ZERO)),
                    clause(SUC, sucNatParameter, Apps(ConCall(SUC), Reference(sucNatParameter))))),
            clause(SUC, sucNatParameter, branch(maxNatParameter2, tail(),
                    clause(ZERO, EmptyDependentLink.getInstance(), Apps(ConCall(SUC), Reference(sucNatParameter))),
                    clause(SUC, sucNatParameterPrime, Apps(ConCall(SUC), Apps(FunCall(MAX_NAT), Reference(sucNatParameter), Reference(sucNatParameterPrime))))))));
    MAX_NAT.setElimTree(maxNatElimTree);
    PRELUDE.addDefinition(MAX_NAT);

    DependentLink maxCNatParameter1 = param(DataCall(CNAT));
    DependentLink maxCNatParameter2 = param(DataCall(CNAT));
    maxCNatParameter1.setNext(maxCNatParameter2);
    DependentLink finCNatParameter = param("n", DataCall(NAT));
    DependentLink finCNatParameterPrime = param("n'", DataCall(NAT));
    MAX_CNAT = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "maxCNat"), Abstract.Definition.DEFAULT_PRECEDENCE, maxCNatParameter1, DataCall(CNAT), null, null);
    ElimTreeNode maxCNatElimTree = top(maxCNatParameter1, branch(maxCNatParameter1, tail(maxCNatParameter2),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, branch(maxCNatParameter2, tail(),
                    clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
                    clause(FIN, finCNatParameterPrime, Apps(ConCall(FIN), Apps(FunCall(MAX_NAT), Reference(finCNatParameter), Reference(finCNatParameterPrime))))))));
    MAX_CNAT.setElimTree(maxCNatElimTree);
    PRELUDE.addDefinition(MAX_CNAT);

    DependentLink sucCNatParameter = param(DataCall(CNAT));
    ElimTreeNode sucCNatElimTree = top(sucCNatParameter, branch(sucCNatParameter, tail(),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, Apps(ConCall(FIN), Apps(ConCall(SUC), Reference(finCNatParameter))))));
    SUC_CNAT = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "suc"), Abstract.Definition.DEFAULT_PRECEDENCE, sucCNatParameter, DataCall(CNAT), sucCNatElimTree, null);
    PRELUDE.addDefinition(SUC_CNAT);

    LEVEL = new ClassDefinition(new DefinitionResolvedName(PRELUDE, "Level"), null);
    PLEVEL = new ClassField(new DefinitionResolvedName(PRELUDE.getChild(LEVEL.getName()), "PLevel"), Abstract.Definition.DEFAULT_PRECEDENCE, DataCall(LVL), LEVEL, EmptyDependentLink.getInstance(), null);
    HLEVEL = new ClassField(new DefinitionResolvedName(PRELUDE.getChild(LEVEL.getName()), "HLevel"), Abstract.Definition.DEFAULT_PRECEDENCE, DataCall(CNAT), LEVEL, EmptyDependentLink.getInstance(), null);
    LEVEL.addField(PLEVEL);
    LEVEL.addField(HLEVEL);
    PRELUDE.addDefinition(LEVEL);
    PRELUDE.getChild(LEVEL.getName()).addDefinition(PLEVEL);
    PRELUDE.getChild(LEVEL.getName()).addDefinition(HLEVEL);

    NAT.setUniverse(TypeUniverse.SetOfLevel(0));
    ZERO.setUniverse(TypeUniverse.SetOfLevel(0));
    SUC.setUniverse(TypeUniverse.SetOfLevel(0));
    INTERVAL.setUniverse(TypeUniverse.PROP);
    LEFT.setUniverse(TypeUniverse.PROP);
    RIGHT.setUniverse(TypeUniverse.PROP);
    ABSTRACT.setUniverse(TypeUniverse.PROP);
    LVL.setUniverse(TypeUniverse.SetOfLevel(0));
    ZERO_LVL.setUniverse(TypeUniverse.SetOfLevel(0));
    SUC_LVL.setUniverse(TypeUniverse.SetOfLevel(0));
    CNAT.setUniverse(TypeUniverse.SetOfLevel(0));
    FIN.setUniverse(TypeUniverse.SetOfLevel(0));
    INF.setUniverse(TypeUniverse.SetOfLevel(0));
    LEVEL.setUniverse(TypeUniverse.SetOfLevel(0));
    PLEVEL.setUniverse(TypeUniverse.SetOfLevel(0));
    HLEVEL.setUniverse(TypeUniverse.SetOfLevel(0));

    DependentLink PathParameter1 = param(false, "lvl", Level());
    DependentLink PathParameter2 = param("A", Pi(param(DataCall(INTERVAL)), Universe(Reference(PathParameter1))));
    DependentLink PathParameter3 = param("a", Apps(Reference(PathParameter2), ConCall(LEFT)));
    DependentLink PathParameter4 = param("a'", Apps(Reference(PathParameter2), ConCall(RIGHT)));
    PathParameter1.setNext(PathParameter2);
    PathParameter2.setNext(PathParameter3);
    PathParameter3.setNext(PathParameter4);
    PATH = new DataDefinition(new DefinitionResolvedName(PRELUDE, "Path"), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(PathParameter1))), PathParameter1);
    PRELUDE.addDefinition(PATH);

    DependentLink piParam = param("i", DataCall(INTERVAL));
    DependentLink pathParameter1 = param(false, "lvl", Level());
    DependentLink pathParameter2 = param(Pi(piParam, Apps(Reference(PathParameter2), Reference(piParam))));
    pathParameter1.setNext(pathParameter2);
    PATH_CON = new Constructor(new DefinitionResolvedName(PRELUDE.getChild(PATH.getName()), "path"), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(pathParameter1))), pathParameter1, PATH);
    PRELUDE.addMember(PATH.addConstructor(PATH_CON));

    DependentLink pathInfixParameter1 = param(false, "lvl", Level());
    DependentLink pathInfixParameter2 = param(false, "A", Universe(Reference(pathInfixParameter1)));
    DependentLink pathInfixParameter3 = param(true, vars("a", "a'"), Reference(pathInfixParameter1));
    pathInfixParameter1.setNext(pathInfixParameter2);
    pathInfixParameter2.setNext(pathInfixParameter3);
    Expression pathInfixTerm = Apps(Apps(DataCall(PATH), new ArgumentExpression(Reference(pathInfixParameter1), false, true)), Lam(param("_", DataCall(INTERVAL)), Reference(pathInfixParameter2)), Reference(pathInfixParameter3), Reference(pathInfixParameter3.getNext()));
    PATH_INFIX = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "="), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixParameter1, Universe(Reference(pathInfixParameter1)), top(pathInfixParameter1, leaf(pathInfixTerm)));

    PRELUDE.addDefinition(PATH_INFIX);

    DependentLink atParameter1 = param(false, "lvl", Level());
    DependentLink atParameter2 = param(false, "A", PathParameter2.getType());
    DependentLink atParameter3 = param(false, "a", PathParameter3.getType());
    DependentLink atParameter4 = param(false, "a'", PathParameter4.getType());
    DependentLink atParameter5 = param("p", Apps(DataCall(PATH), Reference(atParameter2), Reference(atParameter3), Reference(atParameter4)));
    DependentLink atParameter6 = param("i", DataCall(INTERVAL));
    atParameter1.setNext(atParameter2);
    atParameter2.setNext(atParameter3);
    atParameter3.setNext(atParameter4);
    atParameter4.setNext(atParameter5);
    atParameter5.setNext(atParameter6);
    DependentLink atPath = param("f", pathParameter2.getType());
    Expression atResultType = Apps(Reference(atParameter2), Reference(atParameter6));
    ElimTreeNode atElimTree = top(atParameter1, branch(atParameter6, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Reference(atParameter3)),
            clause(RIGHT, EmptyDependentLink.getInstance(), Reference(atParameter4)),
            clause(branch(atParameter5, tail(atParameter6),
                    clause(PATH_CON, atPath, Apps(Reference(atPath), Reference(atParameter6)))))
    ));
    AT = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "@"), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atParameter1, atResultType, atElimTree);
    PRELUDE.addDefinition(AT);

    DependentLink coerceParameter1 = param(false, "lvl", Level());
    DependentLink coerceParameter2 = param("type", Pi(param(DataCall(INTERVAL)), Universe(Reference(coerceParameter1))));
    DependentLink coerceParameter3 = param("elem", Apps(Reference(coerceParameter2), ConCall(LEFT)));
    DependentLink coerceParameter4 = param("point", DataCall(INTERVAL));
    coerceParameter1.setNext(coerceParameter2);
    coerceParameter2.setNext(coerceParameter3);
    coerceParameter3.setNext(coerceParameter4);
    ElimTreeNode coerceElimTreeNode = top(coerceParameter1, branch(coerceParameter4, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(coerceParameter3))));
    COERCE = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "coe"), Abstract.Definition.DEFAULT_PRECEDENCE, coerceParameter1, Apps(Reference(coerceParameter2), Reference(coerceParameter4)), coerceElimTreeNode);
    PRELUDE.addDefinition(COERCE);

    DependentLink isoParameter1 = param(false, "lvl", Level());
    DependentLink isoParameter2 = param(false, vars("A", "B"), Universe(Reference(isoParameter1)));
    DependentLink isoParameter3 = param("f", Pi(param(Reference(isoParameter2)), Reference(isoParameter2.getNext())));
    DependentLink isoParameter4 = param("g", Pi(param(Reference(isoParameter2.getNext())), Reference(isoParameter2)));
    DependentLink piParamA = param("a", Reference(isoParameter2));
    DependentLink piParamB = param("b", Reference(isoParameter2.getNext()));
    DependentLink isoParameter5 = param("linv", Pi(piParamA, Apps(Apps(FunCall(PATH_INFIX), new ArgumentExpression(Reference(isoParameter1), false, true), new ArgumentExpression(Reference(isoParameter2), false, true)), Apps(Reference(isoParameter4), Apps(Reference(isoParameter3), Reference(piParamA))), Reference(piParamA))));
    DependentLink isoParameter6 = param("rinv", Pi(piParamB, Apps(Apps(FunCall(PATH_INFIX), new ArgumentExpression(Reference(isoParameter1), false, true), new ArgumentExpression(Reference(isoParameter2.getNext()), false, true)), Apps(Reference(isoParameter3), Apps(Reference(isoParameter4), Reference(piParamB))), Reference(piParamB))));
    DependentLink isoParameter7 = param("i", DataCall(INTERVAL));
    isoParameter1.setNext(isoParameter2);
    isoParameter2.setNext(isoParameter3);
    isoParameter3.setNext(isoParameter4);
    isoParameter4.setNext(isoParameter5);
    isoParameter5.setNext(isoParameter6);
    isoParameter6.setNext(isoParameter7);
    Expression isoResultType = Universe(Reference(isoParameter1));
    ElimTreeNode isoElimTree = top(isoParameter1, branch(isoParameter7, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter2)),
            clause(RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter2.getNext()))
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
    DependentLink propTruncConParameter3 = param("i", DataCall(INTERVAL));
    propTruncConParameter1.setNext(propTruncConParameter2);
    propTruncConParameter2.setNext(propTruncConParameter3);
    Constructor propTruncPathCon = new Constructor(PRELUDE.getChild(PROP_TRUNC.getName()).getChild("truncP").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(truncParameter1))), propTruncConParameter1, PROP_TRUNC);
    PRELUDE.addMember(PROP_TRUNC.addConstructor(propTruncInCon));
    PRELUDE.addMember(PROP_TRUNC.addConstructor(propTruncPathCon));
    Condition propTruncPathCond = new Condition(propTruncPathCon, top(propTruncConParameter1, branch(propTruncConParameter3, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter1)),
            clause(RIGHT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter2)))));
    PROP_TRUNC.addCondition(propTruncPathCond);

    SET_TRUNC = new DataDefinition(PRELUDE.getChild("TrS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(PLevel().applyThis(Reference(truncParameter1)), TypeUniverse.HomotopyLevel.SET.getValue())), truncParameter1);
    PRELUDE.addDefinition(SET_TRUNC);

    Constructor setTruncInCon = new Constructor(PRELUDE.getChild(SET_TRUNC.getName()).getChild("inS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(truncParameter1))), param("inS", Reference(truncParameter2)), SET_TRUNC);
    DependentLink setTruncConParameter1 = param("a", Apps(DataCall(SET_TRUNC), Reference(truncParameter2)));
    DependentLink setTruncConParameter2 = param("a'", Apps(DataCall(SET_TRUNC), Reference(truncParameter2)));
    DependentLink setTruncConParameter3 = param("p", Apps(Apps(FunCall(PATH_INFIX), new ArgumentExpression(Reference(truncParameter1), false, true), new ArgumentExpression(Apps(DataCall(SET_TRUNC), Reference(truncParameter2)), false, true)), Reference(setTruncConParameter1), Reference(setTruncConParameter2)));
    DependentLink setTruncConParameter4 = param("q", Apps(Apps(FunCall(PATH_INFIX), new ArgumentExpression(Reference(truncParameter1), false, true), new ArgumentExpression(Apps(DataCall(SET_TRUNC), Reference(truncParameter2)), false, true)), Reference(setTruncConParameter1), Reference(setTruncConParameter2)));
    DependentLink setTruncConParameter5 = param("i", DataCall(INTERVAL));
    DependentLink setTruncConParameter6 = param("j", DataCall(INTERVAL));
    setTruncConParameter1.setNext(setTruncConParameter2);
    setTruncConParameter2.setNext(setTruncConParameter3);
    setTruncConParameter3.setNext(setTruncConParameter4);
    setTruncConParameter4.setNext(setTruncConParameter5);
    setTruncConParameter5.setNext(setTruncConParameter6);
    Constructor setTruncPathCon = new Constructor(PRELUDE.getChild(SET_TRUNC.getName()).getChild("truncS").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new TypeUniverse(new TypeUniverse.TypeLevel(Reference(truncParameter1))), setTruncConParameter1, SET_TRUNC);
    PRELUDE.addMember(SET_TRUNC.addConstructor(setTruncInCon));
    PRELUDE.addMember(SET_TRUNC.addConstructor(setTruncPathCon));
    Condition setTruncPathCond = new Condition(setTruncPathCon, top(setTruncConParameter1, branch(setTruncConParameter6, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Apps(Apps(FunCall(AT), new ArgumentExpression(Reference(truncParameter1), false, true)), Reference(setTruncConParameter3), Reference(setTruncConParameter5))),
            clause(RIGHT, EmptyDependentLink.getInstance(), Apps(Apps(FunCall(AT), new ArgumentExpression(Reference(truncParameter1), false, true)), Reference(setTruncConParameter4), Reference(setTruncConParameter5))),
            clause(branch(setTruncConParameter5, tail(setTruncConParameter6),
                    clause(LEFT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter1)),
                    clause(RIGHT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter2))))
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

  public static boolean isPathInfix(Definition definition) {
    return PATH_INFIX == definition;
  }

  public static boolean isIso(Definition definition) {
    return ISO == definition;
  }

  public static boolean isCoe(Definition definition) {
    return COERCE == definition;
  }

  public static boolean isTruncP(Definition definition) {
    return PROP_TRUNC == definition;
  }

  public static boolean isTruncS(Definition definition) {
    return SET_TRUNC == definition;
  }

}
