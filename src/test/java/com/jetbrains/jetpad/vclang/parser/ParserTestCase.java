package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.NameModuleID;
import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractCompareVisitor;
import org.antlr.v4.runtime.*;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTestCase {
  public static VcgrammarParser parse(final String name, final ErrorReporter errorReporter, String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new NameModuleID(name), new Concrete.Position(line, pos), msg));
      }
    });
    return parser;
  }

  public static Concrete.Expression parseExpr(String text, int errors) {
    Root.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Expression result = new BuildVisitor(errorReporter).visitExpr(parse("test", errorReporter, text).expr());
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
    Root.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Definition definition = new BuildVisitor(errorReporter).visitDefinition(parse("test", errorReporter, text).definition());
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return definition;
  }

  public static Concrete.ClassDefinition parseClass(String name, String text) {
    return parseClass(name, text, 0);
  }

  public static Concrete.ClassDefinition parseClass(String name, String text, int errors) {
    Root.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    VcgrammarParser.StatementsContext tree = parse(name, errorReporter, text).statements();
    assertTrue(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());
    List<Concrete.Statement> statements = new BuildVisitor(errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(null, name, statements, Abstract.ClassDefinition.Kind.Module);
    for (Concrete.Statement statement : statements) {
      if (statement instanceof Concrete.DefineStatement) {
        ((Concrete.DefineStatement) statement).setParentDefinition(classDefinition);
      }
    }
    classDefinition.setModuleID(new NameModuleID(name));
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    // classDefinition.accept(new DefinitionResolveStaticModVisitor(new ConcreteStaticModListener()), null);
    return classDefinition;
  }

  public static boolean compare(Abstract.Expression expr1, Abstract.Expression expr2) {
    return expr1.accept(new AbstractCompareVisitor(), expr2);
  }
}
