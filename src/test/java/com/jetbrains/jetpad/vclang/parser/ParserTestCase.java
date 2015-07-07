package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTestCase {
  public static VcgrammarParser parse(final ModuleLoader moduleLoader, String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        moduleLoader.getErrors().add(new ParserError(new Module(moduleLoader.rootModule(), "test"), new Concrete.Position(line, pos), msg));
      }
    });
    return parser;
  }

  public static Concrete.Expression parseExpr(ModuleLoader moduleLoader, String text) {
    Concrete.Expression result = new BuildVisitor(moduleLoader.rootModule(), moduleLoader).visitExpr(parse(moduleLoader, text).expr());
    assertEquals(0, moduleLoader.getErrors().size());
    return result;
  }

  public static Concrete.Definition parseDef(ModuleLoader moduleLoader, String text) {
    Abstract.Definition result = new BuildVisitor(new ClassDefinition("test", moduleLoader.rootModule()), moduleLoader).visitDef(parse(moduleLoader, text).def());
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertTrue(result instanceof Concrete.Definition);
    return (Concrete.Definition) result;
  }

  public static ClassDefinition parseDefs(ModuleLoader moduleLoader, String text) {
    return parseDefs(moduleLoader, text, 0);
  }

  public static ClassDefinition parseDefs(ModuleLoader moduleLoader, String text, int errors) {
    return parseDefs(moduleLoader, text, 0, errors);
  }

  public static ClassDefinition parseDefs(ModuleLoader moduleLoader, String text, int moduleErrors, int errors) {
    ClassDefinition result = new ClassDefinition("test", moduleLoader.rootModule());
    new BuildVisitor(result, moduleLoader).visitDefs(parse(moduleLoader, text).defs());
    assertEquals(moduleErrors, moduleLoader.getErrors().size());
    assertEquals(errors, moduleLoader.getTypeCheckingErrors().size());
    return result;
  }

  public static boolean compare(Expression expr1, Abstract.Expression expr2) {
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result = Expression.compare(expr2, expr1, equations);
    return result.isOK() != CompareVisitor.CMP.NOT_EQUIV && equations.size() == 0;
  }
}
