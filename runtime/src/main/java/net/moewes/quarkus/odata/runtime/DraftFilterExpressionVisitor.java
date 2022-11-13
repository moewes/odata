package net.moewes.quarkus.odata.runtime;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import java.util.List;
import java.util.logging.Logger;

public class DraftFilterExpressionVisitor extends FilterExpressionVisitor {

    public enum DraftRelatedMembers {
        IsActiveEntity,
        HasDraftEntity,
        SiblingEntity_IsActiveEntity,

        DraftAdministrativeData_InProcessByUser,
        inactiveEntities,
        activeEntities,
        EntitiesWithNoDrafts,
        SiblingIsActiveEntityIsNull,
        SelectAll,
        SelectUnchanged, SelectEnqueued, ProcessedByOtherUser, SelectAllButDrafts

    }

    private final Logger log = Logger.getLogger("DraftFilterExpressionVisitor");

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind binaryOperatorKind, Object o, Object t1)
            throws ExpressionVisitException, ODataApplicationException {

        if (o instanceof DraftRelatedMembers) {
            if (t1 instanceof DraftRelatedMembers) {
                if (DraftRelatedMembers.inactiveEntities.equals(o)
                        && DraftRelatedMembers.SiblingIsActiveEntityIsNull.equals(t1)) {
                    log.info("select all");
                    return DraftRelatedMembers.SelectAll;
                }
                if (DraftRelatedMembers.inactiveEntities.equals(t1)
                        && DraftRelatedMembers.SiblingIsActiveEntityIsNull.equals(o)) {
                    log.info("select all");
                    return DraftRelatedMembers.SelectAll;
                }
                if (DraftRelatedMembers.activeEntities.equals(o)
                        && DraftRelatedMembers.EntitiesWithNoDrafts.equals(t1)) {
                    log.info("select unchanged");
                    return DraftRelatedMembers.SelectUnchanged;
                }
                if (DraftRelatedMembers.activeEntities.equals(t1)
                        && DraftRelatedMembers.EntitiesWithNoDrafts.equals(o)) {
                    log.info("select unchanged");
                    return DraftRelatedMembers.SelectUnchanged;
                }
                if (DraftRelatedMembers.activeEntities.equals(o)
                        && DraftRelatedMembers.DraftAdministrativeData_InProcessByUser.equals(t1)) {
                    log.info("select enqueued");
                    return DraftRelatedMembers.SelectEnqueued;
                }
                if (DraftRelatedMembers.activeEntities.equals(t1)
                        && DraftRelatedMembers.DraftAdministrativeData_InProcessByUser.equals(o)) {
                    log.info("select enqueued");
                    return DraftRelatedMembers.SelectEnqueued;
                }
                log.info("passed thru, both draft related " + o + " " + t1);
            } else {
                if (DraftRelatedMembers.IsActiveEntity.equals(o) && BinaryOperatorKind.EQ.equals(
                        binaryOperatorKind) && t1 instanceof Boolean && (((Boolean) t1) == false)) {
                    log.info("inactive Entities " + t1);
                    return DraftRelatedMembers.inactiveEntities;
                }
                if (DraftRelatedMembers.IsActiveEntity.equals(o) && BinaryOperatorKind.EQ.equals(
                        binaryOperatorKind) && t1 instanceof Boolean && (((Boolean) t1) == true)) {
                    log.info("active Entities " + t1);
                    return DraftRelatedMembers.activeEntities;
                }
                if (DraftRelatedMembers.HasDraftEntity.equals(o) && BinaryOperatorKind.EQ.equals(
                        binaryOperatorKind) && t1 instanceof Boolean && (((Boolean) t1) == false)) {
                    log.info("has no drafts " + t1);
                    return DraftRelatedMembers.EntitiesWithNoDrafts;
                }
                if (DraftRelatedMembers.SiblingEntity_IsActiveEntity.equals(o) && BinaryOperatorKind.EQ.equals(
                        binaryOperatorKind) && t1 == null) {
                    log.info("Sibling IsActiveEntity is null");
                    return DraftRelatedMembers.SiblingIsActiveEntityIsNull;
                }
                if (DraftRelatedMembers.DraftAdministrativeData_InProcessByUser.equals(o) && BinaryOperatorKind.EQ.equals(
                        binaryOperatorKind) && t1 == "") {
                    log.info("processed by other user");
                    return DraftRelatedMembers.ProcessedByOtherUser;
                }
                log.info("draft relevant, but not evaluated " + o);
            }
            return o;
        } else {
            log.info("passed thru, not draft related");
            return true;
        }
    }

    public Object visitMember(Member member) throws ODataApplicationException {

        List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();

        try {
            if (uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty) {
                return DraftRelatedMembers.valueOf(uriResourceParts.get(0).getSegmentValue());
            } else {
                return DraftRelatedMembers.valueOf(uriResourceParts.get(0)
                        .getSegmentValue() + "_" + uriResourceParts.get(1).getSegmentValue());
            }
        } catch (IllegalArgumentException e) {
            log.info("visitMember not found " + uriResourceParts);
            return null;
        }
    }
}
