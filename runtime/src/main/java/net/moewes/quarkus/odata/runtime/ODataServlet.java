package net.moewes.quarkus.odata.runtime;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@WebServlet
public class ODataServlet extends HttpServlet {

    @Inject
    EdmRepository repository;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(new EdmProvider(repository), new ArrayList<EdmxReference>());
        ODataHttpHandler handler = odata.createHandler(edm);
        handler.register(new QuarkusEntityCollectionProcessor(repository));
        handler.register(new QuarkusEntityProcessor(repository));
        handler.register(new QuarkusPrimitiveProcessor(repository));
        handler.register(new QuarkusActionProcessor(repository));
        handler.register(new QuarkusBatchProcessor());
        handler.process(req, resp);
    }
}
