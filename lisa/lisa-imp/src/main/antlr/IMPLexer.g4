/**
 * The lexer for the java subset
 * 
 * @author Luca Negrini
 */
lexer grammar IMPLexer;

@ lexer :: header
{package it.unive.lisa.imp.antlr;}
// =========================== KEYWORDS ===========================  

// basic types

BOOLEAN
   : 'boolean'
   ;

STRING
   : 'string'
   ;

FLOAT
   : 'float'
   ;

INT
   : 'int'
   ;
   // branching
   
IF
   : 'if'
   ;

ELSE
   : 'else'
   ;
   // loop
   
FOR
   : 'for'
   ;

WHILE
   : 'while'
   ;
   // class declaration
   
CLASS
   : 'class'
   ;

INTERFACE
   : 'interface'
   ;

IMPLEMENTS
   : 'implements'
   ;

ABSTRACT
   : 'abstract'
   ;

EXTENDS
   : 'extends'
   ;
   // method body    
   
DEFINE
   : 'def'
   ;

CONSTANT
   : 'const'
   ;

FINAL
   : 'final'
   ;

RETURN
   : 'return'
   ;

THROW
   : 'throw'
   ;

THIS
   : 'this'
   ;

SUPER
   : 'super'
   ;

BUMP
   : 'bump'
   ;

NEW
   : 'new'
   ;

ASSERT
   : 'assert'
   ;

IMPORT
   : 'import'
   ;

AS
   : 'as'
   ;

FROM
   : 'from'
   ;

   // =========================== STRING FUNCTIONS =========================== 
   
STRCAT
   : 'strcat'
   ;

STRCONTAINS
   : 'strcon'
   ;

STRENDS
   : 'strends'
   ;

STREQ
   : 'streq'
   ;

STRINDEXOF
   : 'strindex'
   ;

STRLEN
   : 'strlen'
   ;

STRREPLACE
   : 'strrep'
   ;

STRSTARTS
   : 'strstarts'
   ;

STRSUB
   : 'strsub'
   ;
   // =========================== ARRAY FUNCTIONS =========================== 
   
ARRAYLEN
   : 'arraylen'
   ;
   // =========================== LITERALS ===========================  
   
LITERAL_DECIMAL
   : '0'
   | [1-9] Digits?
   ;

LITERAL_FLOAT
   : (Digits '.' Digits? | '.' Digits) [fF]?
   ;

LITERAL_BOOL
   : 'true'
   | 'false'
   ;

LITERAL_STRING
   : '"' (~ ["\\\r\n] | EscapeSequence)* '"'
   ;

LITERAL_NULL
   : 'null'
   ;
   // =========================== SYMBOLS ===========================  
   
   // parenthesis
   
LPAREN
   : '('
   ;

RPAREN
   : ')'
   ;

LBRACE
   : '{'
   ;

RBRACE
   : '}'
   ;

LBRACK
   : '['
   ;

RBRACK
   : ']'
   ;
   // separators
   
TILDE
   : '~'
   ;

SEMI
   : ';'
   ;

COMMA
   : ','
   ;

DOT
   : '.'
   ;
   // operators
   
ASSIGN
   : '='
   ;

GT
   : '>'
   ;

LT
   : '<'
   ;

NOT
   : '!'
   ;

EQUAL
   : '=='
   ;

LE
   : '<='
   ;

GE
   : '>='
   ;

NOTEQUAL
   : '!='
   ;

AND
   : '&&'
   ;

OR
   : '||'
   ;

ADD
   : '+'
   ;

SUB
   : '-'
   ;

MUL
   : '*'
   ;

DIV
   : '/'
   ;

MOD
   : '%'
   ;
   // =========================== WHITESPACE ===========================  
   
WS
   : [ \t\r\n\u000C]+ -> channel (HIDDEN)
   ;
   // =========================== COMMENTS ===========================  
   
COMMENT
   : '/*' .*? '*/' -> channel (HIDDEN)
   ;

LINE_COMMENT
   : '//' ~ [\r\n]* -> channel (HIDDEN)
   ;
   // =========================== IDENTIFIERS ===========================  
   
IDENTIFIER
   : Letter LetterOrDigit*
   ;
   // =========================== RULES ===========================  
   
fragment EscapeSequence
   : '\\' [btnfr"'\\]
   | '\\' ([0-3]? [0-7])? [0-7]
   ;

fragment Digits
   : [0-9]+
   ;

fragment LetterOrDigit
   : Letter
   | [0-9]
   ;

fragment Letter
   : [a-zA-Z$_]
   ;