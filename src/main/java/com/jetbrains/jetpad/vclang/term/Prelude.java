package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.ModuleResolvedName;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.*;

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

    /* Path */
    Binding PathLp = new TypedBinding("lp", Lvl());
    Binding PathLh = new TypedBinding("lh", CNat());
    DependentLink PathParameter1 = param("A", Pi(param(Interval()), Universe(new LevelExpression(PathLp), new LevelExpression(PathLh))));
    DependentLink PathParameter2 = param("a", Apps(Reference(PathParameter1), Left()));
    DependentLink PathParameter3 = param("a'", Apps(Reference(PathParameter1), Right()));
    PathParameter1.setNext(PathParameter2);
    PathParameter2.setNext(PathParameter3);
    Preprelude.DefinitionBuilder.Data path = new Preprelude.DefinitionBuilder.Data(PRELUDE, "Path", Abstract.Binding.DEFAULT_PRECEDENCE, new TypeUniverse(new LevelExpression(PathLp), new LevelExpression(PathLh)), PathParameter1, Arrays.asList(PathLp, PathLh));
    PATH = path.definition();

    /* path */
    DependentLink piParam = param("i", Interval());
    DependentLink pathParameter = param(Pi(piParam, Apps(Reference(PathParameter1), Reference(piParam))));
    PATH_CON = path.addConstructor("path", Abstract.Binding.DEFAULT_PRECEDENCE, new TypeUniverse(new LevelExpression(PathLp), new LevelExpression(PathLh)), pathParameter);

    /* = */
    // Binding pathInfixLp = new TypedBinding("lp", Lvl());
    // Binding pathInfixLh = new TypedBinding("lh", CNat());
    DependentLink pathInfixParameter1 = param(false, "A", Universe(new LevelExpression(PathLp), new LevelExpression(PathLh)));
    DependentLink pathInfixParameter2 = param(true, vars("a", "a'"), Reference(pathInfixParameter1));
    pathInfixParameter1.setNext(pathInfixParameter2);
    Expression pathInfixTerm = DataCall(PATH, new LevelSubstitution(PathLp, PathLp, PathLh, PathLh))
        .addArgument(Lam(param("_", Interval()), Reference(pathInfixParameter1)), AppExpression.DEFAULT)
        .addArgument(Reference(pathInfixParameter2), AppExpression.DEFAULT)
        .addArgument(Reference(pathInfixParameter2.getNext()), AppExpression.DEFAULT);
    PATH_INFIX = new Preprelude.DefinitionBuilder.Function(PRELUDE, "=", new Abstract.Binding.Precedence(Abstract.Binding.Associativity.NON_ASSOC, (byte) 0), pathInfixParameter1, Universe(new LevelExpression(PathLp), new LevelExpression(PathLh)), top(pathInfixParameter1, leaf(pathInfixTerm)), Arrays.asList(PathLp, PathLh)).definition();

    /* @ */
    Binding atLp = new TypedBinding("lp", Lvl());
    Binding atLh = new TypedBinding("lh", CNat());
    DependentLink atParameter1 = param(false, "A", Pi(param(Interval()), Universe(new LevelExpression(atLp), new LevelExpression(atLh))));
    DependentLink atParameter2 = param(false, "a", Apps(Reference(atParameter1), Left()));
    DependentLink atParameter3 = param(false, "a'", Apps(Reference(atParameter1), Right()));
    DependentLink atParameter4 = param("p", Apps(DataCall(PATH, new LevelSubstitution(PathLp, atLp, PathLh, atLh)), Reference(atParameter1), Reference(atParameter2), Reference(atParameter3)));
    DependentLink atParameter5 = param("i", Interval());
    atParameter1.setNext(atParameter2);
    atParameter2.setNext(atParameter3);
    atParameter3.setNext(atParameter4);
    atParameter4.setNext(atParameter5);
    DependentLink atPath = param("f", pathParameter.getType());
    Expression atResultType = Apps(Reference(atParameter1), Reference(atParameter5));
    ElimTreeNode atElimTree = top(atParameter1, branch(atParameter5, tail(),
        clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(atParameter2)),
        clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(atParameter3)),
        clause(branch(atParameter3, tail(atParameter5),
            clause(PATH_CON, atPath, Apps(Reference(atPath), Reference(atParameter5)))))
    ));
    AT = new Preprelude.DefinitionBuilder.Function(PRELUDE, "@", new Abstract.Binding.Precedence(Abstract.Binding.Associativity.LEFT_ASSOC, (byte) 9), atParameter1, atResultType, atElimTree, Arrays.asList(atLp, atLh)).definition();

    /* coe */
    Binding coerceLp = new TypedBinding("lp", Lvl());
    Binding coerceLh = new TypedBinding("lh", CNat());
    DependentLink coerceParameter1 = param("type", Pi(param(Interval()), Universe(new LevelExpression(coerceLp), new LevelExpression(coerceLh))));
    DependentLink coerceParameter2 = param("elem", Apps(Reference(coerceParameter1), Left()));
    DependentLink coerceParameter3 = param("point", Interval());
    coerceParameter1.setNext(coerceParameter2);
    coerceParameter2.setNext(coerceParameter3);
    ElimTreeNode coerceElimTreeNode = top(coerceParameter1, branch(coerceParameter3, tail(),
        clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(coerceParameter2))));
    COERCE = new Preprelude.DefinitionBuilder.Function(PRELUDE, "coe", Abstract.Binding.DEFAULT_PRECEDENCE, coerceParameter1, Apps(Reference(coerceParameter1), Reference(coerceParameter3)), coerceElimTreeNode, Arrays.asList(coerceLp, coerceLh)).definition();

    /* iso */
    Binding isoLp = new TypedBinding("lp", Lvl());
    Binding isoLh = new TypedBinding("lh", CNat());
    DependentLink isoParameter1 = param(false, vars("A", "B"), Universe(new LevelExpression(isoLp), new LevelExpression(isoLh)));
    DependentLink isoParameter2 = param("f", Pi(param(Reference(isoParameter1)), Reference(isoParameter1.getNext())));
    DependentLink isoParameter3 = param("g", Pi(param(Reference(isoParameter1.getNext())), Reference(isoParameter1)));
    DependentLink piParamA = param("a", Reference(isoParameter1));
    DependentLink piParamB = param("b", Reference(isoParameter1.getNext()));
    Expression isoParameters4type = FunCall(PATH_INFIX, new LevelSubstitution(PathLp, isoLp, PathLh, isoLh))
        .addArgument(Reference(isoParameter1), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Apps(Reference(isoParameter3), Apps(Reference(isoParameter2), Reference(piParamA))), AppExpression.DEFAULT)
        .addArgument(Reference(piParamA), AppExpression.DEFAULT);
    DependentLink isoParameter4 = param("linv", Pi(piParamA, isoParameters4type));
    Expression isoParameters5type = FunCall(PATH_INFIX, new LevelSubstitution(PathLp, isoLp, PathLh, isoLh))
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
    Expression isoResultType = Universe(new LevelExpression(isoLp), new LevelExpression(isoLh));
    ElimTreeNode isoElimTree = top(isoParameter1, branch(isoParameter5, tail(),
        clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter1)),
        clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter1.getNext()))
    ));
    ISO = new Preprelude.DefinitionBuilder.Function(PRELUDE, "iso", Abstract.Binding.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree, Arrays.asList(isoLp, isoLh)).definition();

    /* TrP, inP */
    Binding truncLp = new TypedBinding("lp", Lvl());
    Binding truncLh = new TypedBinding("lh", CNat());
    DependentLink truncParameter = param("A", Universe(new LevelExpression(truncLp), new LevelExpression(truncLh)));
    Preprelude.DefinitionBuilder.Data propTrunc = new Preprelude.DefinitionBuilder.Data(PRELUDE, "TrP", Abstract.Binding.DEFAULT_PRECEDENCE, TypeUniverse.PROP, truncParameter, Arrays.asList(truncLp, truncLh));
    PROP_TRUNC = propTrunc.definition();
    propTrunc.addConstructor("inP", Abstract.Binding.DEFAULT_PRECEDENCE, new TypeUniverse(new LevelExpression(truncLp), new LevelExpression(truncLh)), param("inP", Reference(truncParameter)));

    /* truncP */
    Expression propTruncConParameterType = DataCall(PROP_TRUNC, new LevelSubstitution(truncLp, truncLp, truncLh, truncLh))
        .addArgument(Reference(truncParameter), AppExpression.DEFAULT);
    DependentLink propTruncConParameter1 = param("a", propTruncConParameterType);
    DependentLink propTruncConParameter2 = param("a'", propTruncConParameterType);
    DependentLink propTruncConParameter3 = param("i", Interval());
    propTruncConParameter1.setNext(propTruncConParameter2);
    propTruncConParameter2.setNext(propTruncConParameter3);
    PROP_TRUNC_PATH_CON = propTrunc.addConstructor("truncP", Abstract.Binding.DEFAULT_PRECEDENCE, new TypeUniverse(new LevelExpression(truncLp), new LevelExpression(truncLh)), propTruncConParameter1);
    Condition propTruncPathCond = new Condition(PROP_TRUNC_PATH_CON, top(propTruncConParameter1, branch(propTruncConParameter3, tail(),
        clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter1)),
        clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter2)))));
    PROP_TRUNC.addCondition(propTruncPathCond);

    /* TrS, inS */
    Preprelude.DefinitionBuilder.Data setTrunc = new Preprelude.DefinitionBuilder.Data(PRELUDE, "TrS", Abstract.Binding.DEFAULT_PRECEDENCE, TypeUniverse.SetOfLevel(new LevelExpression(truncLp)), truncParameter);
    SET_TRUNC = setTrunc.definition();
    setTrunc.addConstructor("inS", Abstract.Binding.DEFAULT_PRECEDENCE, new TypeUniverse(new LevelExpression(truncLp), new LevelExpression(truncLh)), param("inS", Reference(truncParameter)));

    /* truncS */
    Expression setTruncConParameterType = DataCall(SET_TRUNC, new LevelSubstitution(truncLp, truncLp, truncLh, truncLh));
    DependentLink setTruncConParameter1 = param("a", Apps(DataCall(SET_TRUNC), setTruncConParameterType));
    DependentLink setTruncConParameter2 = param("a'", Apps(DataCall(SET_TRUNC), setTruncConParameterType));
    Expression setTruncConParameter3type = FunCall(PATH_INFIX, new LevelSubstitution(PathLp, new LevelExpression(truncLp), PathLh, new LevelExpression(0)))
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
    SET_TRUNC_PATH_CON = setTrunc.addConstructor("truncS", Abstract.Binding.DEFAULT_PRECEDENCE, new TypeUniverse(new LevelExpression(truncLp), new LevelExpression(truncLh)), setTruncConParameter1);
    Condition setTruncPathCond = new Condition(SET_TRUNC_PATH_CON, top(setTruncConParameter1, branch(setTruncConParameter6, tail(),
        clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), FunCall(AT, new LevelSubstitution(atLp, truncLp, atLh, truncLh)).addArgument(Reference(setTruncConParameter3), AppExpression.DEFAULT).addArgument(Reference(setTruncConParameter5), AppExpression.DEFAULT)),
        clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), FunCall(AT, new LevelSubstitution(atLp, truncLp, atLh, truncLh)).addArgument(Reference(setTruncConParameter4), AppExpression.DEFAULT).addArgument(Reference(setTruncConParameter5), AppExpression.DEFAULT)),
        clause(branch(setTruncConParameter5, tail(setTruncConParameter6),
            clause(Preprelude.LEFT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter1)),
            clause(Preprelude.RIGHT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter2))))
    )));
    SET_TRUNC.addCondition(setTruncPathCond);
  }

  private Prelude() {
    super(moduleID);
  }

  public static boolean isAt(Definition definition) {
    return AT == definition;
  }

  public static boolean isPathCon(Definition definition) {
    return PATH_CON.getResolvedName().equals(definition.getResolvedName());
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

  @Override
  public Collection<NamespaceMember> getMembers() {
    throw new IllegalStateException();
  }
}
