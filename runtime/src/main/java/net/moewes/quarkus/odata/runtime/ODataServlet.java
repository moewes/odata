package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.runtime.edm.CsdlBuilder;
import net.moewes.quarkus.odata.runtime.edm.EdmProvider;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
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
import java.util.logging.Logger;

@WebServlet
public class ODataServlet extends HttpServlet {

    @Inject
    EdmRepository repository;

    @Inject
    CsdlBuilder csdlBuilder;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Logger.getLogger("req").info(req.getMethod() + " " + req.getPathInfo());
        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(new EdmProvider(repository, csdlBuilder),
                new ArrayList<>());
        ODataHttpHandler handler = odata.createHandler(edm);
        handler.register(new QuarkusEntityCollectionProcessor(repository));
        handler.register(new QuarkusEntityProcessor(repository));
        handler.register(new QuarkusPrimitiveProcessor(repository));
        handler.register(new QuarkusActionProcessor(repository));
        handler.register(new QuarkusBatchProcessor());
        handler.process(req, resp);
    }
}
