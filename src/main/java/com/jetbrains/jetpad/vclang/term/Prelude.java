package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.lang.ref.Reference;
import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude extends Namespace {
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
    PRELUDE_CLASS = new ClassDefinition(RootModule.ROOT, new Name("Prelude"));
    RootModule.ROOT.addDefinition(PRELUDE_CLASS);

    NAT = new DataDefinition(PRELUDE, new Name("Nat"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), EmptyDependentLink.getInstance());
    Namespace natNamespace = PRELUDE.getChild(NAT.getName());
    ZERO = new Constructor(natNamespace, new Name("zero"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance(), NAT);
    SUC = new Constructor(natNamespace, new Name("suc"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), param(DataCall(NAT)), NAT);
    NAT.addConstructor(ZERO);
    NAT.addConstructor(SUC);

    PRELUDE.addDefinition(NAT);
    PRELUDE.addDefinition(ZERO);
    PRELUDE.addDefinition(SUC);

    INTERVAL = new DataDefinition(PRELUDE, new Name("I"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance());
    Namespace intervalNamespace = PRELUDE.getChild(INTERVAL.getName());
    LEFT = new Constructor(intervalNamespace, new Name("left"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance(), INTERVAL);
    RIGHT = new Constructor(intervalNamespace, new Name("right"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance(), INTERVAL);
    ABSTRACT = new Constructor(intervalNamespace, new Name("<abstract>"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), EmptyDependentLink.getInstance(), INTERVAL);
    INTERVAL.addConstructor(LEFT);
    INTERVAL.addConstructor(RIGHT);
    INTERVAL.addConstructor(ABSTRACT);

    PRELUDE.addDefinition(INTERVAL);
    PRELUDE.addDefinition(LEFT);
    PRELUDE.addDefinition(RIGHT);

    PATH = (DataDefinition) PRELUDE.getDefinition("Path");
    PATH_CON = (Constructor) PRELUDE.getDefinition("path");
    PATH_INFIX = (FunctionDefinition) PRELUDE.getDefinition("=");
    AT = (FunctionDefinition) PRELUDE.getDefinition("@");
    ISO = (FunctionDefinition) PRELUDE.getDefinition("iso");
    COERCE = (FunctionDefinition) PRELUDE.getDefinition("coe");
  }

  private Prelude() {
    super("Prelude");
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
    DataDefinition path = new DataDefinition(PRELUDE, new Name("Path" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), PathParameter1);
    PRELUDE.addDefinition(path);
    defToLevel.put(path, i);

    DependentLink piParam = param("i", DataCall(INTERVAL));
    DependentLink pathParameters = param(Pi(piParam, Apps(Reference(PathParameter1), Reference(piParam))));
    Constructor pathCon = new Constructor(PRELUDE.getChild(path.getName()), new Name("path" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), pathParameters, path);
    path.addConstructor(pathCon);
    PRELUDE.addDefinition(pathCon);
    defToLevel.put(pathCon, i);

    char[] chars = new char[i + 1];

    DependentLink pathInfixParameter1 = param(false, "A", Universe(i, Universe.Type.NOT_TRUNCATED));
    DependentLink pathInfixParameter2 = param(true, vars("a", "a'"), Reference(pathInfixParameter1));
    pathInfixParameter1.setNext(pathInfixParameter2);
    Expression pathInfixTerm = Apps(DataCall((DataDefinition) PRELUDE.getDefinition("Path" + suffix)), Lam(param("_", DataCall(INTERVAL)), Reference(pathInfixParameter1)), Reference(pathInfixParameter2), Reference(pathInfixParameter2.getNext()));
    Arrays.fill(chars, '=');
    FunctionDefinition pathInfix = new FunctionDefinition(PRELUDE, new Name(new String(chars), Abstract.Definition.Fixity.INFIX), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixParameter1, Universe(i), top(pathInfixParameter1, leaf(pathInfixTerm)));
    PRELUDE.addDefinition(pathInfix);
    defToLevel.put(pathInfix, i);

    DependentLink atParameter1 = param(false, "A", PathParameter1.getType());
    DependentLink atParameter2 = param(false, "a", PathParameter2.getType());
    DependentLink atParameter3 = param(false, "a'", PathParameter3.getType());
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
    FunctionDefinition at = new FunctionDefinition(PRELUDE, new Name(new String(chars), Abstract.Definition.Fixity.INFIX), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atParameter1, atResultType, atElimTree);
    PRELUDE.addDefinition(at);
    defToLevel.put(at, i);

    DependentLink coerceParameter1 = param("type", Pi(param(DataCall(INTERVAL)), Universe(i, Universe.Type.NOT_TRUNCATED)));
    DependentLink coerceParameter2 = param("elem", Apps(Reference(coerceParameter1), ConCall(LEFT)));
    DependentLink coerceParameter3 = param("point", DataCall(INTERVAL));
    coerceParameter1.setNext(coerceParameter2);
    coerceParameter2.setNext(coerceParameter3);
    ElimTreeNode coerceElimTreeNode = top(coerceParameter1, branch(coerceParameter3, tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(coerceParameter2))));
    FunctionDefinition coe = new FunctionDefinition(PRELUDE, new Name("coe" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, coerceParameter1, Apps(Reference(coerceParameter1), Reference(coerceParameter3)), coerceElimTreeNode);
    PRELUDE.addDefinition(coe);
    defToLevel.put(coe, i);

    DependentLink isoParameter1 = param(false, vars("A", "B"), Universe(i, Universe.Type.NOT_TRUNCATED));
    DependentLink isoParameter2 = param("f", Pi(param(Reference(isoParameter1)), Reference(isoParameter1.getNext())));
    DependentLink isoParameter3 = param("g", Pi(param(Reference(isoParameter1.getNext())), Reference(isoParameter1)));
    DependentLink piParamA = param("a", Reference(isoParameter1));
    DependentLink piParamB = param("b", Reference(isoParameter1.getNext()));
    DependentLink isoParameter4 = param("linv", Pi(piParamA, Apps(Apps(FunCall(pathInfix), new ArgumentExpression(Reference(isoParameter1), false, true)), Apps(Reference(isoParameter3), Apps(Reference(isoParameter2), Reference(piParamA))), Reference(piParamA))));
    DependentLink isoParameter5 = param("rinv", Pi(piParamB, Apps(Apps(FunCall(pathInfix), new ArgumentExpression(Reference(isoParameter1.getNext()), false, true)), Apps(Reference(isoParameter2), Apps(Reference(isoParameter3), Reference(piParamB))), Reference(piParamB))));
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
    FunctionDefinition iso = new FunctionDefinition(PRELUDE, new Name("iso" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree);
    PRELUDE.addDefinition(iso);
    defToLevel.put(iso, i);

    DependentLink truncParameter = param("A", Universe(i, Universe.Type.NOT_TRUNCATED));
    DataDefinition propTrunc = new DataDefinition(PRELUDE, new Name("TrP" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), truncParameter);
    PRELUDE.addDefinition(propTrunc);
    defToLevel.put(propTrunc, i);

    Constructor propTruncInCon = new Constructor(PRELUDE.getChild(propTrunc.getName()), new Name("inP" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), truncParameter, propTrunc);
    DependentLink propTruncConParameter1 = param("a", Apps(DataCall(propTrunc), Reference(truncParameter)));
    DependentLink propTruncConParameter2 = param("a'", Apps(DataCall(propTrunc), Reference(truncParameter)));
    DependentLink propTruncConParameter3 = param("i", DataCall(INTERVAL));
    propTruncConParameter1.setNext(propTruncConParameter2);
    propTruncConParameter2.setNext(propTruncConParameter3);
    Constructor propTruncPathCon = new Constructor(PRELUDE.getChild(propTrunc.getName()), new Name("truncP" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), propTruncConParameter1, propTrunc);
    propTrunc.addConstructor(propTruncInCon);
    propTrunc.addConstructor(propTruncPathCon);
    Condition propTruncPathCond = new Condition(propTruncPathCon, top(propTruncConParameter1, branch(propTruncConParameter3, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter1)),
            clause(RIGHT, EmptyDependentLink.getInstance(), Reference(propTruncConParameter2)))));
    propTrunc.addCondition(propTruncPathCond);

    PRELUDE.addDefinition(propTruncInCon);
    PRELUDE.addDefinition(propTruncPathCon);

    defToLevel.put(propTruncInCon, i);
    defToLevel.put(propTruncPathCon, i);

    DataDefinition setTrunc = new DataDefinition(PRELUDE, new Name("TrS" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), truncParameter);
    PRELUDE.addDefinition(setTrunc);
    defToLevel.put(setTrunc, i);

    Constructor setTruncInCon = new Constructor(PRELUDE.getChild(setTrunc.getName()), new Name("inS" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), truncParameter, setTrunc);
    DependentLink setTruncConParameter1 = param("a", Apps(DataCall(setTrunc), Reference(truncParameter)));
    DependentLink setTruncConParameter2 = param("a'", Apps(DataCall(setTrunc), Reference(truncParameter)));
    DependentLink setTruncConParameter3 = param("p", Apps(Apps(FunCall(pathInfix), new ArgumentExpression(Apps(DataCall(setTrunc), Reference(truncParameter)), false, true)), Reference(setTruncConParameter1), Reference(setTruncConParameter2)));
    DependentLink setTruncConParameter4 = param("q", Apps(Apps(FunCall(pathInfix), new ArgumentExpression(Apps(DataCall(setTrunc), Reference(truncParameter)), false, true)), Reference(setTruncConParameter1), Reference(setTruncConParameter2)));
    DependentLink setTruncConParameter5 = param("i", DataCall(INTERVAL));
    DependentLink setTruncConParameter6 = param("j", DataCall(INTERVAL));
    setTruncConParameter1.setNext(setTruncConParameter2);
    setTruncConParameter2.setNext(setTruncConParameter3);
    setTruncConParameter3.setNext(setTruncConParameter4);
    setTruncConParameter4.setNext(setTruncConParameter5);
    setTruncConParameter5.setNext(setTruncConParameter6);
    Constructor setTruncPathCon = new Constructor(PRELUDE.getChild(setTrunc.getName()), new Name("truncS" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), setTruncConParameter1, setTrunc);
    setTrunc.addConstructor(setTruncInCon);
    setTrunc.addConstructor(setTruncPathCon);
    Condition setTruncPathCond = new Condition(setTruncPathCon, top(setTruncConParameter1, branch(setTruncConParameter6, tail(),
            clause(LEFT, EmptyDependentLink.getInstance(), Apps(FunCall(at), Reference(setTruncConParameter3), Reference(setTruncConParameter5))),
            clause(RIGHT, EmptyDependentLink.getInstance(), Apps(FunCall(at), Reference(setTruncConParameter4), Reference(setTruncConParameter5))),
            clause(branch(setTruncConParameter5, tail(setTruncConParameter6),
                    clause(LEFT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter1)),
                    clause(RIGHT, EmptyDependentLink.getInstance(), Reference(setTruncConParameter2))))
    )));

    setTrunc.addCondition(setTruncPathCond);

    PRELUDE.addDefinition(setTruncInCon);
    PRELUDE.addDefinition(setTruncPathCon);

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
