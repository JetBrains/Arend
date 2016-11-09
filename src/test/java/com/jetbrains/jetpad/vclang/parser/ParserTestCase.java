package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;
import com.jetbrains.jetpad.vclang.module.source.NameModuleSourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractCompareVisitor;
import org.antlr.v4.runtime.*;

import java.util.List;

import static org.junit.Assert.assertThat;

public abstract class ParserTestCase extends VclangTestCase {
  private static final ModuleSourceId SOURCE_ID = new ModuleSourceId() {
    @Override
    public ModulePath getModulePath() {
      return ModulePath.moduleName(toString());
    }
    @Override
    public String toString() {
      return "$TestCase$";
    }
  };

  private static VcgrammarParser _parse(final String name, final ErrorReporter errorReporter, String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new NameModuleSourceId(name), new Concrete.Position(SOURCE_ID, line, pos), msg));
      }
    });

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new NameModuleSourceId(name), new Concrete.Position(SOURCE_ID, line, pos), msg));
      }
    });
    return parser;
  }


  Concrete.Expression parseExpr(String text, int errors) {
    VcgrammarParser.ExprContext exprContext = _parse("$testModule$", errorReporter, text).expr();
    Concrete.Expression result = new BuildVisitor(SOURCE_ID, errorReporter).visitExpr(exprContext);
    assertThat(errorList, containsErrors(errors));
    return result;
  }

  protected Concrete.Expression parseExpr(String text) {
    return parseExpr(text, 0);
  }

  Concrete.Definition parseDef(String text, int errors) {
    Concrete.Definition definition = new BuildVisitor(SOURCE_ID, errorReporter).visitDefinition(_parse("$testModule$", errorReporter, text).definition());
    assertThat(errorList, containsErrors(errors));
    return definition;
  }

  protected Concrete.Definition parseDef(String text) {
    return parseDef(text, 0);
  }

  Concrete.ClassDefinition parseClass(String name, String text, int errors) {
    VcgrammarParser.StatementsContext tree = _parse(name, errorReporter, text).statements();
    List<Concrete.Statement> statements = new BuildVisitor(SOURCE_ID, errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(ConcreteExpressionFactory.POSITION, name, statements);
    assertThat(errorList, containsErrors(errors));
    // classDefinition.accept(new DefinitionResolveStaticModVisitor(new ConcreteStaticModListener()), null);
    return classDefinition;
  }

  protected Concrete.ClassDefinition parseClass(String name, String text) {
    return parseClass(name, text, 0);
  }


  protected static boolean compareAbstract(Abstract.Expression expr1, Abstract.Expression expr2) {
    return expr1.accept(new AbstractCompareVisitor(), expr2);
  }
}
