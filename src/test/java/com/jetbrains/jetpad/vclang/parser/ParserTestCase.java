package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.NameModuleID;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractCompareVisitor;
import org.antlr.v4.runtime.*;

import java.util.List;

import static com.jetbrains.jetpad.vclang.util.TestUtil.assertErrorListSize;

public class ParserTestCase {
  private static final ModuleID MODULE_ID = new ModuleID() {
    @Override
    public ModulePath getModulePath() {
      return ModulePath.moduleName(toString());
    }
    @Override
    public String toString() {
      return "$ParserTestCase$";
    }
  };

  public static VcgrammarParser parse(final String name, final ErrorReporter errorReporter, String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new NameModuleID(name), new Concrete.Position(MODULE_ID, line, pos), msg));
      }
    });
    return parser;
  }

  public static Concrete.Expression parseExpr(String text, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Expression result = new BuildVisitor(MODULE_ID, errorReporter).visitExpr(parse("test", errorReporter, text).expr());
    assertErrorListSize(errorReporter.getErrorList(), errors);
    return result;
  }

  public static Concrete.Expression parseExpr(String text) {
    return parseExpr(text, 0);
  }

  public static Concrete.Definition parseDef(String text) {
    return parseDef(text, 0);
  }

  public static Concrete.Definition parseDef(String text, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Definition definition = new BuildVisitor(MODULE_ID, errorReporter).visitDefinition(parse("test", errorReporter, text).definition());
    assertErrorListSize(errorReporter.getErrorList(), errors);
    return definition;
  }

  public static Concrete.ClassDefinition parseClass(String name, String text) {
    return parseClass(name, text, 0);
  }

  public static Concrete.ClassDefinition parseClass(String name, String text, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    VcgrammarParser.StatementsContext tree = parse(name, errorReporter, text).statements();
    assertErrorListSize(errorReporter.getErrorList(), 0);
    List<Concrete.Statement> statements = new BuildVisitor(MODULE_ID, errorReporter).visitStatements(tree);
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(ConcreteExpressionFactory.POSITION, name, statements, Abstract.ClassDefinition.Kind.Module);
    for (Concrete.Statement statement : statements) {
      if (statement instanceof Concrete.DefineStatement) {
        ((Concrete.DefineStatement) statement).setParentDefinition(classDefinition);
      }
    }
    classDefinition.setModuleID(new NameModuleID(name));
    assertErrorListSize(errorReporter.getErrorList(), errors);
    // classDefinition.accept(new DefinitionResolveStaticModVisitor(new ConcreteStaticModListener()), null);
    return classDefinition;
  }

  public static boolean compare(Abstract.Expression expr1, Abstract.Expression expr2) {
    return expr1.accept(new AbstractCompareVisitor(), expr2);
  }
}
