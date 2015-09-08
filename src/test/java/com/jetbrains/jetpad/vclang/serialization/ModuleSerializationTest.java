package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.module.SimpleModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class ModuleSerializationTest {
  SimpleModuleLoader moduleLoader;
  @Before
  public void initialize() {
    RootModule.initialize();
    moduleLoader = new SimpleModuleLoader(false);
  }

  @Test
  public void serializeExprTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    Expression term = Lam(lamArgs(Tele(false, vars("x", "y"), Nat()), Tele(vars("z"), Pi(Nat(), Nat()))), Pi(args(Tele(vars("A"), Universe()), TypeArg(false, Index(0))), Index(1)));
    FunctionDefinition functionDefinition = new FunctionDefinition(def.getLocalNamespace().getChild(new Utils.Name("f")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, term);
    def.getLocalNamespace().addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    RootModule.ROOT.addDefinition(newDef);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef.getNamespace(), newDef);
    assertEquals(0, errors);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) def.getLocalNamespace().getDefinition("f")).getTerm(), ((FunctionDefinition) newDef.getLocalNamespace().getDefinition("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void serializeElimTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    List<Clause> clauses1 = new ArrayList<>(2);
    ElimExpression term1 = Elim(Index(0), clauses1);
    List<Clause> clauses2 = new ArrayList<>(2);
    ElimExpression term2 = Elim(Index(1), clauses2);
    clauses1.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Zero(), term1));
    clauses1.add(new Clause(match(Prelude.SUC, match("x")), Abstract.Definition.Arrow.LEFT, term2, term1));
    clauses2.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Index(0), term2));
    clauses2.add(new Clause(match(Prelude.SUC, match("x")), Abstract.Definition.Arrow.LEFT, Suc(Index(0)), term2));
    FunctionDefinition functionDefinition = new FunctionDefinition(def.getLocalNamespace().getChild(new Utils.Name("f")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, term1);
    def.getLocalNamespace().addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    RootModule.ROOT.addDefinition(newDef);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef.getNamespace(), newDef);
    assertEquals(0, errors);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) def.getLocalNamespace().getDefinition("f")).getTerm(), ((FunctionDefinition) newDef.getLocalNamespace().getDefinition("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test(expected = ModuleDeserialization.NameIsAlreadyDefined.class)
  public void alreadyDefinedNameTestError() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    ClassDefinition aClass = new ClassDefinition(def.getNamespace().getChild(new Utils.Name("A")));
    def.getNamespace().addDefinition(aClass);
    FunctionDefinition functionDefinition = new FunctionDefinition(aClass.getLocalNamespace().getChild(new Utils.Name("f")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), null, null);
    aClass.getLocalNamespace().addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    RootModule.initialize();
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    RootModule.ROOT.addDefinition(newDef);
    ClassDefinition bClass = new ClassDefinition(newDef.getNamespace().getChild(new Utils.Name("A")));
    newDef.getNamespace().addDefinition(bClass);
    bClass.getLocalNamespace().addDefinition(new FunctionDefinition(aClass.getNamespace().getChild(new Utils.Name("g")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), null, null));
    moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef.getNamespace(), newDef);
  }

  @Test
  public void serializeDataTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    DataDefinition dataDefinition = new DataDefinition(def.getNamespace().getChild(new Utils.Name("D")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(Tele(vars("A"), Universe(0))));
    dataDefinition.addConstructor(new Constructor(dataDefinition.getNamespace().getChild(new Utils.Name("con1")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(TypeArg(Index(0))), dataDefinition));
    dataDefinition.addConstructor(new Constructor(dataDefinition.getNamespace().getChild(new Utils.Name("con2")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(TypeArg(Nat()), TypeArg(Index(1))), dataDefinition));
    def.getNamespace().addDefinition(dataDefinition);
    def.getNamespace().addDefinition(dataDefinition.getConstructors().get(0));
    def.getNamespace().addDefinition(dataDefinition.getConstructors().get(1));
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    RootModule.initialize();
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    RootModule.ROOT.addDefinition(newDef);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef.getNamespace(), newDef);
    assertEquals(0, errors);
    assertEquals(def.getFields().size(), newDef.getFields().size());
    assertEquals(def.getNamespace().getDefinitions().size(), newDef.getNamespace().getDefinitions().size());
    assertEquals(CompareVisitor.CMP.EQUALS, compare(dataDefinition.getType(), newDef.getNamespace().getDefinition("D").getType(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void serializeFunctionTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    FunctionDefinition funcDef = new FunctionDefinition(def.getNamespace().getChild(new Utils.Name("f")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, null);
    FunctionDefinition innerFunc = new FunctionDefinition(funcDef.getNamespace().getChild(new Utils.Name("g")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, Zero());
    funcDef.getNamespace().addDefinition(innerFunc);
    funcDef.setTerm(DefCall(innerFunc));
    def.getNamespace().addDefinition(funcDef);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition(RootModule.ROOT.getChild(new Utils.Name("test")));
    RootModule.ROOT.addDefinition(newDef);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef.getNamespace(), newDef);
    assertEquals(0, errors);
    assertEquals(1, newDef.getNamespace().getChildren().size());
    assertEquals(1, newDef.getNamespace().getDefinitions().size());
    assertEquals(1, newDef.getNamespace().getDefinition("f").getNamespace().getDefinitions().size());
    assertEquals(0, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void serializeNestedTest() throws IOException {
    ClassDefinition def = (ClassDefinition) parseDef("\\class A { \\class B { \\class C { } } }");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    ClassDefinition newDef = (ClassDefinition) parseDef("\\class B {}");
    RootModule.ROOT.addDefinition(newDef);
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef.getNamespace(), newDef);
    assertEquals(0, errors);
  }
}