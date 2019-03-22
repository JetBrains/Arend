<h1 id="hits">Higher Inductive Types<a class="headerlink" href="#hits" title="Permanent link">&para;</a></h1>

Higher inductive types generalize ordinary 
[inductive types](/language-reference/definitions/data).
Various homotopy colimits of types and other constructions such as
truncations are higher inductive types. A specific homotopy structure of a higher inductive
type can be defined by means of _conditions_ in data definitions.

# Conditions

If `con` is a constructor of an inductive type `D`, then an expression of the form
`con a_1 ... a_n` does not evaluate unless the definition of `D` contains _conditions_ on `con`.
A condition on a constructor is a rule that says how such an expression might evaluate.
For example, one can define integers as a data type with two constructors for positive and negative integers.
One can add a condition on the second constructor that says that positive and negative zero are computationally equal:

```arend
\data Int
  | pos Nat
  | neg Nat \with {
    | zero => pos zero
  }
```

Conditions are imposed on a constructor by defining it as a function by
[pattern matching](/language-reference/definitions/functions/#pattern-matching).
The only differences are that it is not required that all cases are covered and that pattern matching on constructors
`left` and `right` of the [interval type](/language-reference/prelude) `I` is allowed.
The general syntax is the same as for ordinary pattern matching.
Either `\with { | c_1 | ... | c_m }` or `\elim x_1, ... x_n { | c_1 | ... | c_m }` can be added after parameters
of the constructor, where `| c_1 | ... | c_m` is a list of clauses.

A constructor with conditions evaluates if its arguments match the specification in the same way as a function defined by pattern matching.
This means that a function over a data type with conditions must respect the conditions, this is checked
by the typechecker.
For example, a function of type `Int -> X` must map positive and negative zero to the same value.
Thus, one cannot define the following function:

```arend
\func f (x : Int) : Nat
  | pos n => n
  | neg n => suc n
```

# Higher inductive types

A higher inductive type is a data type with a constructor that has conditions of the form `| left => e` and `| right => e'`.
Let us give a few examples:

```arend
-- Circle
\data S1
  | base
  | loop I \with {
    | left => base
    | right => base
  }

-- Suspension
\data Susp (A : \Type)
  | north
  | south
  | merid A (i : I) \elim i {
    | left => north
    | right => south
  }

-- Propositional truncation
\data Trunc (A : \Type)
  | inT A
  | truncT (x y : Trunc A) (i : I) \elim i {
    | left => x
    | right => x
  }

-- Set quotient
\data Quotient (A : \Type) (R : A -> A -> \Type)
  | inQ A
  | equivQ (x y : A) (R x y) (i : I) \elim i {
    | left => inQ x
    | right => inQ y
  }
  | truncQ (a a' : Quotient A R) (p p' : a = a') (i j : I) \elim i, j {
    | i, left  => p @ i
    | i, right => p' @ i
    | left,  _ => a
    | right, _ => a'
  }
```

If `X` is a proposition, then, to define a function of type `Trunc A -> X`, it is enough to specify its value for `inT a`.
The same works for any higher inductive type and any level.
For example, to define a function `Quotient A -> X`, it is enough to specify its value for `inQ a` and `equivQ x y r i` if `X` is a set and only for `inQ a` if it is a proposition.
This also works for several arguments.
For example, if `X` is a set, then, to define a function `Quotient A -> Quotient A -> X`, it is enough to specify its value for `inQ a, inQ a'`, `inQ a, equivQ x y r i`, and `equivQ x y r i, inQ a`.
