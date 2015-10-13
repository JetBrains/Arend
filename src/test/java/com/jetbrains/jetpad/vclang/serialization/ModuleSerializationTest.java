package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
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
    ClassDefinition def = new ClassDefinition(RootModule.ROOT, new Name("test"));
    Expression term = Lam(lamArgs(Tele(false, vars("x", "y"), Nat()), Tele(vars("z"), Pi(Nat(), Nat()))), Pi(args(Tele(vars("A"), Universe()), TypeArg(false, Index(0))), Index(1)));
    Namespace namespace = def.getParentNamespace().getChild(def.getName());
    FunctionDefinition functionDefinition = new FunctionDefinition(namespace, new Name("f"), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, term);
    namespace.addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(new ResolvedName(def.getParentNamespace(), def.getName()), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, "test"));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) namespace.getDefinition("f")).getTerm(), ((FunctionDefinition) result.namespaceMember.namespace.getDefinition("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeElimTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT, new Name("test"));
    List<Clause> clauses1 = new ArrayList<>(2);
    ElimExpression term1 = Elim(Index(0), clauses1);
    List<Clause> clauses2 = new ArrayList<>(2);
    ElimExpression term2 = Elim(Index(1), clauses2);
    clauses1.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Zero(), term1));
    clauses1.add(new Clause(match(Prelude.SUC, match("x")), Abstract.Definition.Arrow.LEFT, term2, term1));
    clauses2.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Index(0), term2));
    clauses2.add(new Clause(match(Prelude.SUC, match("x")), Abstract.Definition.Arrow.LEFT, Suc(Index(0)), term2));
    Namespace namespace = def.getParentNamespace().getChild(def.getName());
    FunctionDefinition functionDefinition = new FunctionDefinition(namespace, new Name("f"), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, term1);
    namespace.addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(new ResolvedName(def.getParentNamespace(), def.getName()), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, "test"));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) namespace.getDefinition("f")).getTerm(), ((FunctionDefinition) result.namespaceMember.namespace.getDefinition("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test(expected = ModuleDeserialization.NameIsAlreadyDefined.class)
  public void alreadyDefinedNameTestError() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT, new Name("test"));
    Namespace namespace = def.getParentNamespace().getChild(def.getName());
    ClassDefinition aClass = new ClassDefinition(namespace, new Name("A"));
    namespace.addDefinition(aClass);
    Namespace aNamespace = aClass.getParentNamespace().getChild(aClass.getName());
    FunctionDefinition functionDefinition = new FunctionDefinition(aNamespace, new Name("f"), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), null, null);
    aNamespace.addDefinition(functionDefinition);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(new ResolvedName(def.getParentNamespace(), def.getName()), def, dataStream);

    RootModule.initialize();
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition(RootModule.ROOT, new Name("test"));
    Namespace newNamespace = newDef.getParentNamespace().findChild(newDef.getName().name);
    RootModule.ROOT.addDefinition(newDef);
    ClassDefinition bClass = new ClassDefinition(newNamespace, new Name("A"));
    newNamespace.addDefinition(bClass);
    Namespace bNamespace = bClass.getParentNamespace().getChild(bClass.getName());
    bNamespace.addDefinition(new FunctionDefinition(aClass.getParentNamespace().findChild(aClass.getName().name), new Name("g"), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), null, null));
    moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(newDef.getParentNamespace(), newDef.getName()));
  }

  @Test
  public void serializeDataTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT, new Name("test"));
    Namespace namespace = def.getParentNamespace().findChild(def.getName().name);
    DataDefinition dataDefinition = new DataDefinition(namespace, new Name("D"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(Tele(vars("A"), Universe(0))));
    Namespace dataNamespace = namespace.getChild(dataDefinition.getName());
    dataDefinition.addConstructor(new Constructor(dataNamespace, new Name("con1"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(TypeArg(Index(0))), dataDefinition));
    dataDefinition.addConstructor(new Constructor(dataNamespace, new Name("con2"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(TypeArg(Nat()), TypeArg(Index(1))), dataDefinition));
    namespace.addDefinition(dataDefinition);
    namespace.addDefinition(dataDefinition.getConstructors().get(0));
    namespace.addDefinition(dataDefinition.getConstructors().get(1));
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(new ResolvedName(def.getParentNamespace(), def.getName()), def, dataStream);

    RootModule.initialize();
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, "test"));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(def.getStatements().size(), ((ClassDefinition) result.namespaceMember.definition).getStatements().size());
    assertEquals(namespace.getMembers().size(), result.namespaceMember.namespace.getMembers().size());
    assertEquals(CompareVisitor.CMP.EQUALS, compare(dataDefinition.getType(), result.namespaceMember.namespace.getDefinition("D").getType(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeFunctionTest() throws IOException {
    ClassDefinition def = new ClassDefinition(RootModule.ROOT, new Name("test"));
    Namespace namespace = def.getParentNamespace().findChild(def.getName().name);
    FunctionDefinition funcDef = new FunctionDefinition(namespace, new Name("f"), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, null);
    Namespace funcNamespace = namespace.getChild(funcDef.getName());
    FunctionDefinition innerFunc = new FunctionDefinition(funcNamespace, new Name("g"), null, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, Zero());
    funcNamespace.addDefinition(innerFunc);
    funcDef.setTerm(DefCall(innerFunc));
    namespace.addDefinition(funcDef);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(new ResolvedName(def.getParentNamespace(), def.getName()), def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, new Name("test")));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(1, result.namespaceMember.namespace.getMembers().size());
    assertEquals(1, result.namespaceMember.namespace.getMembers().size());
    Definition definition = result.namespaceMember.namespace.getDefinition("f");
    assertNotNull(definition);
    assertEquals(1, definition.getParentNamespace().findChild(definition.getName().name).getMembers().size());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeNestedTest() throws IOException {
    ClassDefinition def = (ClassDefinition) typeCheckDef("\\class A { \\class B { \\class C { } } }");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(new ResolvedName(def.getParentNamespace(), def.getName()), def, dataStream);

    ClassDefinition newDef = (ClassDefinition) typeCheckDef("\\class B {}");
    RootModule.ROOT.addDefinition(newDef);
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(newDef.getParentNamespace(), newDef.getName()));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
  }
}