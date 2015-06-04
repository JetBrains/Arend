package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.error.ParserError;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParserTestCase {
  public static VcgrammarParser parse(String text, final List<ParserError> errors) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errors.add(new ParserError(new Concrete.Position(line, pos), msg));
      }
    });
    return parser;
  }

  public static Concrete.Expression parseExpr(String text) {
    BuildVisitor builder = new BuildVisitor();
    Concrete.Expression result = builder.visitExpr(parse(text, builder.getErrors()).expr());
    assertEquals(0, builder.getErrors().size());
    return result;
  }

  public static Concrete.Definition parseDef(String text) {
    BuildVisitor builder = new BuildVisitor();
    Concrete.Definition result = builder.visitDef(parse(text, builder.getErrors()).def());
    assertEquals(0, builder.getErrors().size());
    return result;
  }

  public static List<Concrete.Definition> parseDefs(String text) {
    BuildVisitor builder = new BuildVisitor();
    List<Concrete.Definition> result = builder.visitDefs(parse(text, builder.getErrors()).defs());
    assertEquals(0, builder.getErrors().size());
    return result;
  }

  public static boolean compare(Expression expr1, Abstract.Expression expr2) {
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result = Expression.compare(expr2, expr1, equations);
    return result.isOK() != CompareVisitor.CMP.NOT_EQUIV && equations.size() == 0;
  }
}
