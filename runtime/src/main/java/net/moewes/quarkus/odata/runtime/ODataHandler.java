package net.moewes.quarkus.odata.runtime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.DeserializerException.MessageKeys;
import org.apache.olingo.server.api.etag.CustomETagSupport;
import org.apache.olingo.server.api.processor.Processor;
import org.apache.olingo.server.api.serializer.CustomContentTypeSupport;
import org.apache.olingo.server.core.ODataExceptionHelper;
import org.apache.olingo.server.core.ODataHandlerException;
import org.apache.olingo.server.core.ODataHandlerImpl;
import org.apache.olingo.server.core.debug.ServerCoreDebugger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;

public class ODataHandler { //implements ODataHttpHandler {
    public static final int COPY_BUFFER_SIZE = 8192;
    private static final String REQUESTMAPPING = "requestMapping";
    private final ODataHandlerImpl handler;
    private final ServerCoreDebugger debugger;
    private int split = 0;

    public ODataHandler(OData odata, ServiceMetadata serviceMetadata) {
        this.debugger = new ServerCoreDebugger(odata);
        this.handler = new ODataHandlerImpl(odata, serviceMetadata, this.debugger);
    }

    public ODataResponse process(ODataRequest request) {
        return this.handler.process(request);
    }

    public void process(HttpServletRequest request, HttpServletResponse response) {
        ODataRequest odRequest = new ODataRequest();
        Exception exception = null;
        //this.debugger.resolveDebugMode(request);
        int processMethodHandle =
                this.debugger.startRuntimeMeasurement("ODataHttpHandlerImpl", "process");

        ODataResponse odResponse;
        try {
            this.fillODataRequest(odRequest, request, this.split);
            odResponse = this.process(odRequest);
        } catch (Exception var8) {
            exception = var8;
            odResponse = this.handleException(odRequest, var8);
        }

        this.debugger.stopRuntimeMeasurement(processMethodHandle);
        if (this.debugger.isDebugMode()) {
            Map<String, String> serverEnvironmentVariables =
                    this.createEnvironmentVariablesMap(request);
            if (exception == null) {
                exception = this.handler.getLastThrownException();
            }

            odResponse = this.debugger.createDebugResponse(odRequest,
                    odResponse,
                    exception,
                    this.handler.getUriInfo(),
                    serverEnvironmentVariables);
        }

        convertToHttp(response, odResponse);
    }

    private Map<String, String> createEnvironmentVariablesMap(HttpServletRequest request) {
        Map<String, String> environment = new LinkedHashMap();
        environment.put("authType", request.getAuthType());
        environment.put("localAddr", request.getLocalAddr());
        environment.put("localName", request.getLocalName());
        environment.put("localPort", this.getIntAsString(request.getLocalPort()));
        environment.put("pathInfo", request.getPathInfo());
        environment.put("pathTranslated", request.getPathTranslated());
        environment.put("remoteAddr", request.getRemoteAddr());
        environment.put("remoteHost", request.getRemoteHost());
        environment.put("remotePort", this.getIntAsString(request.getRemotePort()));
        environment.put("remoteUser", request.getRemoteUser());
        environment.put("scheme", request.getScheme());
        environment.put("serverName", request.getServerName());
        environment.put("serverPort", this.getIntAsString(request.getServerPort()));
        environment.put("servletPath", request.getServletPath());
        return environment;
    }

    private String getIntAsString(int number) {
        return number == 0 ? "unknown" : Integer.toString(number);
    }

    public void setSplit(int split) {
        this.split = split;
    }

    private ODataResponse handleException(ODataRequest odRequest, Exception e) {
        ODataResponse resp = new ODataResponse();
        ODataServerError serverError;
        if (e instanceof ODataHandlerException) {
            serverError = ODataExceptionHelper.createServerErrorObject((ODataHandlerException) e,
                    (Locale) null);
        } else if (e instanceof ODataLibraryException) {
            serverError = ODataExceptionHelper.createServerErrorObject((ODataLibraryException) e,
                    (Locale) null);
        } else {
            serverError = ODataExceptionHelper.createServerErrorObject(e);
        }

        this.handler.handleException(odRequest, resp, serverError, e);
        return resp;
    }

