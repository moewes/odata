package net.moewes.quarkus.odata.runtime;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.moewes.quarkus.odata.runtime.edm.CsdlBuilder;
import net.moewes.quarkus.odata.runtime.edm.EdmProvider;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;

import java.util.ArrayList;
import java.util.logging.Logger;

@WebServlet
public class ODataServlet extends HttpServlet {

    @Inject
    EdmRepository repository;

    @Inject
    CsdlBuilder csdlBuilder;

    private ODataHandler handler;

    @Override
    public void init() throws ServletException {
        super.init();

        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(new EdmProvider(repository, csdlBuilder),
                new ArrayList<>());
        //handler = odata.createHandler(edm);
        handler = new ODataHandler(odata, edm);
        // handler.register(new CustomDefaultProcessor());
        handler.register(new QuarkusEntityCollectionProcessor(repository));
        handler.register(new QuarkusEntityProcessor(repository));
        handler.register(new QuarkusPrimitiveProcessor(repository));
        handler.register(new QuarkusActionProcessor(repository));
        handler.register(new QuarkusBatchProcessor());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {

        Logger.getLogger("OData request").info(req.getMethod() + " " + req.getPathInfo());
        handler.process(req, resp);
        Logger.getLogger("OData request").info("executed");
    }
}
