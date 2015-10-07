package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.ReportingModuleLoader;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static org.junit.Assert.*;

public class ModuleSerializationTest {
  ListErrorReporter errorReporter;
  ReportingModuleLoader moduleLoader;

  @Before
  public void initialize() {
    RootModule.initialize();
    errorReporter = new ListErrorReporter();
    moduleLoader = new ReportingModuleLoader(errorReporter, false);
  }

  @Test
  public void serializeExprTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Name("test")));
    Expression term = Lam(lamArgs(Tele(false, vars("x", "y"), Nat()), Tele(vars("z"), Pi(Nat(), Nat()))), Pi(args(Tele(vars("A"), Universe()), TypeArg(false, Index(0))), Index(1)));
    FunctionDefinition functionDefinition = new FunctionDefinition(def.getLocalNamespace().getChild(new Name("f")), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, term);
    def.getLocalNamespace().addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), RootModule.ROOT.getChild(new Name("test")));
    assertNotNull(result);
    assertNotNull(result.definition);
    assertTrue(result.definition.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) def.getLocalNamespace().getDefinition("f")).getTerm(), ((FunctionDefinition) ((ClassDefinition) result.definition.definition).getLocalNamespace().getDefinition("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeElimTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Name("test")));
    List<Clause> clauses1 = new ArrayList<>(2);
    ElimExpression term1 = Elim(Index(0), clauses1);
    List<Clause> clauses2 = new ArrayList<>(2);
    ElimExpression term2 = Elim(Index(1), clauses2);
    clauses1.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Zero(), term1));
    clauses1.add(new Clause(match(Prelude.SUC, match("x")), Abstract.Definition.Arrow.LEFT, term2, term1));
    clauses2.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Index(0), term2));
    clauses2.add(new Clause(match(Prelude.SUC, match("x")), Abstract.Definition.Arrow.LEFT, Suc(Index(0)), term2));
    FunctionDefinition functionDefinition = new FunctionDefinition(def.getLocalNamespace().getChild(new Name("f")), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, term1);
    def.getLocalNamespace().addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), RootModule.ROOT.getChild(new Name("test")));
    assertNotNull(result);
    assertNotNull(result.definition);
    assertTrue(result.definition.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) def.getLocalNamespace().getDefinition("f")).getTerm(), ((FunctionDefinition) ((ClassDefinition) result.definition.definition).getLocalNamespace().getDefinition("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test(expected = ModuleDeserialization.NameIsAlreadyDefined.class)
  public void alreadyDefinedNameTestError() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Name("test")));
    ClassDefinition aClass = new ClassDefinition(def.getNamespace().getChild(new Name("A")));
    def.getNamespace().addDefinition(aClass);
    FunctionDefinition functionDefinition = new FunctionDefinition(aClass.getLocalNamespace().getChild(new Name("f")), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), null, null);
    aClass.getLocalNamespace().addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    RootModule.initialize();
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition(RootModule.ROOT.getChild(new Name("test")));
    RootModule.ROOT.addDefinition(newDef);
    ClassDefinition bClass = new ClassDefinition(newDef.getNamespace().getChild(new Name("A")));
    newDef.getNamespace().addDefinition(bClass);
    bClass.getLocalNamespace().addDefinition(new FunctionDefinition(aClass.getNamespace().getChild(new Name("g")), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), null, null));
    moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef.getNamespace());
  }

  @Test
  public void serializeDataTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Name("test")));
    DataDefinition dataDefinition = new DataDefinition(def.getNamespace().getChild(new Name("D")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(Tele(vars("A"), Universe(0))));
    dataDefinition.addConstructor(new Constructor(dataDefinition.getNamespace().getChild(new Name("con1")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(TypeArg(Index(0))), dataDefinition));
    dataDefinition.addConstructor(new Constructor(dataDefinition.getNamespace().getChild(new Name("con2")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(TypeArg(Nat()), TypeArg(Index(1))), dataDefinition));
    def.getNamespace().addDefinition(dataDefinition);
    def.getNamespace().addDefinition(dataDefinition.getConstructors().get(0));
    def.getNamespace().addDefinition(dataDefinition.getConstructors().get(1));
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    RootModule.initialize();
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), RootModule.ROOT.getChild(new Name("test")));
    assertNotNull(result);
    assertNotNull(result.definition);
    assertTrue(result.definition.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(def.getStatements().size(), ((ClassDefinition) result.definition.definition).getStatements().size());
    assertEquals(def.getNamespace().getMembers().size(), result.definition.namespace.getMembers().size());
    assertEquals(CompareVisitor.CMP.EQUALS, compare(dataDefinition.getType(), result.definition.namespace.getDefinition("D").getType(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeFunctionTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT.getChild(new Name("test")));
    FunctionDefinition funcDef = new FunctionDefinition(def.getNamespace().getChild(new Name("f")), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, null);
    FunctionDefinition innerFunc = new FunctionDefinition(funcDef.getNamespace().getChild(new Name("g")), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, Zero());
    funcDef.getNamespace().addDefinition(innerFunc);
    funcDef.setTerm(DefCall(innerFunc));
    def.getNamespace().addDefinition(funcDef);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), RootModule.ROOT.getChild(new Name("test")));
    assertNotNull(result);
    assertNotNull(result.definition);
    assertTrue(result.definition.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(1, result.definition.namespace.getMembers().size());
    assertEquals(1, result.definition.namespace.getMembers().size());
    assertEquals(1, result.definition.namespace.getDefinition("f").getNamespace().getMembers().size());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeNestedTest() throws IOException {
    ClassDefinition def = (ClassDefinition) typeCheckDef("\\class A { \\class B { \\class C { } } }");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getNamespace(), def, dataStream);

    ClassDefinition newDef = (ClassDefinition) typeCheckDef("\\class B {}");
    RootModule.ROOT.addDefinition(newDef);
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef.getNamespace());
    assertNotNull(result);
    assertNotNull(result.definition);
    assertTrue(result.definition.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
  }
}