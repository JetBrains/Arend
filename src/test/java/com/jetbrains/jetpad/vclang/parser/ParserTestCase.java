package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractCompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.antlr.v4.runtime.*;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTestCase {
  public static VcgrammarParser parse(final ErrorReporter errorReporter, String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new ResolvedName(RootModule.ROOT, "test"), new Concrete.Position(line, pos), msg));
      }
    });
    return parser;
  }

  public static Concrete.Expression parseExpr(String text, int errors) {
    RootModule.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Expression result = new BuildVisitor(errorReporter).visitExpr(parse(errorReporter, text).expr());
    if (errors >= 0) {
      assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    } else {
      assertTrue(errorReporter.getErrorList().toString(), errorReporter.getErrorList().size() > 0);
    }
    return result;
  }

  public static Concrete.Expression parseExpr(String text) {
    return parseExpr(text, 0);
  }

  public static Concrete.Definition parseDef(String text) {
    return parseDef(text, 0);
  }

  public static Concrete.Definition parseDef(String text, int errors) {
    RootModule.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Definition definition = new BuildVisitor(errorReporter).visitDefinition(parse(errorReporter, text).definition());
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return definition;
  }

  public static Concrete.ClassDefinition parseClass(String name, String text) {
    return parseClass(name, text, 0);
  }

  public static Concrete.ClassDefinition parseClass(String name, String text, int errors) {
    RootModule.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    VcgrammarParser.StatementsContext tree = parse(errorReporter, text).statements();
    assertTrue(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());
    List<Concrete.Statement> statements = new BuildVisitor(errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(null, name, statements);
    for (Concrete.Statement statement : statements) {
      if (statement instanceof Concrete.DefineStatement) {
        ((Concrete.DefineStatement) statement).setParentDefinition(classDefinition);
      }
    }
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return classDefinition;
  }

  public static boolean compare(Abstract.Expression expr1, Abstract.Expression expr2) {
    return expr1.accept(new AbstractCompareVisitor(), expr2);
  }
}
