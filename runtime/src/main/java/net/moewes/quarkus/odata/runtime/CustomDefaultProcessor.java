package net.moewes.quarkus.odata.runtime;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.etag.ETagHelper;
import org.apache.olingo.server.api.etag.ServiceMetadataETagSupport;
import org.apache.olingo.server.api.processor.DefaultProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.uri.UriInfo;

public class CustomDefaultProcessor extends DefaultProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
        super.init(odata, serviceMetadata);
    }

    @Override
    public void readServiceDocument(ODataRequest request,
                                    ODataResponse response,
                                    UriInfo uriInfo,
                                    ContentType requestedContentType) throws
            ODataApplicationException, ODataLibraryException {
        boolean isNotModified = false;
        ServiceMetadataETagSupport eTagSupport =
                this.serviceMetadata.getServiceMetadataETagSupport();
        if (eTagSupport != null && eTagSupport.getServiceDocumentETag() != null) {
            response.setHeader("ETag", eTagSupport.getServiceDocumentETag());
            ETagHelper eTagHelper = this.odata.createETagHelper();
            isNotModified = eTagHelper.checkReadPreconditions(eTagSupport.getServiceDocumentETag(),
                    request.getHeaders("If-Match"),
                    request.getHeaders("If-None-Match"));
        }

        if (isNotModified) {
            response.setStatusCode(HttpStatusCode.NOT_MODIFIED.getStatusCode());
        } else if (HttpMethod.HEAD == request.getMethod()) {
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        } else {
            ODataSerializer serializer = this.odata.createSerializer(requestedContentType);
            response.setContent(serializer.serviceDocument(this.serviceMetadata,
                            request.getRawBaseUri())
                    .getContent());
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader("Content-Type", requestedContentType.toContentTypeString());
        }

    }
}
