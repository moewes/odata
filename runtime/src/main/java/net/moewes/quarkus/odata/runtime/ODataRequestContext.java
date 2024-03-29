package net.moewes.quarkus.odata.runtime;

import lombok.Getter;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.serializer.*;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.*;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ODataRequestContext {

    protected final ODataRequest request;

    protected final OData odata;
    private final ODataResponse response;
    private final UriInfo uriInfo;

    private final int level;
    @Getter
    private final ODataRequestContext parentContext;

    protected final UriResource uriResource;

    public ODataRequestContext(OData odata, ODataRequest oDataRequest,
                               ODataResponse oDataResponse,
                               UriInfo uriInfo) {

        this(odata,
                oDataRequest,
                oDataResponse,
                uriInfo,
                uriInfo.getUriResourceParts().size() - 1);

    }

    private ODataRequestContext(OData odata, ODataRequest oDataRequest,
                                ODataResponse oDataResponse,
                                UriInfo uriInfo,
                                int level) {

        this.request = oDataRequest;
        this.odata = odata;
        this.response = oDataResponse;
        this.uriInfo = uriInfo;
        this.level = level;
        this.uriResource = uriInfo.getUriResourceParts().get(level);

        if (level != 0) {
            parentContext = new ODataRequestContext(odata, oDataRequest, oDataResponse, uriInfo,
                    level - 1);
        } else {
            parentContext = null;
        }
    }

    public UriResource getUriResource() {
        return uriResource;
    }

    public boolean isEntitySet() {
        return (uriResource instanceof UriResourceEntitySet);
    }

    public boolean isNavigation() {
        return (uriResource instanceof UriResourceNavigation);
    }

    public boolean isFunction() {
        return (uriResource instanceof UriResourceFunction);
    }

    public boolean isPrimitiveProperty() {
        return (uriResource instanceof UriResourcePrimitiveProperty);
    }

    public List<UriParameter> getKeyPredicates() {

        if (uriResource instanceof UriResourceEntitySet) {
            return ((UriResourceEntitySet) uriResource).getKeyPredicates();
        } else if (uriResource instanceof UriResourceNavigation) {
            return ((UriResourceNavigation) uriResource).getKeyPredicates();
        } else {
            return new ArrayList<>();
        }
    }

    public EdmEntitySet getEntitySet() {

        if (uriResource instanceof UriResourceEntitySet) {
            return ((UriResourceEntitySet) uriResource).getEntitySet();
        } else if (uriResource instanceof UriResourceNavigation) {
            UriResourceNavigation uriResourceNavigation =
                    (UriResourceNavigation) uriResource;

            EdmEntitySet parentEntitySet = getParentContext().getEntitySet();
            EdmNavigationProperty navigationProperty = uriResourceNavigation.getProperty();
            EdmBindingTarget relatedBindingTarget = parentEntitySet
                    .getRelatedBindingTarget(navigationProperty.getName());
            if (relatedBindingTarget != null) {
                return (EdmEntitySet) relatedBindingTarget;
            } else {
                return parentEntitySet;
            }
        } else {
            return null; // FIXME Throw Error?
        }
    }

    public EdmEntityType getEntityType() {

        if (uriResource instanceof UriResourceEntitySet) {
            return ((UriResourceEntitySet) uriResource).getEntitySet().getEntityType();
        } else if (uriResource instanceof UriResourceNavigation) {
            UriResourceNavigation uriResourceNavigation =
                    (UriResourceNavigation) uriResource;
            return uriResourceNavigation.getProperty().getType();
        } else {
            return null; // FIXME Throw Error?
        }
    }

    public Entity getEntityFromRequest(ContentType requestFormat)
            throws DeserializerException {
        InputStream inputStream = request.getBody();
        ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(inputStream,
                getEntitySet().getEntityType());

        return result.getEntity();
    }

    public void respondWithNoContent() {
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    public void respondWithEntity(Entity entity, ContentType contentType,
                                  HttpStatusCode statusCode,
                                  ServiceMetadata serviceMetadata) throws SerializerException {

        SelectOption selectOption = uriInfo.getSelectOption();
        ExpandOption expandOption = uriInfo.getExpandOption();

        String selectList =
                odata.createUriHelper().buildContextURLSelectList(getEntityType(),
                        expandOption, selectOption);

        ContextURL contextURL =
                ContextURL.with().entitySet(getEntitySet()).selectList(selectList).build();
        EntitySerializerOptions options =
                EntitySerializerOptions.with()
                        .contextURL(contextURL)
                        .select(selectOption)
                        .expand(expandOption)
                        .build();
        ODataSerializer serializer = odata.createSerializer(contentType);
        SerializerResult serializerResult = serializer.entity(serviceMetadata,
                getEntityType(), entity, options);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(statusCode.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    public void respondWithEntityCollection(EntityCollection entityCollection,
                                            ContentType contentType,
                                            HttpStatusCode statusCode,
                                            ServiceMetadata serviceMetadata)
            throws SerializerException {

        CountOption countOption = uriInfo.getCountOption();
        ExpandOption expandOption = uriInfo.getExpandOption();

        if (countOption != null && countOption.getValue()) {
            entityCollection.setCount(entityCollection.getEntities().size());
        }

        ContextURL contextURL =
                ContextURL.with()
                        .entitySet(getEntitySet())
                        .serviceRoot(URI.create(request.getRawBaseUri() + "/"))
                        .build();
        EntityCollectionSerializerOptions options =
                EntityCollectionSerializerOptions.with()
                        .contextURL(contextURL)
                        .count(countOption)
                        .expand(expandOption)
                        .build();
        ODataSerializer serializer = odata.createSerializer(contentType);
        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata,
                getEntitySet().getEntityType(), entityCollection, options);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(statusCode.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    public void respondWithPrimitive(Property property,
                                     EdmPrimitiveType edmPropertyType,
                                     ContentType contentType,
                                     HttpStatusCode statusCode,
                                     ServiceMetadata serviceMetadata) throws SerializerException {

        ContextURL contextURL = ContextURL.with().entitySet(getEntitySet()).build();
        PrimitiveSerializerOptions options =
                PrimitiveSerializerOptions.with().contextURL(contextURL).build();
        ODataSerializer serializer = odata.createSerializer(contentType);
        SerializerResult serializerResult = serializer.primitive(serviceMetadata,
                edmPropertyType, property, options);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(statusCode.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    public void respondWithPrimitiveCollection(Property property,
                                               EdmPrimitiveType edmPropertyType,
                                               ContentType contentType,
                                               HttpStatusCode statusCode,
                                               ServiceMetadata serviceMetadata)
            throws SerializerException {

        ContextURL contextURL = ContextURL.with().entitySet(getEntitySet()).build();
        PrimitiveSerializerOptions options =
                PrimitiveSerializerOptions.with().contextURL(contextURL).build();
        ODataSerializer serializer = odata.createSerializer(contentType);
        SerializerResult serializerResult = serializer.primitiveCollection(serviceMetadata,
                edmPropertyType, property, options);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(statusCode.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }


    public UriResource getLastUriPart() {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        return resourcePaths.get(resourcePaths.size() - 1);
    }

    public boolean isPatch() {
        return HttpMethod.PATCH.equals(request.getMethod());
    }

    public EdmNavigationProperty getNavigationProperty() {

        if (isNavigation()) {
            return ((UriResourceNavigation) uriResource).getProperty();
        } else {
            return null;
        }
    }

    public boolean hasExpandedEntities() {
        return uriInfo.getExpandOption() != null;
    }

    public List<ExpandItem> getExpandItems() {
        return uriInfo.getExpandOption().getExpandItems();
    }

    public Object preSelectionFilterEvaluation() throws ODataApplicationException {

        FilterOption filterOption = uriInfo.getFilterOption();
        if (filterOption != null) {
            Expression filterExpression = filterOption.getExpression();

            DraftFilterExpressionVisitor draftVisitor = new DraftFilterExpressionVisitor();
            try {
                return filterExpression.accept(draftVisitor);
            } catch (ExpressionVisitException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
