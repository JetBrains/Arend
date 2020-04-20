package org.arend.naming;

import org.antlr.v4.runtime.*;
import org.arend.ArendTestCase;
import org.arend.ext.module.ModulePath;
import org.arend.frontend.parser.*;
import org.arend.naming.reference.ModuleReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.expr.ConcreteCompareVisitor;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.FileGroup;
import org.arend.term.group.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertThat;

public abstract class ParserTestCase extends ArendTestCase {
  protected static final ModulePath MODULE_PATH = new ModulePath("$TestCase$");

  private ArendParser _parse(String text) {
    CharStream input = CharStreams.fromString(text);
    ArendLexer lexer = new ArendLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new Position(MODULE_PATH, line, pos), msg));
      }
    });

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ArendParser parser = new ArendParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new Position(MODULE_PATH, line, pos), msg));
      }
    });
    // parser.addErrorListener(new DiagnosticErrorListener());
    // parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
    return parser;
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
    List<Group> subgroups = new ArrayList<>(1);
    FileGroup fileGroup = new FileGroup(new ModuleReferable(MODULE_PATH), subgroups, Collections.emptyList());
    ChildGroup definition = errorList.isEmpty() ? new BuildVisitor(MODULE_PATH, errorReporter).visitDefinition(ctx, fileGroup, null) : null;
    if (definition != null) {
      subgroups.add(definition);
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
