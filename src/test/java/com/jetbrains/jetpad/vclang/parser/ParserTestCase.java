package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ParserTestCase {
  public static VcgrammarParser parse(String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    return new VcgrammarParser(tokens);
  }

  public static Concrete.Expression parseExpr(String text, Map<String, Definition> definitions) {
    BuildVisitor builder = new BuildVisitor(definitions);
    Concrete.Expression result = builder.visitExpr(parse(text).expr());
    assertEquals(0, builder.getErrors().size());
    return result;
  }

  public static Concrete.Expression parseExpr(String text) {
    return parseExpr(text, Prelude.DEFINITIONS);
  }

  public static Concrete.Definition parseDef(String text) {
    BuildVisitor builder = new BuildVisitor(Prelude.DEFINITIONS);
    Concrete.Definition result = builder.visitDef(parse(text).def());
    assertEquals(0, builder.getErrors().size());
    return result;
  }

  public static List<Concrete.Definition> parseDefs(String text) {
    BuildVisitor builder = new BuildVisitor(Prelude.DEFINITIONS);
    List<Concrete.Definition> result = builder.visitDefs(parse(text).defs());
    assertEquals(0, builder.getErrors().size());
    return result;
  }

  public static boolean compare(Expression expr1, Abstract.Expression expr2) {
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    boolean result = expr2.accept(new CompareVisitor(CompareVisitor.CMP.EQ, equations), expr1);
    return result && equations.size() == 0;
  }
}
