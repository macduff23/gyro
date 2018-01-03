grammar Beam;

@header {
    package beam.parser.antlr4;
}

/*
 * Parser Rules
 */

beamRoot
    : globalScope EOF
    ;

globalScope
    : (providerBlock | method)+
    ;

resourceScope
    :  (providerBlock | method | keyValueBlock)+
    ;

providerBlock
    : PROVIDER_NAME LBLOCK (resourceScope)* RBLOCK
    ;

key
    : ID COLON
    ;

value
    : (ID | QUOTED_STRING)+
    ;

keyValueBlock
    : key value
    ;

method
    : (
        (METHOD methodArguments (COMMA methodArguments)*) |
        (METHOD LPAREN methodArguments (COMMA methodArguments)* RPAREN)
      )+
    ;

methodArguments
    : (QUOTED_STRING | methodNamedArgument)+
    ;

methodNamedArgument
    : ID MAP_ASSIGN QUOTED_STRING
    ;

/*
 * Lexer Rules
 */

fragment LETTER : [a-zA-Z_.] ;
fragment DIGIT  : [0-9] ;

QUOTED_STRING
    : '"' ~('\\'|'"')* '"'
    | '\'' ~('\''|'"')* '\''
    ;

ID
    : LETTER (LETTER|'0'..'9'|' ')*
    ;

PROVIDER_NAME
    : ID '::' ID ;

INT
    : DIGIT+ ;

// Keywords

METHOD_INCLUDE: 'include' ;
METHOD_IMPORT : 'import' ;

HASH          : '#' ;
LPAREN        : '(' ;
RPAREN        : ')' ;
LBLOCK        : '{' ;
RBLOCK        : '}' ;
METHOD        : '@' ID ;
MAP_ASSIGN    : '=>' ;
COMMA         : ',' ;
COLON         : ':' ;

// Whitespace

NEWLINE       : [\n\r]+ -> channel(HIDDEN);
WHITESPACE    : [ \u000C\t]+ -> channel(HIDDEN);
COMMENT       : '#' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN) ;