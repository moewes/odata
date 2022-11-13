package net.moewes.quarkus.odata.runtime;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class EntityFilterExpressionVisitor extends DraftFilterExpressionVisitor {

    private final Logger log = Logger.getLogger("FilterExpressionVisitor");

    private final Entity currentEntity;

    public EntityFilterExpressionVisitor(Entity entity) {
        super();
        currentEntity = entity;
    }

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind binaryOperatorKind, Object o, Object t1)
            throws ExpressionVisitException, ODataApplicationException {

        if (o instanceof DraftRelatedMembers) {
            return true;
        }
        if (o == null) {
            log.info("visitBinaryOperator o = null");
            //   return null; // TODO
        }

        if (BinaryOperatorKind.EQ.equals(binaryOperatorKind)) {
            return o != null && o.equals(t1);
        }
        if (BinaryOperatorKind.NE.equals(binaryOperatorKind)) {
            return o != null && !o.equals(t1);
        }
        if (BinaryOperatorKind.OR.equals(binaryOperatorKind)) {
            boolean a = o != null && ((Boolean) o);
            boolean b = t1 != null && ((Boolean) t1);
            return a || b;
        }
        if (BinaryOperatorKind.AND.equals(binaryOperatorKind)) {
            boolean a = o != null && ((Boolean) o);
            boolean b = t1 != null && ((Boolean) t1);
            return a && b;
        }
        log.info("visitBinaryOperator " + binaryOperatorKind);
        return true;
    }

    public Object visitMember(Member member)
            throws ODataApplicationException {

        Object result = super.visitMember(member);

        if (result == null) {
            List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

            if (uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty) {
                return currentEntity.getProperty(((UriResourcePrimitiveProperty) uriResourceParts.get(
                        0)).getProperty()
                        .getName()).getValue();
            } else {
                log.info("visitMember");
                throw new ODataApplicationException("not implemented",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
            }
        }
        return result;
    }
}
