package net.moewes.quarkus.odata.runtime;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.batch.BatchFacade;
import org.apache.olingo.server.api.deserializer.batch.BatchOptions;
import org.apache.olingo.server.api.deserializer.batch.BatchRequestPart;
import org.apache.olingo.server.api.deserializer.batch.ODataResponsePart;
import org.apache.olingo.server.api.processor.BatchProcessor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuarkusBatchProcessor implements BatchProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    @Override
    public void processBatch(BatchFacade batchFacade, ODataRequest oDataRequest, ODataResponse oDataResponse) throws ODataApplicationException, ODataLibraryException {

        final String boundary = batchFacade
                .extractBoundaryFromContentType(oDataRequest.getHeader(HttpHeader.CONTENT_TYPE));
        final BatchOptions options = BatchOptions.with().rawBaseUri(oDataRequest.getRawBaseUri())
                .rawServiceResolutionUri(oDataRequest.getRawServiceResolutionUri())
                .build();

        final List<BatchRequestPart> requestParts = odata.createFixedFormatDeserializer()
                .parseBatchRequest(oDataRequest.getBody(), boundary, options);

        final List<ODataResponsePart> responseParts = new ArrayList<>();
        for (final BatchRequestPart part : requestParts) {
            responseParts.add(batchFacade.handleBatchRequest(part));
        }

        final String responseBoundary = "batch_" + UUID.randomUUID().toString();
        final InputStream responseContent = odata.createFixedFormatSerializer().batchResponse(responseParts, responseBoundary);

        oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, ContentType.MULTIPART_MIXED + ";boundary=" + responseBoundary);
        oDataResponse.setContent(responseContent);
        oDataResponse.setStatusCode(HttpStatusCode.ACCEPTED.getStatusCode());
    }

    @Override
    public ODataResponsePart processChangeSet(BatchFacade batchFacade, List<ODataRequest> list) throws ODataApplicationException, ODataLibraryException {

        final List<ODataResponse> responses = new ArrayList<>();

        // TODO Transaction?
        for (final ODataRequest request : list) {
            ODataResponse response = batchFacade.handleODataRequest(request);

            if (response.getStatusCode() < 400) {
                responses.add(response);
            } else {
                return new ODataResponsePart(response, false);
            }
        }
        return new ODataResponsePart(responses, true);
    }

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }
}