    static void convertToHttp(HttpServletResponse response, ODataResponse odResponse) {
        response.setStatus(odResponse.getStatusCode());
        Iterator var2 = odResponse.getAllHeaders().entrySet().iterator();

        while (var2.hasNext()) {
            Map.Entry<String, List<String>> entry = (Map.Entry) var2.next();
            Iterator var4 = ((List) entry.getValue()).iterator();

            while (var4.hasNext()) {
                String headerValue = (String) var4.next();
                response.addHeader((String) entry.getKey(), headerValue);
            }
        }

        if (odResponse.getContent() != null) {
            copyContent(odResponse.getContent(), response);
        } else if (odResponse.getODataContent() != null) {
            writeContent(odResponse, response);
        }

    }

    static void writeContent(ODataResponse odataResponse, HttpServletResponse servletResponse) {
        try {
            ODataContent res = odataResponse.getODataContent();
            res.write(Channels.newChannel(servletResponse.getOutputStream()));
        } catch (IOException var3) {
            throw new ODataRuntimeException("Error on reading request content", var3);
        }
    }

    static void copyContent(InputStream inputStream, HttpServletResponse servletResponse) {
        copyContent(Channels.newChannel(inputStream), servletResponse);
    }

    static void copyContent(ReadableByteChannel input, HttpServletResponse servletResponse) {
        try {
            WritableByteChannel output = Channels.newChannel(servletResponse.getOutputStream());
            Throwable var3 = null;

            try {
                ByteBuffer inBuffer = ByteBuffer.allocate(8192);

                while (input.read(inBuffer) > 0) {
                    inBuffer.flip();
                    output.write(inBuffer);
                    inBuffer.clear();
                }
            } catch (Throwable var21) {
                var3 = var21;
                throw var21;
            } finally {
                if (output != null) {
                    if (var3 != null) {
                        try {
                            output.close();
                        } catch (Throwable var20) {
                            var3.addSuppressed(var20);
                        }
                    } else {
                        output.close();
                    }
                }

            }
        } catch (IOException var23) {
            throw new ODataRuntimeException("Error on reading request content", var23);
        } finally {
            closeStream(input);
        }

    }

