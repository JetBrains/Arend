package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.DummyNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
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
        errorReporter.report(new ParserError(RootModule.ROOT.getChild(new Utils.Name("test")), new Concrete.Position(line, pos), msg));
      }
    });
    return parser;
  }

  public static Concrete.Expression parseExpr(NameResolver nameResolver, String text, int errors) {
    RootModule.initialize();
    Namespace namespace = RootModule.ROOT.getChild(new Utils.Name("test"));
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Expression result = new BuildVisitor(namespace, new Namespace(namespace.getName(), null), nameResolver, errorReporter).visitExpr(parse(errorReporter, text).expr());
    if (errors < 0) {
      assertTrue(!errorReporter.getErrorList().isEmpty());
    } else {
      assertEquals(errors, errorReporter.getErrorList().size());
    }
    return result;
  }

  public static Concrete.Expression parseExpr(String text, int errors) {
    return parseExpr(DummyNameResolver.getInstance(), text, errors);
  }

  public static Concrete.Expression parseExpr(String text) {
    return parseExpr(text, 0);
  }

  public static Definition parseDef(String text) {
    return parseDef(text, 0);
  }

  public static Definition parseDef(String text, int errors) {
    RootModule.initialize();
    Namespace namespace = RootModule.ROOT.getChild(new Utils.Name("test"));
    Namespace localNamespace = new Namespace(namespace.getName(), null);
    ListErrorReporter errorReporter = new ListErrorReporter();
    Definition result = new BuildVisitor(namespace, localNamespace, DummyNameResolver.getInstance(), errorReporter).visitDef(parse(errorReporter, text).def());
    assertEquals(errors, errorReporter.getErrorList().size());
    return result;
  }

  public static ClassDefinition parseDefs(String text) {
    return parseDefs(text, 0);
  }

  public static ClassDefinition parseDefs(String text, int errors) {
    RootModule.initialize();
    Namespace namespace = RootModule.ROOT.getChild(new Utils.Name("test"));
    ClassDefinition result = new ClassDefinition(namespace);
    ListErrorReporter errorReporter = new ListErrorReporter();
    new BuildVisitor(namespace, result.getLocalNamespace(), DummyNameResolver.getInstance(), errorReporter).visitDefs(parse(errorReporter, text).defs());
    assertEquals(errors, errorReporter.getErrorList().size());
    return result;
  }

  public static boolean compare(Expression expr1, Abstract.Expression expr2) {
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result = Expression.compare(expr2, expr1, equations);
    return result.isOK() != CompareVisitor.CMP.NOT_EQUIV && equations.size() == 0;
  }
}
