package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;

import java.util.Arrays;
import java.util.Collection;

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

  private static char[] specInfix = {'@', '='};
  private static String[] specPrefix = {"iso", "path", "Path"};

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

    DependentLink coerceParameter1 = param("type", Pi(param(DataCall(INTERVAL)), Universe(Universe.NO_LEVEL)));
    DependentLink coerceParameter2 = param("elem", Apps(Reference(coerceParameter1), ConCall(LEFT)));
    DependentLink coerceParameter3 = param("point", DataCall(INTERVAL));
    coerceParameter1.setNext(coerceParameter2);
    coerceParameter2.setNext(coerceParameter3);
    BranchElimTreeNode coerceElimTreeNode = branch(coerceParameter3, tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(coerceParameter2)));
    COERCE = new FunctionDefinition(PRELUDE, new Name("coe"), Abstract.Definition.DEFAULT_PRECEDENCE, coerceParameter1, Apps(Reference(coerceParameter1), Reference(coerceParameter3)), coerceElimTreeNode);

    PRELUDE.addDefinition(COERCE);

    PATH = (DataDefinition) PRELUDE.getDefinition("Path");
    PATH_CON = (Constructor) PRELUDE.getDefinition("path");
    PATH_INFIX = (FunctionDefinition) PRELUDE.getDefinition("=");
    AT = (FunctionDefinition) PRELUDE.getDefinition("@");
    ISO = (FunctionDefinition) PRELUDE.getDefinition("iso");
  }

  private Prelude() {
    super("Prelude");
  }

  @Override
  public Collection<NamespaceMember> getMembers() {
    throw new IllegalStateException();
  }

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
        try {
          Integer level = Integer.parseInt(name.substring(sname.length()));
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

  private void generateLevel(int i) {
    String suffix = i == 0 ? "" : Integer.toString(i);
    DependentLink PathParameter1 = param("A", Pi(param(DataCall(INTERVAL)), Universe(i, Universe.Type.NOT_TRUNCATED)));
    DependentLink PathParameter2 = param(Apps(Reference(PathParameter1), ConCall(LEFT)));
    DependentLink PathParameter3 = param(Apps(Reference(PathParameter1), ConCall(RIGHT)));
    PathParameter1.setNext(PathParameter2);
    PathParameter2.setNext(PathParameter3);
    DataDefinition path = new DataDefinition(PRELUDE, new Name("Path" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), PathParameter1);
    PRELUDE.addDefinition(path);

    DependentLink piParam = param("i", DataCall(INTERVAL));
    DependentLink pathParameters = param(Pi(piParam, Apps(Reference(PathParameter1), Reference(piParam))));
    Constructor pathCon = new Constructor(PRELUDE.getChild(path.getName()), new Name("path" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), pathParameters, path);
    path.addConstructor(pathCon);
    PRELUDE.addDefinition(pathCon);

    char[] chars = new char[i + 1];

    DependentLink pathInfixParameter1 = param(false, "A", Universe(i, Universe.Type.NOT_TRUNCATED));
    DependentLink pathInfixParameter2 = param(true, vars("a", "a'"), Reference(pathInfixParameter1));
    pathInfixParameter1.setNext(pathInfixParameter2);
    Expression pathInfixTerm = Apps(DataCall((DataDefinition) PRELUDE.getDefinition("Path" + suffix)), Lam(param("_", DataCall(INTERVAL)), Reference(pathInfixParameter1)), Reference(pathInfixParameter2), Reference(pathInfixParameter2.getNext()));
    Arrays.fill(chars, '=');
    FunctionDefinition pathInfix = new FunctionDefinition(PRELUDE, new Name(new String(chars), Abstract.Definition.Fixity.INFIX), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixParameter1, Universe(i), leaf(pathInfixTerm));

    PRELUDE.addDefinition(pathInfix);

    DependentLink atParameter1 = param(false, "A", PathParameter1.getType());
    DependentLink atParameter2 = param(false, "a", PathParameter2.getType());
    DependentLink atParameter3 = param(false, "a'", PathParameter3.getType());
    DependentLink atParameter4 = param("p", Apps(DataCall((DataDefinition) PRELUDE.getDefinition("Path" + suffix)), Reference(atParameter1), Reference(atParameter2), Reference(atParameter3)));
    DependentLink atParameter5 = param("i", DataCall(INTERVAL));
    atParameter1.setNext(atParameter2);
    atParameter2.setNext(atParameter3);
    atParameter3.setNext(atParameter4);
    atParameter4.setNext(atParameter5);
    Expression atResultType = Apps(Reference(atParameter1), Reference(atParameter5));
    BranchElimTreeNode atElimTree = branch(atParameter5, tail(),
      clause(LEFT, EmptyDependentLink.getInstance(), Reference(atParameter2)),
      clause(RIGHT, EmptyDependentLink.getInstance(), Reference(atParameter3)),
      clause(branch(atParameter4, tail(),
          clause((Constructor) PRELUDE.getDefinition("path" + suffix), EmptyDependentLink.getInstance(), Apps(Reference(atParameter4), Reference(atParameter5)))))
    );
    Arrays.fill(chars, '@');
    FunctionDefinition at = new FunctionDefinition(PRELUDE, new Name(new String(chars), Abstract.Definition.Fixity.INFIX), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atParameter1, atResultType, atElimTree);
    PRELUDE.addDefinition(at);

    DependentLink isoParameter1 = param(false, vars("A", "B"), Universe(i, Universe.Type.NOT_TRUNCATED));
    DependentLink isoParameter2 = param("f", Pi(param(Reference(isoParameter1)), Reference(isoParameter1.getNext())));
    DependentLink isoParameter3 = param("g", Pi(param(Reference(isoParameter1.getNext())), Reference(isoParameter1)));
    DependentLink piParamA = param("a", Reference(isoParameter1));
    DependentLink piParamB = param("b", Reference(isoParameter1.getNext()));
    DependentLink isoParameter4 = param("linv", Pi(piParamA, Apps(Apps(FunCall(PATH_INFIX), new ArgumentExpression(Reference(isoParameter1), false, true)), Apps(Reference(isoParameter3), Apps(Reference(isoParameter2), Reference(piParamA)), Reference(piParamA)))));
    DependentLink isoParameter5 = param("rinv", Pi(piParamB, Apps(Apps(FunCall(PATH_INFIX), new ArgumentExpression(Reference(isoParameter1.getNext()), false, true)), Apps(Reference(isoParameter2), Apps(Reference(isoParameter3), Reference(piParamB)), Reference(piParamB)))));
    DependentLink isoParameter6 = param("i", DataCall(INTERVAL));
    isoParameter1.setNext(isoParameter2);
    isoParameter2.setNext(isoParameter3);
    isoParameter3.setNext(isoParameter4);
    isoParameter4.setNext(isoParameter5);
    isoParameter5.setNext(isoParameter6);
    Expression isoResultType = Universe(i, Universe.Type.NOT_TRUNCATED);
    BranchElimTreeNode isoElimTree = branch(isoParameter6, tail(),
      clause(LEFT, EmptyDependentLink.getInstance(), Reference(isoParameter1)),
      clause(RIGHT, EmptyDependentLink.getInstance(), Reference(isoParameter1.getNext()))
    );
    FunctionDefinition iso = new FunctionDefinition(PRELUDE, new Name("iso" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, isoParameter1, isoResultType, isoElimTree);
    PRELUDE.addDefinition(iso);
  }

  public static boolean isAt(Definition definition) {
    return isSpec(definition, "@");
  }

  public static boolean isPathCon(Definition definition) {
    return isSpec(definition, "path");
  }

  public static boolean isPath(Definition definition) {
    return isSpec(definition, "Path");
  }

  public static boolean isPathInfix(Definition definition) {
    return isSpec(definition, "=");
  }

  public static boolean isIso(Definition definition) {
    return isSpec(definition, "iso");
  }

  private static boolean isSpec(Definition definition, String prefix) {
    return definition != null && definition == PRELUDE.getDefinition(definition.getName()) && definition.getName().startsWith(prefix);
  }

  public static int getLevel(Definition definition) {
    for (char c : specInfix) {
      if (isSpec(definition, Character.toString(c))) {
        return definition.getName().length() - 1;
      }
    }
    for (String name : specPrefix) {
      if (isSpec(definition, name)) {
        return definition.getName().length() == name.length() ? 0 : Integer.parseInt(definition.getName().substring(name.length()));
      }
    }

    throw new IllegalStateException();
  }
}
