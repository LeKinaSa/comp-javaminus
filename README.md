**GROUP: 3B**

(Names, numbers, self assessment, and contribution of the members of the group to the project according to:)
* Clara Martins
    * **Nr:** 201806528
    * **Grade:** 0 to 20 value
    * **Contribution:** 0 to 100 %
* Daniel Monteiro
    * **Nr:** 201806185
    * **Grade:** 0 to 20 value
    * **Contribution:** 0 to 100 %
* Gonçalo Pascoal
    * **Nr:** 201806332
    * **Grade:** 0 to 20 value
    * **Contribution:** 0 to 100 %
* Rui Pinto
    * **Nr:** 201806441
    * **Grade:** 0 to 20 value
    * **Contribution:** 0 to 100 %

(Note that the sum of the CONTRIBUTION? values must be 100 %)

**GLOBAL Grade of the project:** <0 to 20>

### SUMMARY
(Describe what your tool does and its main features.)
Our compiler compiles code. ???

### DEALING WITH SYNTACTIC ERRORS
The grammar used is mostly LL(1), only using a lookahead of 2 to differentiate between a `VarDeclaration` and a `Statement`; it also doesn't allow `Identifier;` to be obtained from `Statement`.
It has error handling for while loops, only exiting at the end of the analysis.
It respects the operator precedence.

(Describe how the syntactic error recovery of your tool works. Does it exit after the first error?)

### SEMANTIC ANALYSIS
(Refer the semantic rules implemented by your tool.)

### CODE GENERATION
(describe how the code generation of your tool works and identify the possible problems your tool has regarding code generation.)

### TASK DISTRIBUTION
Most of the tasks were completed as a group. ___

(Identify the set of tasks done by each member of the project. You can divide this by checkpoint it if helps)

#### PROS
* It supports method overload
* It supports optimizations, such as register allocation, constant propagation and constant folding
* When the `if` condition is a constant, it identifies the program flow

(Identify the most positive aspects of your tool)

### CONS
(Identify the most negative aspects of your tool)