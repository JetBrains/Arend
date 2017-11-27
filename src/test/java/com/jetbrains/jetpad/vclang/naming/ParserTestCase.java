package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.frontend.parser.*;
import com.jetbrains.jetpad.vclang.frontend.term.group.FileGroup;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.expr.ConcreteCompareVisitor;
import org.antlr.v4.runtime.*;

import static org.junit.Assert.assertThat;

public abstract class ParserTestCase extends VclangTestCase {
  protected final SimpleModuleScopeProvider moduleScopeProvider = new SimpleModuleScopeProvider();

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


  Concrete.Expression parseExpr(String text, int errors) {
    VcgrammarParser.ExprContext ctx = _parse(text).expr();
    Concrete.Expression expr = errorList.isEmpty() ? new BuildVisitor(SOURCE_ID, errorReporter).visitExpr(ctx) : null;
    assertThat(errorList, containsErrors(errors));
    return expr;
  }

  protected Concrete.Expression parseExpr(String text) {
    return parseExpr(text, 0);
  }

  ChildGroup parseDef(String text, int errors) {
    VcgrammarParser.DefinitionContext ctx = _parse(text).definition();
    ChildGroup definition = errorList.isEmpty() ? new BuildVisitor(SOURCE_ID, errorReporter).visitDefinition(ctx, null) : null;
    assertThat(errorList, containsErrors(errors));
    return definition;
  }

  protected ChildGroup parseDef(String text) {
    return parseDef(text, 0);
  }

  protected ChildGroup parseModule(String text, int errors) {
    VcgrammarParser.StatementsContext tree = _parse(text).statements();
    FileGroup group = errorList.isEmpty() ? new BuildVisitor(SOURCE_ID, errorReporter).visitStatements(tree) : null;
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
