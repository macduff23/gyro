package gyro.lang.ast.expression;

import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser.ComparisonExpressionContext;

import static gyro.parser.antlr4.BeamParser.ExpressionContext;

public class ComparisonNode extends ExpressionNode {

    private String operator;

    public ComparisonNode(ExpressionContext context) {
        super(context);

        ComparisonExpressionContext compareContext = (ComparisonExpressionContext) context;
        operator = compareContext.operator().getText();
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Object leftValue = getLeftNode().evaluate(scope);
        Object rightValue = getRightNode().evaluate(scope);

        switch (operator) {
            case "==" : return leftValue.equals(rightValue);
            case "!=" : return !leftValue.equals(rightValue);
            default   : return false;
        }
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        builder.append(getLeftNode());
        builder.append(" ");
        builder.append(operator);
        builder.append(" ");
        builder.append(getRightNode());
    }
}