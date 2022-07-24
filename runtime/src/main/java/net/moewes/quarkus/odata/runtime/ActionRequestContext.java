package net.moewes.quarkus.odata.runtime;

import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmReturnType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;

import java.util.Map;

public class ActionRequestContext extends ODataRequestContext {

    private final EdmAction edmAction;

    public ActionRequestContext(ODataRequest oDataRequest,
                                ODataResponse oDataResponse,
                                UriInfo uriInfo) {
        super(oDataRequest, oDataResponse, uriInfo);

        UriResource lastUriPart = getLastUriPart();

        if (lastUriPart instanceof UriResourceAction) {
            edmAction = ((UriResourceAction) lastUriPart).getAction();
        } else {
            throw new ODataRuntimeException("LastUriPart not of type UriResourceAction");
        }
    }

    public String getActionName() {
        return edmAction.getName();
    }

    public EdmReturnType getEdmReturnType() {
        return edmAction.getReturnType();
    }

    // FIXME eleminate odata import parameter
    public Map<String, Parameter> getActionParameter(ContentType importContentType,
                                                     OData odata)
            throws DeserializerException {

        ODataDeserializer deserializer = odata.createDeserializer(importContentType);
        return
                deserializer.actionParameters(request.getBody(), edmAction)
                        .getActionParameters();
    }
}
