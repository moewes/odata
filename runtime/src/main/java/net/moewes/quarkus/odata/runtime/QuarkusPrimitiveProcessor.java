package net.moewes.quarkus.odata.runtime;

import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceProperty;

import java.util.Locale;

public class QuarkusPrimitiveProcessor implements PrimitiveProcessor {

    private final EdmRepository repository;
    private final ODataEntityConverter odataEntityConverter;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public QuarkusPrimitiveProcessor(EdmRepository repository) {
        this.repository = repository;
        odataEntityConverter = new ODataEntityConverter(repository);
    }

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readPrimitive(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(oDataRequest, oDataResponse, uriInfo);

        UriResource lastUriPart = context.getLastUriPart();

        UriResourceKind kind = lastUriPart.getKind();

        switch (lastUriPart.getKind()) {
            case function:
            case primitiveProperty:
            case navigationProperty:
                break;
            default:
                throw new ODataApplicationException("Not supported", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }

        UriResourceProperty uriProperty = (UriResourceProperty) lastUriPart;
        EdmProperty edmProperty = uriProperty.getProperty();
        EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();

        Property property = new Property();
        property.setValue(ValueType.PRIMITIVE, String.valueOf("Test"));

        context.respondWithPrimitive(property, edmPropertyType, contentType, HttpStatusCode.OK, odata, serviceMetadata);
    }

    @Override
    public void updatePrimitive(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType, ContentType contentType1) throws ODataApplicationException, ODataLibraryException {

    }

    @Override
    public void deletePrimitive(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {

    }


}
