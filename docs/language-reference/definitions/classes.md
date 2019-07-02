<h1 id="classes">Classes<a class="headerlink" href="#classes" title="Permanent link">&para;</a></h1>

A _class_ is a record that has several useful properties.
A class is defined in the same way as a record, but it begins with the keyword `\class` instead of `\record`.
Classes can extend other classes and records and vice versa.
The first explicit parameter of a class is called its _classifying field_.
Classes are implicitly coerced to the type of the classifying field.
Let us consider an example:

```arend
\class Semigroup (E : \Set)
  | \infixl 7 * : E -> E -> E
  | *-assoc (x y z : E) : (x * y) * z = x * (y * z)

\func square {S : Semigroup} (x : S) => x * x
```

It is allowed to write `x : S` instead of `x : S.E` since `S` is implicitly coerced to an element of type `\Set`, that is `S.E`.

In case an parameter of a definition or a constructor has type, which is an [extension](/language-reference/expressions/class-ext) `C { ... }` of `C`,
it is marked as a _local instance_ of class `C`. Implicit parameter `\this` of a field of class `C` is also a local instance. 


If the parameter `p : C { ... }` of `f` is a local instance, then the value of `p` in a usage of `f` is inferred to 
a local instance `v : C { ... }`, visible in the context of the usage of `f`, if the expected value of the classifying
field of `p` coincides with the classifying field of `v`. 
For instance, the function `square` in the example above has one local instance `S` of the class `Semigroup`. The field
`*` of the class `Semigroup` is used in the body of `square` and the expected type of its call in `x * x` is 
`S.E -> S.E -> {?}`. This implies that the expected classifying field is `S.E`, which is the classifying field of the
local instance `S`, so this instance is inferred as the implicit argument of `*`.

It is also possible to define _global instances_.
To do this, one needs to use the keyword `\instance':

```arend
\instance NatSemigroup : Semigroup Nat
  | * => Nat.*
  | *-assoc => {?} -- the proof is omitted
```

A global instance is just a function, defined by [copattern matching](/language-reference/definitions/functions/#copattern-matching).
It can be used as an ordinary function.
The only difference with an ordinary function is that it can only be defined by copattern matching and must define an
instance of a class.
Also, the classifying field of an instance must be a data or a record applied to some arguments and if some parameters
of the instance have a class as a type, its classifying field must be an argument of the classifying field of the 
resulting instance.

If there is no local instance with the expected classifying field, then such an instance will be searched among
global instances.
There is no backtracking, so the first appropriate instance will be chosen.
A global instance is appropriate in a usage if the expected classifying field is the same data or record
as the data or the record in the classifying field of the instance. If this holds, the global instance
is appropriate even if the data or the record are applied to different arguments.
