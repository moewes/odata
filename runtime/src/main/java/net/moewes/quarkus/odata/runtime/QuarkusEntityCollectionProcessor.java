package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class QuarkusEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private final EdmRepository repository;
    private final ODataEntityConverter odataEntityConverter;

    public QuarkusEntityCollectionProcessor(EdmRepository repository) {
        this.repository = repository;
        odataEntityConverter = new ODataEntityConverter(repository);
    }

    @Override
    public void readEntityCollection(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType)
            throws ODataApplicationException, ODataLibraryException {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        EntityCollection entitySet = getData(edmEntitySet);

        ODataSerializer serializer = odata.createSerializer(contentType);

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextURL = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = oDataRequest.getRawBaseUri() + "/" + edmEntitySet.getName();

        EntityCollectionSerializerOptions options = EntityCollectionSerializerOptions.with().id(id).contextURL(contextURL).build();
        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, options);

        InputStream serializedContent = serializerResult.getContent();

        oDataResponse.setContent(serializedContent);
        oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
        oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
    }

    private EntityCollection getData(EdmEntitySet edmEntitySet) {

        EntityCollection collection = new EntityCollection();

        repository.findEntitySet(edmEntitySet.getName()).ifPresent(entitySet -> {
            Object serviceBean = repository.getServiceBean(entitySet);
            if (serviceBean instanceof EntityCollectionProvider<?>) {
                Object dataCollection = ((EntityCollectionProvider<?>) serviceBean).getCollection();

                if (dataCollection instanceof Collection) {
                    ((Collection<?>) dataCollection).forEach(data -> {
                        Entity entity = new Entity();
                        odataEntityConverter.convertDataToFrameworkEntity(entity, entitySet, data);

                        collection.getEntities().add(entity);
                    });
                }
            }
        });
        return collection;
    }

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }
}
