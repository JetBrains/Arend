package org.arend.naming;

import org.arend.ArendTestCase;
import org.arend.ext.module.ModulePath;
import org.arend.frontend.parser.*;
import org.arend.frontend.repl.CommonCliRepl;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.FullModuleReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteCompareVisitor;
import org.arend.term.group.*;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public abstract class ParserTestCase extends ArendTestCase {
  protected static final ModuleLocation MODULE_PATH = new ModuleLocation(null, false, null, new ModulePath("$TestCase$"));
  protected static final LocatedReferable MODULE_REF = new FullModuleReferable(MODULE_PATH);

  private ArendParser _parse(String text) {
    return CommonCliRepl.createParser(text, MODULE_PATH, errorReporter);
  }


  Concrete.Expression parseExpr(String text, int errors) {
    ArendParser.ExprContext ctx = _parse(text).expr();
    Concrete.Expression expr = errorList.isEmpty() ? new BuildVisitor(MODULE_PATH, errorReporter).visitExpr(ctx) : null;
    assertThat(errorList, containsErrors(errors));
    return expr;
  }

  protected Concrete.Expression parseExpr(String text) {
    return parseExpr(text, 0);
  }

  ChildGroup parseDef(String text, int errors) {
    ArendParser.DefinitionContext ctx = _parse(text).definition();
    List<Statement> statements = new ArrayList<>(1);
    FileGroup fileGroup = new FileGroup(new FullModuleReferable(MODULE_PATH), statements);
    StaticGroup definition = errorList.isEmpty() ? new BuildVisitor(MODULE_PATH, errorReporter).visitDefinition(AccessModifier.PUBLIC, ctx, fileGroup, null) : null;
    if (definition != null) {
      statements.add(definition);
    }
    assertThat(errorList, containsErrors(errors));
    return definition;
  }

  protected ChildGroup parseDef(String text) {
    return parseDef(text, 0);
  }

  protected ChildGroup parseModule(String text, int errors) {
    ArendParser.StatementsContext tree = _parse(text).statements();
    FileGroup group = errorList.isEmpty() ? new BuildVisitor(MODULE_PATH, errorReporter).visitStatements(tree) : null;
    if (group != null) {
      group.setModuleScopeProvider(moduleScopeProvider);
    }
    assertThat(errorList, containsErrors(errors));
    return group;
  }

  protected ChildGroup parseModule(String text) {
    return parseModule(text, 0);
  }


  protected static boolean compareAbstract(Concrete.Expression expr1, Concrete.Expression expr2) {
    return new ConcreteCompareVisitor().compare(expr1, expr2);
  }
}
