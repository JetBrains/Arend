<h1 id="modules-sec">Modules<a class="headerlink" href="#modules-sec" title="Permanent link">&para;</a></h1>

Every definition defined in a file is visible anywhere in this file.
That is, the order of definitions does not matter.
The module system allows you to define definitions in other namespaces.

## Modules

A module consists of a name and a list of definitions:

```arend
\module Mod \where {
  def_1
  ...
  def_n
}
```

You can refer to definitions `def_1` ... `def_n` inside module `Mod` by their names.
To refer to them outside this module, you need to use their full names `Mod.def_1` ... `Mod.def_n`.
For example, consider the following code:

```arend
\func f2 => Mod.f1
\module Mod \where {
  \func f1 => f2
  \func f2 => 0
  \func f3 => f4
}
\func f4 => f2
```

You cannot refer to `f1` in `f2` without `Mod.` prefix.
Function `f4` refers to `f2` defined on the top level.
Function `Mod.f2` hides the top level `f2` inside module `Mod`, so `Mod.f1` refers to `Mod.f2`.
You can refer to top level functions inside modules as shown in the example where `Mod.f3` refers to `f4`.

If a `\where` block contains a single definition, you can omit curly braces.

## Where blocks

Every definition has an associated module with the same name.
To add definitions to this module, you can write the `\where` block at the end of this definition.
Definitions defined in the associated module of a definition are visible inside this definition.

```arend
\func f => g \where \func g => 0
\func h => f.g \where
  \data D \where {
    \func k => D
    \func s => M.g.N.s
  }
\module M \where
  \func g => N.s \where {
    \module N \where {
      \func s => E
    }
    \data E
  }
```

Constructors of a `\data` definition and fields of a `\class` or a `\record` definition are defined inside the module associated to the definition, but they are also visible outside this module.

## Open commands

A module can be opened.
This makes definitions defined inside this module visible in the current scope.
The `\open` command affects all definitions in the current scope.

```arend
\func h1 => f
\module M \where {
  \func f => 0
  \func g => 1
}
\open M
\func h2 => g
```

The command `\open M (def_1, ... def_n)` opens only definitions `def_1`, ... `def_n`.
Other definitions must be refered to by their full names.
The command `\open M \hiding (def_1, ... def_n)` opens all of the definitions of `M` except for `def_1`, ... `def_n`.
These definitions still can be refered to by their full names.
The command `\open M (def_1 \as def_1', ... def_n \as def_n')` opens definitions `def_1`, ... `def_n`, but renames them to `def_1'`, ... `def_n'`, respectively.
If you want to open all of the definitions and rename some of them, use the command `\open M \using (def_1 \as def_1', ... def_n \as def_n')`.

```arend
\module M \where {
  \func f => 0
  \func g => 1
  \func h => 2
}
\module M1 \where {
  \open M (f,g)
  \func h1 => f
  \func h2 => g
  \func h3 => M.h -- we can refer to M.h only by its full name.
}
\module M2 \where {
  \open M \hiding (f,g)
  \func h1 => M.f -- we can refer to M.f and M.g only by their full names.
  \func h2 => M.g
  \func h3 => h
}
\module M3 \where {
  \open M1 (h1 \as M1_h1, h2)
  \open M2 \using (h2 \as M2_h2) \hiding (h3)
  \func k1 => M1_h1 -- this refers to M1.h1
  \func k2 => h1 -- this refers to M2.h1
  \func k3 => h2 -- this refers to M1.h2
  \func k4 => M2_h2 -- this refers to M2.h2
  \func k5 => M1.h3 -- we can refer to M1.h3 only by its full name.
}
```

Note that if you open a module `M` inside a module `M'` and then open `M'` inside `M''`, then definitions from `M` will not be visible in `M''`.
You need to explicitly open `M` inside `M''` to make them visible.

## Import commands

If you have several files, you can use the `\import` command to make one of them visible in the other.
For example, suppose that we have files `A.vc`, `B.vc`, a directory `Dir`, and a file `Dir/C.vc` with the following content:

```arend
-- A.vc
\func a1 => 0
\func a2 => 0
  \where \func a3 => 0
```

```arend
-- Dir/C.vc
\import A

\func c1 => a1
\func c2 => a2.a3
```

```arend
-- B.vc
\import Dir.C

\func b1 => c1
-- \func b2 => a1 -- definitions from file A are not visible
-- \func b3 => A.a1 -- you cannot refer to definitions from file A by their full names.
\func b4 => Dir.C.c2 -- you can refer to definitions from file Dir/C.vc by their full names.
```

The `\import` command also opens the content of the imported file.
You can use the same syntax as for `\open` commands to control which definitions will be opened.
If you want only import a file and not open any definitions, you can write `\import X ()`.
Then you can refer to definitions defined in file `X` by their full names:

```arend
-- X.vc
\func f => 0
```

```arend
-- Y.vc
\import X()

\func f => X.f
```
