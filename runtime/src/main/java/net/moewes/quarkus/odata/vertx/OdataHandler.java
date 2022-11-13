package net.moewes.quarkus.odata.vertx;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.*;

import java.io.ByteArrayInputStream;

public class OdataHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {

        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(null, null);
        ODataHttpHandler handler = odata.createHandler(edm);

        ODataRequest odRequest = new ODataRequest();

        odRequest.setBody(new ByteArrayInputStream(routingContext.getBodyAsString().getBytes()));
        odRequest.setProtocol(routingContext.request().version().name());
        odRequest.setMethod(HttpMethod.valueOf(routingContext.request().method().toString()));
        routingContext.request()
                .headers()
                .forEach(header -> odRequest.addHeader(header.getKey(), header.getValue()));
        String query = routingContext.request().query();

        String rawRequestUri = routingContext.request().absoluteURI();
        String rawODataPath = routingContext.request().uri();

        String rawBaseUri =
                rawRequestUri.substring(0, rawRequestUri.length() - rawODataPath.length());
        odRequest.setRawQueryPath(query);
        odRequest.setRawRequestUri(rawRequestUri + (query == null ? "" : "?" + query));
        odRequest.setRawODataPath(rawODataPath);
        odRequest.setRawBaseUri(rawBaseUri);
        odRequest.setRawServiceResolutionUri(rawODataPath);

        ODataResponse oDataResponse = handler.process(odRequest);
    }
}
