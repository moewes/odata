package net.moewes.quarkus.odata.runtime;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmBoolean;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.*;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public abstract class FilterExpressionVisitor implements ExpressionVisitor<Object> {

    private final Logger log = Logger.getLogger("FilterExpressionVisitor");

    @Override
    public abstract Object visitBinaryOperator(BinaryOperatorKind binaryOperatorKind, Object o,
                                               Object t1)
            throws ExpressionVisitException, ODataApplicationException;

    @Override
    public Object visitUnaryOperator(UnaryOperatorKind unaryOperatorKind, Object o)
            throws ExpressionVisitException, ODataApplicationException {
        log.info("visitUnaryOperator");
        throw new ODataApplicationException("not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitMethodCall(MethodKind methodKind, List<Object> list)
            throws ExpressionVisitException, ODataApplicationException {
        log.info("visitMethodCall");
        throw new ODataApplicationException("not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitLambdaExpression(String s, String s1, Expression expression)
            throws ExpressionVisitException, ODataApplicationException {
        log.info("visitLambdaExpression");
        throw new ODataApplicationException("not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);

    }

    @Override
    public Object visitLiteral(Literal literal)
            throws ExpressionVisitException, ODataApplicationException {

        if (literal.getType() instanceof EdmString) {
            String stringResult = "";
            if (literal.getText().length() > 2) {
                stringResult = literal.getText().substring(1, literal.getText().length() - 1);
            }
            return stringResult;
        }
        if (literal.getType() instanceof EdmBoolean) {
            return Boolean.valueOf(literal.getText());
        }
        log.info("visitLiteral " + literal.getText());
        return null;
    }

    public abstract Object visitMember(Member member)
            throws ExpressionVisitException, ODataApplicationException;


    @Override
    public Object visitAlias(String s) throws ODataApplicationException {
        log.info("visitAlias");
        throw new ODataApplicationException("not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitTypeLiteral(EdmType edmType)
            throws ODataApplicationException {
        log.info("visitTypeLiteral");
        throw new ODataApplicationException("not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitLambdaReference(String s)
            throws ODataApplicationException {
        log.info("visitLambdaReference");
        throw new ODataApplicationException("not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitEnum(EdmEnumType edmEnumType, List<String> list)
            throws ODataApplicationException {
        log.info("visitEnum");
        throw new ODataApplicationException("not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind binaryOperatorKind,
                                      Object o,
                                      List<Object> list)
            throws ODataApplicationException {

        log.info("visitBinaryOperator");
        throw new ODataApplicationException("not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }
}
