package org.arend.util;

import java.util.ArrayList;
import java.util.List;

public class Range<T extends Comparable<T>> extends Pair<T,T> {
  public Range(T lowerBound, T upperBound) {
    super(lowerBound, upperBound);
  }

  public static <T extends Comparable<T>> Range<T> unbound() {
    return new Range<>(null, null);
  }

  public T getLowerBound() {
    return proj1;
  }

  public T getUpperBound() {
    return proj2;
  }

  public boolean isBound() {
    return proj1 != null || proj2 != null;
  }

  public boolean inRange(T t) {
    return (proj1 == null || proj1.compareTo(t) <= 0) && (proj2 == null || proj2.compareTo(t) >= 0);
  }

  public String checkRange(T t) {
    String s1 = proj1 != null && proj1.compareTo(t) > 0 ? "<= " + proj1 : null;
    String s2 = proj2 != null && proj2.compareTo(t) < 0 ? ">= " + proj2 : null;
    return s1 == null ? s2 : s2 == null ? s1 : s1 + ", " + s2;
  }

  private enum TokenType { LEQ, GEQ, COMMA, TEXT }

  public static Range<Version> parseVersionRange(String text) {
    // Lexer

    List<Pair<TokenType,String>> tokens = new ArrayList<>();

    int start = 0;
    for (int i = 0; i < text.length(); i++) {
      Pair<TokenType,String> newToken = null;
      int stop = i;

      if (Character.isWhitespace(text.charAt(i))) {
        // Do nothing
      } else if (text.charAt(i) == ',') {
        newToken = new Pair<>(TokenType.COMMA, ",");
      } else if (text.charAt(i) == '<' && i + 1 < text.length() && text.charAt(i + 1) == '=') {
        newToken = new Pair<>(TokenType.LEQ, "<=");
        i++;
      } else if (text.charAt(i) == '>' && i + 1 < text.length() && text.charAt(i + 1) == '=') {
        newToken = new Pair<>(TokenType.GEQ, ">=");
        i++;
      } else {
        continue;
      }

      if (stop > start) {
        tokens.add(new Pair<>(TokenType.TEXT, text.substring(start, stop)));
      }
      start = i + 1;

      if (newToken != null) {
        tokens.add(newToken);
      }
    }
    if (start < text.length()) {
      tokens.add(new Pair<>(TokenType.TEXT, text.substring(start)));
    }

    // Parser

    if (tokens.isEmpty()) {
      return unbound();
    }

    if (tokens.size() == 1) {
      return tokens.get(0).proj1 != TokenType.TEXT ? null
          : new Range<>(new Version(tokens.get(0).proj2), new Version(tokens.get(0).proj2));
    }

    String lowerBound = null;
    String upperBound = null;

    if (tokens.get(0).proj1 == TokenType.LEQ && tokens.get(1).proj1 == TokenType.TEXT) {
      upperBound = tokens.get(1).proj2;
    } else
    if (tokens.get(0).proj1 == TokenType.GEQ && tokens.get(1).proj1 == TokenType.TEXT) {
      lowerBound = tokens.get(1).proj2;
    }
    if (tokens.size() == 2) {
      return new Range<>(new Version(lowerBound), new Version(upperBound));
    }

    if (tokens.size() != 5 || tokens.get(2).proj1 != TokenType.COMMA) {
      return null;
    }
    if (tokens.get(3).proj1 == TokenType.LEQ && tokens.get(4).proj1 == TokenType.TEXT) {
      upperBound = tokens.get(4).proj2;
    } else
    if (tokens.get(3).proj1 == TokenType.GEQ && tokens.get(4).proj1 == TokenType.TEXT) {
      lowerBound = tokens.get(4).proj2;
    }

    return lowerBound == null || upperBound == null ? null
        : new Range<>(new Version(lowerBound), new Version(upperBound));
  }

  @Override
  public String toString() {
    return proj1 == null && proj2 == null ? "" : proj1 == null ? "<= " + proj2 : proj2 == null ? ">= " + proj1 : proj1.equals(proj2) ? proj1.toString() : ">= " + proj1 + ", <= " + proj2;
  }
}
