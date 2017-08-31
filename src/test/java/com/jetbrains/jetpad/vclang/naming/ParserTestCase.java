package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.frontend.parser.*;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.expr.ConcreteCompareVisitor;
import org.antlr.v4.runtime.*;

import static org.junit.Assert.assertThat;

public abstract class ParserTestCase extends VclangTestCase {
  private static final SourceId SOURCE_ID = new SourceId() {
    @Override
    public ModulePath getModulePath() {
      return ModulePath.moduleName(toString());
    }
    @Override
    public String toString() {
      return "$TestCase$";
    }
  };

  private VcgrammarParser _parse(String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new Position(SOURCE_ID, line, pos), msg));
      }
    });

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new Position(SOURCE_ID, line, pos), msg));
      }
    });
    // parser.addErrorListener(new DiagnosticErrorListener());
    // parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
    return parser;
  }


  Concrete.Expression<Position> parseExpr(String text, int errors) {
    VcgrammarParser.ExprContext ctx = _parse(text).expr();
    Concrete.Expression<Position> expr = errorList.isEmpty() ? new BuildVisitor(SOURCE_ID, errorReporter).visitExpr(ctx) : null;
    assertThat(errorList, containsErrors(errors));
    return expr;
  }

  protected Concrete.Expression<Position> parseExpr(String text) {
    return parseExpr(text, 0);
  }

  GlobalReference parseDef(String text, int errors) {
    VcgrammarParser.DefinitionContext ctx = _parse(text).definition();
    Group definition = errorList.isEmpty() ? new BuildVisitor(SOURCE_ID, errorReporter).visitDefinition(ctx) : null;
    assertThat(errorList, containsErrors(errors));
    return definition == null ? null : (GlobalReference) definition.getReferable();
  }

  protected GlobalReference parseDef(String text) {
    return parseDef(text, 0);
  }

  Group parseModule(String text, int errors) {
    VcgrammarParser.StatementsContext tree = _parse(text).statements();
    Group group = errorList.isEmpty() ? new BuildVisitor(SOURCE_ID, errorReporter).visitStatements(tree) : null;
    assertThat(errorList, containsErrors(errors));
    return group;
  }

  protected Group parseModule(String text) {
    return parseModule(text, 0);
  }


  protected static boolean compareAbstract(Concrete.Expression<Position> expr1, Concrete.Expression<Position> expr2) {
    return expr1.accept(new ConcreteCompareVisitor(), expr2);
  }
}
