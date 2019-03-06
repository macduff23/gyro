parser grammar BeamParser;

import BeamReferenceParser;

options { tokenVocab = BeamLexer; }

beamFile : file* EOF;
file     : (keyValue | resource | forStmt | ifStmt | importStmt | virtualResource);
blockEnd : END;

resource     : resourceType resourceName? resourceBody* blockEnd;
resourceBody : keyValue | resource | forStmt | ifStmt;

resourceType : IDENTIFIER;
resourceName : IDENTIFIER | stringValue;

importStmt  : IMPORT importPath (AS importName)?;
importPath  : IDENTIFIER;
importName  : IDENTIFIER;

virtualResource      : VR virtualResourceName virtualResourceParam* DEFINE virtualResourceBody* blockEnd;
virtualResourceParam : PARAM IDENTIFIER;
virtualResourceName  : IDENTIFIER;
virtualResourceBody  : keyValue | resource | forStmt | ifStmt;

// -- Control Structures

controlBody  : controlStmts*;
controlStmts : keyValue | resource | forStmt | ifStmt;

forStmt      : FOR forVariables IN (listValue | referenceValue) controlBody blockEnd;
forVariables : forVariable (COMMA forVariable)*;
forVariable  : IDENTIFIER;

ifStmt       : IF expression controlBody (ELSEIF expression controlBody)* (ELSE controlBody)? blockEnd;

expression
    : value                          # ValueExpression
    | expression operator expression # ComparisonExpression
    | expression OR expression       # OrExpression
    | expression AND expression      # AndExpression
    ;

operator     : EQ | NOTEQ;

// -- Key/Value blocks.
//
// Regular key/value blocks can have values that contain references.
// Simple key/value blocks cannot have value references.
keyValue       : key value;
key            : (IDENTIFIER | STRING_LITERAL | keywords) keyDelimiter;
keywords       : IMPORT | AS | VR | PARAM | DEFINE;
keyDelimiter   : COLON;

// -- Value Types
value        : listValue | mapValue | stringValue | booleanValue | numberValue | referenceValue;
numberValue  : DECIMAL_LITERAL | FLOAT_LITERAL;
booleanValue : TRUE | FALSE;
stringValue  : stringExpression | STRING_LITERAL;

mapValue
    : LCURLY keyValue? (COMMA keyValue)* RCURLY
    | LCURLY keyValue? (COMMA keyValue)* {
          notifyErrorListeners("Extra ',' in map");
      } COMMA RCURLY
    ;

listValue
    : LBRACKET listItemValue? (COMMA listItemValue)* RBRACKET
    ;

listItemValue : stringValue | booleanValue | numberValue | referenceValue;