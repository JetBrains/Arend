package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParserTestCase {
  public static VcgrammarParser parse(String text, final List<VcError> errors) {
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

  public static Concrete.Expression parseExpr(List<Definition> definitions, String text) {
    List<VcError> errors = new ArrayList<>();
    ClassDefinition root = new ClassDefinition("\\root", null, new Universe.Type(0), definitions);
    Concrete.Expression result = new BuildVisitor(root, new ArrayList<ModuleLoader.TypeCheckingUnit>(), errors).visitExpr(parse(text, errors).expr());
    assertEquals(0, errors.size());
    return result;
  }

  public static Concrete.Expression parseExpr(String text) {
    return parseExpr(new ArrayList<Definition>(0), text);
  }

  public static Concrete.Definition parseDef(String text) {
    List<VcError> errors = new ArrayList<>();
    ClassDefinition root = new ClassDefinition("\\root", null, new Universe.Type(0), new ArrayList<Definition>(0));
    Concrete.Definition result = new BuildVisitor(root, new ArrayList<ModuleLoader.TypeCheckingUnit>(), errors).visitDef(parse(text, errors).def()).rawDefinition;
    assertEquals(0, errors.size());
    return result;
  }

  public static List<Concrete.Definition> parseDefs(String text) {
    List<VcError> errors = new ArrayList<>();
    ClassDefinition root = new ClassDefinition("\\root", null, new Universe.Type(0), new ArrayList<Definition>(0));
    List<Concrete.Definition> result = new BuildVisitor(root, new ArrayList<ModuleLoader.TypeCheckingUnit>(), errors).visitDefs(parse(text, errors).defs());
    assertEquals(0, errors.size());
    return result;
  }

  public static boolean compare(Expression expr1, Abstract.Expression expr2) {
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result = Expression.compare(expr2, expr1, equations);
    return result.isOK() != CompareVisitor.CMP.NOT_EQUIV && equations.size() == 0;
  }
}
