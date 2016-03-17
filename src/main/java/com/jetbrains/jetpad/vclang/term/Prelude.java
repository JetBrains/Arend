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

  //public static DataDefinition PROP_TRUNC;
  //public static DataDefinition SET_TRUNC;

  private static char[] specInfix = {'@', '='};
  private static String[] specPrefix = {"iso", "path", "Path", "coe"};

  private static Map<Definition, Integer> defToLevel = new HashMap<>();
  private static Map<Integer, LevelPoly> levels = new HashMap<>();

  public static class LevelPoly {
    public final Definition path;
    public final Definition pathCon;
    public final Definition pathInfix;
    public final Definition at;
    public final Definition coe;
    public final Definition iso;
    public final Definition truncP;
    public final Definition truncS;

    private LevelPoly(Definition path, Definition pathCon, Definition pathInfix, Definition at, Definition coe, Definition iso, Definition truncP, Definition truncS) {
      this.path = path;
      this.pathCon = pathCon;
      this.pathInfix = pathInfix;
      this.at = at;
      this.coe = coe;
      this.iso = iso;
      this.truncP = truncP;
      this.truncS = truncS;
    }
  }

  static {
    PRELUDE_CLASS = new ClassDefinition(new ModuleResolvedName(moduleID));

    NAT = new DataDefinition(new DefinitionResolvedName(PRELUDE, "Nat"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), EmptyDependentLink.getInstance());
    Namespace natNamespace = PRELUDE.getChild(NAT.getName());
    ZERO = new Constructor(new DefinitionResolvedName(natNamespace, "zero"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance(), NAT);
    SUC = new Constructor(new DefinitionResolvedName(natNamespace, "suc"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), param(DataCall(NAT)), NAT);

    PRELUDE.addDefinition(NAT);
    PRELUDE.addMember(NAT.addConstructor(ZERO));
    PRELUDE.addMember(NAT.addConstructor(SUC));


    INTERVAL = new DataDefinition(new DefinitionResolvedName(PRELUDE, "I"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance());
    Namespace intervalNamespace = PRELUDE.getChild(INTERVAL.getName());
    LEFT = new Constructor(new DefinitionResolvedName(intervalNamespace, "left"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance(), INTERVAL);
    RIGHT = new Constructor(new DefinitionResolvedName(intervalNamespace, "right"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance(), INTERVAL);
    ABSTRACT = new Constructor(new DefinitionResolvedName(intervalNamespace, "<abstract>"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance(), INTERVAL);

    PRELUDE.addDefinition(INTERVAL);
    PRELUDE.addMember(INTERVAL.addConstructor(LEFT));
    PRELUDE.addMember(INTERVAL.addConstructor(RIGHT));
    INTERVAL.addConstructor(ABSTRACT);

    DependentLink isoParameter1 = param(false, vars("A", "B"), Universe(0, Universe.Type.PROP));
    DependentLink isoParameter2 = param("f", Pi(param(Reference(isoParameter1)), Reference(isoParameter1.getNext())));
    DependentLink isoParameter3 = param("g", Pi(param(Reference(isoParameter1.getNext())), Reference(isoParameter1)));
    DependentLink isoParameter4 = param("i", DataCall(INTERVAL));
    isoParameter1.setNext(isoParameter2);
    isoParameter2.setNext(isoParameter3);
    isoParameter3.setNext(isoParameter4);
    Expression isoResultType = Universe(0, Universe.Type.PROP);
    ElimTreeNode isoElimTree = top(isoParameter1, branch(isoParameter4, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter1)),
            clause(RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter1.getNext()))
    ));
    FunctionDefinition iso = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "isoP"), Abstract.Definition.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree);
    PRELUDE.addDefinition(iso);

    generateLevel(0);
    PATH = (DataDefinition) levels.get(0).path;
    PATH_CON = (Constructor) levels.get(0).pathCon;
    PATH_INFIX = (FunctionDefinition) levels.get(0).pathInfix;
    AT = (FunctionDefinition) levels.get(0).at;
    ISO = (FunctionDefinition) levels.get(0).iso;
    COERCE = (FunctionDefinition) levels.get(0).coe;
  }

  private Prelude() {
    super(moduleID);
  }

  @Override
  public Collection<NamespaceMember> getMembers() {
    throw new IllegalStateException();
  }

  @Override
  public NamespaceMember getMember(String name) {
    NamespaceMember result = super.getMember(name);
    if (result != null)
      return result;
    for (char sc : specInfix) {
      if (name.charAt(0) == sc) {
        for (char c : name.toCharArray()) {
          if (sc != c)
            return null;
        }
        generateLevel(name.length() - 1);
        return getMember(name);
      }
    }
    for (String sname : specPrefix) {
      if (name.startsWith(sname)) {
        if (name.length() == sname.length()) {
          generateLevel(0);
          return getMember(name);
        }
        String suffix = name.substring(sname.length());
        try {
          Integer level = Integer.parseInt(suffix);
          if (level > 0) {
            generateLevel(level);
            return getMember(name);
          }
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }
    return null;
  }

  private static void generateLevel(int i) {
    String suffix = i == 0 ? "" : Integer.toString(i);
    DependentLink PathParameter1 = param("A", Pi(param(DataCall(INTERVAL)), Universe(i, Universe.Type.NOT_TRUNCATED)));
    DependentLink PathParameter2 = param("a", Apps(Reference(PathParameter1), ConCall(LEFT)));
    DependentLink PathParameter3 = param("a'", Apps(Reference(PathParameter1), ConCall(RIGHT)));
    PathParameter1.setNext(PathParameter2);
    PathParameter2.setNext(PathParameter3);
    DataDefinition path = new DataDefinition(new DefinitionResolvedName(PRELUDE, "Path" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), PathParameter1);
    PRELUDE.addDefinition(path);
    defToLevel.put(path, i);

    DependentLink piParam = param("i", DataCall(INTERVAL));
    DependentLink pathParameters = param(Pi(piParam, Apps(Reference(PathParameter1), Reference(piParam))));
    Constructor pathCon = new Constructor(new DefinitionResolvedName(PRELUDE.getChild(path.getName()), "path" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), pathParameters, path);
    PRELUDE.addMember(path.addConstructor(pathCon));
    defToLevel.put(pathCon, i);

    char[] chars = new char[i + 1];

    DependentLink pathInfixParameter1 = param(false, "A", Universe(i, Universe.Type.NOT_TRUNCATED));
    DependentLink pathInfixParameter2 = param(true, vars("a", "a'"), Reference(pathInfixParameter1));
    pathInfixParameter1.setNext(pathInfixParameter2);
    Expression pathInfixTerm = Apps(DataCall((DataDefinition) PRELUDE.getDefinition("Path" + suffix)), Lam(param("_", DataCall(INTERVAL)), Reference(pathInfixParameter1)), Reference(pathInfixParameter2), Reference(pathInfixParameter2.getNext()));
    Arrays.fill(chars, '=');
    FunctionDefinition pathInfix = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, new String(chars)), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixParameter1, Universe(i), top(pathInfixParameter1, leaf(pathInfixTerm)));

    PRELUDE.addDefinition(pathInfix);
    defToLevel.put(pathInfix, i);

    DependentLink atParameter1 = param(false, "A", PathParameter1.getType());
    DependentLink atParameter2 = param(false, "a", Apps(Reference(atParameter1), ConCall(LEFT)));
    DependentLink atParameter3 = param(false, "a'", Apps(Reference(atParameter1), ConCall(RIGHT)));
    DependentLink atParameter4 = param("p", Apps(DataCall((DataDefinition) PRELUDE.getDefinition("Path" + suffix)), Reference(atParameter1), Reference(atParameter2), Reference(atParameter3)));
    DependentLink atParameter5 = param("i", DataCall(INTERVAL));
    atParameter1.setNext(atParameter2);
    atParameter2.setNext(atParameter3);
    atParameter3.setNext(atParameter4);
    atParameter4.setNext(atParameter5);
    DependentLink atPath = param("f", pathParameters.getType());
    Expression atResultType = Apps(Reference(atParameter1), Reference(atParameter5));
    ElimTreeNode atElimTree = top(atParameter1, branch(atParameter5, tail(),
      clause(LEFT, EmptyDependentLink.getInstance(), Reference(atParameter2)),
      clause(RIGHT, EmptyDependentLink.getInstance(), Reference(atParameter3)),
      clause(branch(atParameter4, tail(atParameter5),
          clause((Constructor) PRELUDE.getDefinition("path" + suffix), atPath, Apps(Reference(atPath), Reference(atParameter5)))))
    ));
    Arrays.fill(chars, '@');
    FunctionDefinition at = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, new String(chars)), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atParameter1, atResultType, atElimTree);
    PRELUDE.addDefinition(at);
    defToLevel.put(at, i);

    DependentLink coerceParameter1 = param("type", Pi(param(DataCall(INTERVAL)), Universe(i, Universe.Type.NOT_TRUNCATED)));
    DependentLink coerceParameter2 = param("elem", Apps(Reference(coerceParameter1), ConCall(LEFT)));
    DependentLink coerceParameter3 = param("point", DataCall(INTERVAL));
    coerceParameter1.setNext(coerceParameter2);
    coerceParameter2.setNext(coerceParameter3);
    ElimTreeNode coerceElimTreeNode = top(coerceParameter1, branch(coerceParameter3, tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(coerceParameter2))));
    FunctionDefinition coe = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "coe" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, coerceParameter1, Apps(Reference(coerceParameter1), Reference(coerceParameter3)), coerceElimTreeNode);
    PRELUDE.addDefinition(coe);
    defToLevel.put(coe, i);

    DependentLink isoParameter1 = param(false, vars("A", "B"), Universe(i, Universe.Type.NOT_TRUNCATED));
    DependentLink isoParameter2 = param("f", Pi(param(Reference(isoParameter1)), Reference(isoParameter1.getNext())));
    DependentLink isoParameter3 = param("g", Pi(param(Reference(isoParameter1.getNext())), Reference(isoParameter1)));
    DependentLink piParamA = param("a", Reference(isoParameter1));
    DependentLink piParamB = param("b", Reference(isoParameter1.getNext()));
    Expression isoParameters4type = FunCall(pathInfix)
      .addArgument(Reference(isoParameter1), EnumSet.noneOf(AppExpression.Flag.class))
      .addArgument(Apps(Reference(isoParameter3), Apps(Reference(isoParameter2), Reference(piParamA))), AppExpression.DEFAULT)
      .addArgument(Reference(piParamA), AppExpression.DEFAULT);
    DependentLink isoParameter4 = param("linv", Pi(piParamA, isoParameters4type));
    Expression isoParameters5type = FunCall(pathInfix)
      .addArgument(Reference(isoParameter1.getNext()), EnumSet.noneOf(AppExpression.Flag.class))
      .addArgument(Apps(Reference(isoParameter2), Apps(Reference(isoParameter3), Reference(piParamB))), AppExpression.DEFAULT)
      .addArgument(Reference(piParamB), AppExpression.DEFAULT);
    DependentLink isoParameter5 = param("rinv", Pi(piParamB, isoParameters5type));
    DependentLink isoParameter6 = param("i", DataCall(INTERVAL));
    isoParameter1.setNext(isoParameter2);
    isoParameter2.setNext(isoParameter3);
    isoParameter3.setNext(isoParameter4);
    isoParameter4.setNext(isoParameter5);
    isoParameter5.setNext(isoParameter6);
    Expression isoResultType = Universe(i, Universe.Type.NOT_TRUNCATED);
    ElimTreeNode isoElimTree = top(isoParameter1, branch(isoParameter6, tail(),
      clause(LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter1)),
      clause(RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter1.getNext()))
    ));
    FunctionDefinition iso = new FunctionDefinition(new DefinitionResolvedName(PRELUDE, "iso" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree);
    PRELUDE.addDefinition(iso);
    defToLevel.put(iso, i);

    DependentLink truncParameter = param("A", Universe(i, Universe.Type.NOT_TRUNCATED));
    DataDefinition propTrunc = new DataDefinition(PRELUDE.getChild("TrP" + suffix).getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), truncParameter);
    PRELUDE.addDefinition(propTrunc);
    defToLevel.put(propTrunc, i);

    Constructor propTruncInCon = new Constructor(PRELUDE.getChild(propTrunc.getName()).getChild("inP" + suffix).getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), param("inP", Reference(truncParameter)), propTrunc);
    DependentLink propTruncConParameter1 = param("a", Apps(DataCall(propTrunc), Reference(truncParameter)));
    DependentLink propTruncConParameter2 = param("a'", Apps(DataCall(propTrunc), Reference(truncParameter)));
    DependentLink propTruncConParameter3 = param("i", DataCall(INTERVAL));
    propTruncConParameter1.setNext(propTruncConParameter2);
    propTruncConParameter2.setNext(propTruncConParameter3);
    Constructor propTruncPathCon = new Constructor(PRELUDE.getChild(propTrunc.getName()).getChild("truncP" + suffix).getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), propTruncConParameter1, propTrunc);
    PRELUDE.addMember(propTrunc.addConstructor(propTruncInCon));
    PRELUDE.addMember(propTrunc.addConstructor(propTruncPathCon));
    Condition propTruncPathCond = new Condition(propTruncPathCon, top(propTruncConParameter1, branch(propTruncConParameter3, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter1)),
            clause(RIGHT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter2)))));
    propTrunc.addCondition(propTruncPathCond);

    defToLevel.put(propTruncInCon, i);
    defToLevel.put(propTruncPathCon, i);

    DataDefinition setTrunc = new DataDefinition(PRELUDE.getChild("TrS" + suffix).getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), truncParameter);
    PRELUDE.addDefinition(setTrunc);
    defToLevel.put(setTrunc, i);

    Constructor setTruncInCon = new Constructor(PRELUDE.getChild(setTrunc.getName()).getChild("inS" + suffix).getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), param("inS", Reference(truncParameter)), setTrunc);
    DependentLink setTruncConParameter1 = param("a", Apps(DataCall(setTrunc), Reference(truncParameter)));
    DependentLink setTruncConParameter2 = param("a'", Apps(DataCall(setTrunc), Reference(truncParameter)));
    Expression setTruncConParameter3type = FunCall(pathInfix)
      .addArgument(Apps(DataCall(setTrunc), Reference(truncParameter)), EnumSet.noneOf(AppExpression.Flag.class))
      .addArgument(Reference(setTruncConParameter1), AppExpression.DEFAULT)
      .addArgument(Reference(setTruncConParameter2), AppExpression.DEFAULT);
    DependentLink setTruncConParameter3 = param("p", setTruncConParameter3type);
    Expression setTruncConParameter4type = FunCall(pathInfix)
      .addArgument(Apps(DataCall(setTrunc), Reference(truncParameter)), EnumSet.noneOf(AppExpression.Flag.class))
      .addArgument(Reference(setTruncConParameter1), AppExpression.DEFAULT)
      .addArgument(Reference(setTruncConParameter2), AppExpression.DEFAULT);
    DependentLink setTruncConParameter4 = param("q", setTruncConParameter4type);
    DependentLink setTruncConParameter5 = param("i", DataCall(INTERVAL));
    DependentLink setTruncConParameter6 = param("j", DataCall(INTERVAL));
    setTruncConParameter1.setNext(setTruncConParameter2);
    setTruncConParameter2.setNext(setTruncConParameter3);
    setTruncConParameter3.setNext(setTruncConParameter4);
    setTruncConParameter4.setNext(setTruncConParameter5);
    setTruncConParameter5.setNext(setTruncConParameter6);
    Constructor setTruncPathCon = new Constructor(PRELUDE.getChild(setTrunc.getName()).getChild("truncS" + suffix).getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), setTruncConParameter1, setTrunc);
    PRELUDE.addMember(setTrunc.addConstructor(setTruncInCon));
    PRELUDE.addMember(setTrunc.addConstructor(setTruncPathCon));
    Condition setTruncPathCond = new Condition(setTruncPathCon, top(setTruncConParameter1, branch(setTruncConParameter6, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Apps(FunCall(at), Reference(setTruncConParameter3), Reference(setTruncConParameter5))),
            clause(RIGHT, EmptyDependentLink.getInstance(), Apps(FunCall(at), Reference(setTruncConParameter4), Reference(setTruncConParameter5))),
            clause(branch(setTruncConParameter5, tail(setTruncConParameter6),
                    clause(LEFT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter1)),
                    clause(RIGHT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter2))))
    )));

    setTrunc.addCondition(setTruncPathCond);

    defToLevel.put(setTruncInCon, i);
    defToLevel.put(setTruncPathCon, i); /**/

    levels.put(i, new LevelPoly(path, pathCon, pathInfix, at, coe, iso, propTruncPathCon, setTruncPathCon));
  }

  public static boolean isAt(Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).at == definition;
  }

  public static boolean isPathCon(Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).pathCon == definition;
  }

  public static boolean isPath(Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).path == definition;
  }

  public static boolean isPathInfix(Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).pathInfix == definition;
  }

  public static boolean isIso(Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).iso == definition;
  }

  public static boolean isCoe(Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).coe == definition;
  }

  public static boolean isTruncP(Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).truncP == definition;
  }

  public static boolean isTruncS(Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).truncS == definition;
  }

  public static int getLevel(Definition definition) {
    if (defToLevel.containsKey(definition)) {
      return defToLevel.get(definition);
    }

    throw new IllegalStateException();
  }

  public static LevelPoly getLevelDefs(int level) {
    return levels.get(level);
  }
}
