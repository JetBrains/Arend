<h1 id="lexical-structure">Lexical Structure<a class="headerlink" href="#lexical-structure" title="Permanent link">&para;</a></h1>

## Keywords

All Arend's _keywords_ begin with `\`.
Here's the complete list of keywords:

[\open](/language-reference/definitions/modules/#open-commands) [\import](/language-reference/definitions/modules/#import-commands) [\hiding](/language-reference/definitions/modules/#open-commands) [\as](/language-reference/definitions/modules/#open-commands) [\using](/language-reference/definitions/modules/#open-commands)
[\truncated](/language-reference/definitions/data) [\data](/language-reference/definitions/data) [\func](/language-reference/definitions/functions) [\lemma](/language-reference/definitions/functions/#lemmas) [\class](/language-reference/definitions/classes) [\record](/language-reference/definitions/records) [\field](/language-reference/definitions/records) [\property](/language-reference/definitions/records/#properties) [\extends](/language-reference/definitions/records) [\module](/language-reference/definitions/modules/#modules) [\instance](/language-reference/definitions/classes) [\use](/language-reference/definitions/coercion) [\coerce](/language-reference/definitions/coercion) [\level](/language-reference/definitions/level) 
[\with](/language-reference/definitions/functions/#pattern-matching) [\elim](/language-reference/definitions/functions/#elim) [\cowith](/language-reference/definitions/functions/#copattern-matching) [\where](/language-reference/definitions/modules/#where-blocks)
[\infix](/language-reference/definitions/#infix-operators) [\infixl](/language-reference/definitions/#infix-operators) [\infixr](/language-reference/definitions/#infix-operators) [\fix](/language-reference/definitions/#precedence) [\fixl](/language-reference/definitions/#precedence) [\fixr](/language-reference/definitions/#precedence)
[\new](/language-reference/expressions/class-ext) [\this](/language-reference/definitions/records) [\Pi](/language-reference/expressions/pi) [\Sigma](/language-reference/expressions/sigma) [\lam](/language-reference/expressions/pi) [\let](/language-reference/expressions/let) [\in]((/language-reference/expressions/let)) [\case](/language-reference/expressions/case) [\return](/language-reference/expressions/case)
[\lp](/language-reference/expressions/universes/#level-polymorphism) [\lh](/language-reference/expressions/universes/#level-polymorphism) [\suc](/language-reference/expressions/universes/#level-polymorphism) [\max](/language-reference/expressions/universes/#level-polymorphism) [\Prop](/language-reference/expressions/universes) [\Set](/language-reference/expressions/universes) [\Type](/language-reference/expressions/universes).

## Numerals

A _positive numeral_ is a non-empty sequence of digits.
A _negative numeral_ consists of `-` followed by a non-empty sequence of digits.

## Identifiers

An _identifier_ consists of a non-empty sequence of lower and upper case letters, digits, and characters from the list `~!@#$%^&*-+=<>?/|[];:_`.
Exceptions are sequences that begin with a digit, sequences that begin with `--`, numerals, and reserved names: `->`, `=>`, `_`, `:`, and `|`.

Examples:

* Valid identifiers: `xxx`, `+`, `$^~]!005x`, `::`, `->x`, `x:Nat`, `-5b`, `-33+7`.
* Invalid identifiers: `--xxx`, `5b`, `-33`, `->`.

## Infix and postfix notation

A _postfix notation_ is an identifier followed by `` ` ``.
An _infix notation_ is an identifier surrounded by `` ` ``.
Both of these notations are described in [this section](/language-reference/definitions).

## Comments

_Multi-line comments_ are enclosed in `{-` and `-}` and can be nested.
_Single-line comments_ consist of `--` followed by an arbitrary text until the end of the line.
The exception are identifiers which contain `--` in their names, but do not begin with `--`.
To give an example, `--`, `--------`, `--|`, `--foo`, and `-------foobar` are comments but `|--`, `%--foo`, and `x------foobar` are not.