    private static void closeStream(Channel closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException var2) {
            }
        }

    }

    private ODataRequest fillODataRequest(ODataRequest odRequest,
                                          HttpServletRequest httpRequest,
                                          int split) throws ODataLibraryException {
        int requestHandle =
                this.debugger.startRuntimeMeasurement("ODataHttpHandlerImpl", "fillODataRequest");

        ODataRequest var6;
        try {
            odRequest.setBody(httpRequest.getInputStream());
            odRequest.setProtocol(httpRequest.getProtocol());
            odRequest.setMethod(extractMethod(httpRequest));
            int innerHandle =
                    this.debugger.startRuntimeMeasurement("ODataHttpHandlerImpl", "copyHeaders");
            copyHeaders(odRequest, httpRequest);
            this.debugger.stopRuntimeMeasurement(innerHandle);
            innerHandle = this.debugger.startRuntimeMeasurement("ODataHttpHandlerImpl",
                    "fillUriInformation");
            fillUriInformation(odRequest, httpRequest, split);
            this.debugger.stopRuntimeMeasurement(innerHandle);
            var6 = odRequest;
        } catch (IOException var10) {
            throw new DeserializerException("An I/O exception occurred.",
                    var10,
                    MessageKeys.IO_EXCEPTION,
                    new String[0]);
        } finally {
            this.debugger.stopRuntimeMeasurement(requestHandle);
        }

        return var6;
    }

    static HttpMethod extractMethod(HttpServletRequest httpRequest) throws ODataLibraryException {
        HttpMethod httpRequestMethod;
        try {
            httpRequestMethod = HttpMethod.valueOf(httpRequest.getMethod());
        } catch (IllegalArgumentException var5) {
            throw new ODataHandlerException("HTTP method not allowed" + httpRequest.getMethod(),
                    var5,
                    org.apache.olingo.server.core.ODataHandlerException.MessageKeys.HTTP_METHOD_NOT_ALLOWED,
                    new String[]{httpRequest.getMethod()});
        }

        try {
            if (httpRequestMethod == HttpMethod.POST) {
                String xHttpMethod = httpRequest.getHeader("X-HTTP-Method");
                String xHttpMethodOverride = httpRequest.getHeader("X-HTTP-Method-Override");
                if (xHttpMethod == null && xHttpMethodOverride == null) {
                    return httpRequestMethod;
                } else if (xHttpMethod == null) {
                    return HttpMethod.valueOf(xHttpMethodOverride);
                } else if (xHttpMethodOverride == null) {
                    return HttpMethod.valueOf(xHttpMethod);
                } else if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
                    throw new ODataHandlerException("Ambiguous X-HTTP-Methods",
                            org.apache.olingo.server.core.ODataHandlerException.MessageKeys.AMBIGUOUS_XHTTP_METHOD,
                            new String[]{xHttpMethod, xHttpMethodOverride});
                } else {
                    return HttpMethod.valueOf(xHttpMethod);
                }
            } else {
                return httpRequestMethod;
            }
        } catch (IllegalArgumentException var4) {
            throw new ODataHandlerException("Invalid HTTP method" + httpRequest.getMethod(),
                    var4,
                    org.apache.olingo.server.core.ODataHandlerException.MessageKeys.INVALID_HTTP_METHOD,
                    new String[]{httpRequest.getMethod()});
        }
    }

    static void fillUriInformation(ODataRequest odRequest,
                                   HttpServletRequest httpRequest,
                                   int split) {
        String rawRequestUri = httpRequest.getRequestURL().toString();
        String rawServiceResolutionUri = null;
        String rawODataPath;
        String rawBaseUri;
        int index;
        int end;
        if (httpRequest.getAttribute("requestMapping") != null) {
            rawBaseUri = httpRequest.getAttribute("requestMapping").toString();
            rawServiceResolutionUri = rawBaseUri;
            index = rawRequestUri.indexOf(rawBaseUri) + rawBaseUri.length();
            rawODataPath = rawRequestUri.substring(index);
        } else if (!"".equals(httpRequest.getServletPath())) {
            end = rawRequestUri.indexOf(httpRequest.getServletPath()) + httpRequest.getServletPath()
                    .length();
            rawODataPath = rawRequestUri.substring(end);
        } else if (!"".equals(httpRequest.getContextPath())) {
            end = rawRequestUri.indexOf(httpRequest.getContextPath()) + httpRequest.getContextPath()
                    .length();
            rawODataPath = rawRequestUri.substring(end);
        } else {
            rawODataPath = httpRequest.getRequestURI();
        }

        if (split > 0) {
            rawServiceResolutionUri = rawODataPath;

            for (end = 0; end < split; ++end) {
                index = rawODataPath.indexOf(47, 1);
                if (-1 == index) {
                    rawODataPath = "";
                    break;
                }

                rawODataPath = rawODataPath.substring(index);
            }

            end = rawODataPath.length() - rawODataPath.length();
            rawServiceResolutionUri = rawServiceResolutionUri.substring(0, end);
        }

        rawBaseUri = rawRequestUri.substring(0, rawRequestUri.length() - rawODataPath.length());
        odRequest.setRawQueryPath(httpRequest.getQueryString());
        odRequest.setRawRequestUri(rawRequestUri + (httpRequest.getQueryString() == null ? "" : "?" + httpRequest.getQueryString()));
        odRequest.setRawODataPath(rawODataPath);
        odRequest.setRawBaseUri(rawBaseUri);
        odRequest.setRawServiceResolutionUri(rawServiceResolutionUri);
    }

    static void copyHeaders(ODataRequest odRequest, HttpServletRequest req) {
        Enumeration<?> headerNames = req.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            List<String> headerValues = Collections.list(req.getHeaders(headerName));
            odRequest.addHeader(headerName, headerValues);
        }

    }

    public void register(Processor processor) {
        this.handler.register(processor);
    }

    public void register(OlingoExtension extension) {
        this.handler.register(extension);
    }

    public void register(CustomContentTypeSupport customContentTypeSupport) {
        this.handler.register(customContentTypeSupport);
    }

    public void register(CustomETagSupport customConcurrencyControlSupport) {
        this.handler.register(customConcurrencyControlSupport);
    }

    public void register(DebugSupport debugSupport) {
        this.debugger.setDebugSupportProcessor(debugSupport);
    }
}
