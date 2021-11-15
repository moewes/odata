package net.moewes.quarkus.odata.runtime;

import java.io.InputStream;
import java.util.List;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

public class ODataRequestContext {

    private final ODataRequest request;
    private final ODataResponse response;
    private final UriInfo uriInfo;

    public ODataRequestContext(ODataRequest oDataRequest, ODataResponse oDataResponse,
                               UriInfo uriInfo) {

        this.request = oDataRequest;
        this.response = oDataResponse;
        this.uriInfo = uriInfo;
    }

    public List<UriParameter> getKeyPredicates() {

        UriResourceEntitySet uriResourceEntitySet = getUriResourceEntitySet(0);
        return uriResourceEntitySet.getKeyPredicates();
    }

    public EdmEntitySet getEntitySet() {

        UriResourceEntitySet uriResourceEntitySet = getUriResourceEntitySet(0);
        return uriResourceEntitySet.getEntitySet();
    }

    public Entity getEntityFromRequest(OData odata, ContentType requestFormat) throws DeserializerException {
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
                                  HttpStatusCode statusCode, OData odata,
                                  ServiceMetadata serviceMetadata) throws SerializerException {

        ContextURL contextURL = ContextURL.with().entitySet(getEntitySet()).build();
        EntitySerializerOptions options =
                EntitySerializerOptions.with().contextURL(contextURL).build();
        ODataSerializer serializer = odata.createSerializer(contentType);
        SerializerResult serializerResult = serializer.entity(serviceMetadata,
                getEntitySet().getEntityType(), entity, options);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(statusCode.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    private UriResourceEntitySet getUriResourceEntitySet(int level) {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        return (UriResourceEntitySet) resourcePaths.get(level);
    }

    public boolean isPatch() {
        return HttpMethod.PATCH.equals(request.getMethod());
    }
}
