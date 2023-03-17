package net.moewes.quarkus.odata.runtime;

import net.moewes.quarkus.odata.EntityCollectionProvider;
import net.moewes.quarkus.odata.repository.Callable;
import net.moewes.quarkus.odata.repository.EntitySet;
import net.moewes.quarkus.odata.runtime.edm.EdmRepository;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class QuarkusEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private final EdmRepository repository;
    private final ODataEntityConverter odataEntityConverter;

    public QuarkusEntityCollectionProcessor(EdmRepository repository) {
        this.repository = repository;
        odataEntityConverter = new ODataEntityConverter(repository);
    }

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntityCollection(ODataRequest oDataRequest,
                                     ODataResponse oDataResponse,
                                     UriInfo uriInfo,
                                     ContentType contentType)
            throws ODataApplicationException, ODataLibraryException {

        ODataRequestContext context = new ODataRequestContext(odata, oDataRequest,
                oDataResponse, uriInfo);

        // Refactor

        FilterOption filterOption = uriInfo.getFilterOption();
        if (filterOption != null) {
            Expression filterExpression = filterOption.getExpression();

            DraftFilterExpressionVisitor draftVisitor = new DraftFilterExpressionVisitor();
            try {
                Object accept = filterExpression.accept(draftVisitor);
            } catch (ExpressionVisitException e) {
                throw new RuntimeException(e);
            }
        }
        // Refactor end

        EntityCollection entityCollection;

        if (context.isEntitySet()) {
            entityCollection = getData(context);
        } else if (context.isNavigation()) {
            EdmNavigationProperty navigationProperty = context.getNavigationProperty();

            ODataRequestContext parentContext = context.getParentContext();

            entityCollection = getNavigationData(parentContext.getEntitySet(),
                    parentContext.getKeyPredicates(),
                    navigationProperty, context.getEntitySet());
        } else {
            throw new ODataApplicationException("Not supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
                    Locale.ENGLISH);
        }

        // Refactor begin
        //FilterOption filterOption = uriInfo.getFilterOption();
        /*
        if (filterOption != null) {
            Expression filterExpression = filterOption.getExpression();

            Iterator<Entity> entityIterator = entityCollection.getEntities().iterator();

            while (entityIterator.hasNext()) {
                Entity entity = entityIterator.next();
                FilterExpressionVisitor visitor = new FilterExpressionVisitor(entity);
                Object visitorResult = null;
                try {
                    visitorResult = filterExpression.accept(visitor);
                } catch (ExpressionVisitException e) {
                    throw new RuntimeException(e);
                }
                if (visitorResult instanceof Boolean) {
                    if (Boolean.FALSE.equals(visitorResult)) {
                        entityIterator.remove();
                    }
                }
            }
        }

         */
        // Refactor end
        context.respondWithEntityCollection(entityCollection,
                contentType,
                HttpStatusCode.OK,
                serviceMetadata);
    }

    private EntityCollection getData(ODataRequestContext context) throws ODataApplicationException {

        EntityCollection collection = new EntityCollection();

        EntitySet entitySet =
                repository.findEntitySet(context.getEntitySet().getName()).orElseThrow();

        ServiceBean serviceBean1 = new ServiceBean(entitySet);

        Collection<?> dataCollection = serviceBean1.getCollection(context);

        if (dataCollection != null) {
            dataCollection.forEach(data -> {
                Entity entity = new Entity();
                odataEntityConverter.convertDataToFrameworkEntity(entity,
                        repository.findEntityType(entitySet.getEntityType()).orElseThrow(),
                        data);

                // Refactor
                if (context.hasExpandedEntities()) {

                    context.getExpandItems().forEach(expandItem -> {
                        UriResource
                                uriResource =
                                expandItem.getResourcePath().getUriResourceParts().get(0);
                        if (uriResource instanceof UriResourceNavigation) {
                            EdmNavigationProperty edmNavigationProperty =
                                    ((UriResourceNavigation) uriResource).getProperty();

                            Entity expandEntity = new Entity();

                            String entityTypeName = edmNavigationProperty.getType().getName();

                            Callable action = entitySet.getNavigationBindings()
                                    .stream()
                                    .filter(action1 -> action1.getReturnType()
                                            .getEntityType()
                                            .equals(entityTypeName))
                                    .findFirst()
                                    .orElseThrow();

                            Object result2 = serviceBean1.call(action, data, new HashMap<>());

                            if (result2 != null) {
                                odataEntityConverter.convertDataToFrameworkEntity(expandEntity,
                                        repository.findEntityType(action.getReturnType()
                                                        .getEntityType())
                                                .orElseThrow(),
                                        result2);


                                Link link = new Link();
                                link.setTitle(edmNavigationProperty.getName());
                                link.setInlineEntity(expandEntity);
                                entity.getNavigationLinks().add(link);
                            }
                        }
                    });
                }
                // Refactor

                collection.getEntities().add(entity);
            });
        }
        return collection;
    }

    private EntityCollection getNavigationData(EdmEntitySet parentEntitySet,
                                               List<UriParameter> keyPredicates,
                                               EdmNavigationProperty navigationProperty,
                                               EdmEntitySet edmEntitySet) {

        EntityCollection collection = new EntityCollection();

        repository.findEntitySet(parentEntitySet.getName()).ifPresent(entitySet -> {
            Object serviceBean = repository.getServiceBean(entitySet);

            if (serviceBean instanceof EntityCollectionProvider<?>) {

                Map<String, String> keys = new HashMap<>();
                odataEntityConverter.convertKeysToAppFormat(keyPredicates, entitySet, keys);
                ((EntityCollectionProvider<?>) serviceBean).find(keys).ifPresent(data -> {
                    try {
                        String entityTypeName = navigationProperty.getType().getName();

                        Callable action = entitySet.getNavigationBindings()
                                .stream()
                                .filter(action1 -> action1.getReturnType()
                                        .getEntityType()
                                        .equals(entityTypeName))
                                .findFirst().orElseThrow(() -> new ODataApplicationException("Can" +
                                        "'t find NavigationBinding",
                                        HttpStatusCode.BAD_REQUEST.getStatusCode(),
                                        Locale.ENGLISH));

                        List<Class<?>> parameterClasses = new ArrayList<>();
                        action.getParameter().forEach(parameter -> {
                            try {
                                Class<?> aClass = Class.forName(parameter.getTypeName(), true,
                                        Thread.currentThread().getContextClassLoader());
                                parameterClasses.add(aClass);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        });
                        Method declaredMethod =
                                serviceBean.getClass().getDeclaredMethod("get" + action.getName(),
                                        parameterClasses.toArray(Class[]::new)); // FIXME

                        List<Object> valueList = new ArrayList<>();
                        valueList.add(data);

                        Object result = declaredMethod.invoke(serviceBean, valueList.toArray());

                        EntitySet effectiveEntitySet =
                                repository.findEntitySet(edmEntitySet.getName())
                                        .orElseThrow(() -> new ODataRuntimeException(
                                                "Can't find Entityset"));
                        if (result instanceof Collection) {
                            ((Collection<?>) result).forEach(item -> {
                                Entity entity = new Entity();
                                odataEntityConverter.convertDataToFrameworkEntity(entity,
                                        repository.findEntityType(effectiveEntitySet.getEntityType())
                                                .orElseThrow(),
                                        item);

                                collection.getEntities().add(entity);
                            });
                        }
                    } catch (IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException | ODataApplicationException e) {
                        e.printStackTrace(); // FIXME
                    }
                });
            }
        });
        return collection;
    }
}
