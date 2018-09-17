<h1 id="lexical-structure">Lexical Structure<a class="headerlink" href="#lexical-structure" title="Permanent link">&para;</a></h1>

## Keywords

All Arend's _keywords_ begin with `\`.
The complete list of keywords:

[\open](../definitions/modules/#open-commands) [\import](../definitions/modules/#import-commands) [\hiding](../definitions/modules/#open-commands) [\as](../definitions/modules/#open-commands) [\using](../definitions/modules/#open-commands)
[\truncated](../definitions/data) [\data](../definitions/data) [\func](../definitions/functions) [\class](../definitions/classes) [\record](../definitions/records) [\extends](../definitions/records) [\module](../definitions/modules/#modules) [\instance](../definitions/instances) [\coerce](../definitions/coercion)
[\with](../definitions/functions/#pattern-matching) [\elim](../definitions/functions/#elim) [\cowith](../definitions/functions/#copattern-matching) [\where](../definitions/modules/#where-blocks)
[\infix](../definitions/#infix-operators) [\infixl](../definitions/#infix-operators) [\infixr](../definitions/#infix-operators) [\fix](../definitions/#precedence) [\fixl](../definitions/#precedence) [\fixr](../definitions/#precedence)
[\new](../expressions/class-ext) [\Pi](../expressions/pi) [\Sigma](../expressions/sigma) [\lam](../expressions/pi) [\let](../expressions/let) [\in]((../expressions/let)) [\case](../expressions/case) [\return](../expressions/case)
[\lp](../expressions/universes) [\lh](../expressions/universes) [\suc](../expressions/universes) [\max](../expressions/universes) [\levels](../expressions/universes) [\Prop](../expressions/universes) [\Set](../expressions/universes) [\Type](../expressions/universes).

## Numerals

A _positive numeral_ is a non-empty sequence of numbers.
A _negative numeral_ consists of `-` followed by a non-empty sequence of numbers.

## Identifiers

An _identifier_ consists of a non-empty sequence of lower and upper case letters, numbers, and symbols from the list `~!@#$%^&*-+=<>?/|[];:_`.
Exceptions are sequences that begin with a number, sequences that begin with `--`, numerals, and reserved names: `->`, `=>`, `_`, `:`, and `|`.

Examples:

* Valid identifiers: `xxx`, `+`, `$^~]!005x`, `::`, `->x`, `x:Nat`, `-5b`, `-33+7`.
* Invalid identifiers: `--xxx`, `5b`, `-33`, `->`.

## Infix and postfix notation

A _postfix notation_ is an identifier followed by `` ` ``.
An _infix notation_ is an identifier surrounded by `` ` ``.
Both of these notations are described in [this section](../definitions).

## Comments

_Multi-line comments_ are enclosed in `{-` and `-}` and can be nested.
_Single-line comments_ consist of `--` followed by an arbitrary text until the end of the line.
The exception is identifiers which include `--` in their names, but do not begin with `--`.
This means that `--`, `--------`, `--|`, `--foo`, and `-------foobar` are comments and `|--`, `%--foo`, and `x------foobar` are not.
