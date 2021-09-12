package net.moewes.quarkus.odata.vertx;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ODataRecorder {

    public Handler<RoutingContext> getPageHandler() {

        return new OdataHandler();
    }

    public Handler<RoutingContext> getViewHandler(BeanContainer beanContainer) {
        //   return new ViewRequestHandler(beanContainer, Thread.currentThread().getContextClassLoader());
        return null;
    }
}
