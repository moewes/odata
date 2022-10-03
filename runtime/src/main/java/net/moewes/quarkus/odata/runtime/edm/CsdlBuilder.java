package net.moewes.quarkus.odata.runtime.edm;

import net.moewes.quarkus.odata.repository.*;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CsdlBuilder {

    @Inject
    EdmRepository edmRepository;
    public static final String NAMESPACE = "Quarkus.OData"; // FIXME

    public Optional<CsdlEntitySet> findCsdlForEntitySet(String entitySetName) {

        EntitySet entitySet = edmRepository.findEntitySet(entitySetName).orElse(null);
        return Optional.ofNullable(entitySet != null ? getCsdlForEntitySet(entitySet) : null);
    }

    public CsdlEntitySet getCsdlForEntitySet(EntitySet entitySet) {

        List<CsdlNavigationPropertyBinding> navigationPropertyBindings = new ArrayList<>();
        entitySet.getNavigationBindings().forEach(navigationBinding -> {
            CsdlNavigationPropertyBinding csdlNavigationPropertyBinding =
                    new CsdlNavigationPropertyBinding();
            csdlNavigationPropertyBinding.setPath(navigationBinding.getName());

            edmRepository.findEntitySetForEntityTypeName(
                    navigationBinding.getReturnType()
                            .getEntityType()).ifPresent(target -> {
                csdlNavigationPropertyBinding.setTarget(target.getName());
                navigationPropertyBindings.add(csdlNavigationPropertyBinding);
            });
        });

        return new CsdlEntitySet()
                .setName(entitySet.getName())
                .setType(new FullQualifiedName(NAMESPACE, entitySet.getEntityType()))
                .setNavigationPropertyBindings(navigationPropertyBindings);
    }

    public Optional<CsdlEntityType> findCsdlForEntityType(FullQualifiedName entityTypeName) {

        CsdlEntityType csdlEntityType = null;

        Optional<EntityType> entityTypeOptional =
                edmRepository.findEntityType(entityTypeName.getName());
        if (entityTypeOptional.isPresent()) {
            EntityType entityType = entityTypeOptional.get();
            List<CsdlProperty> csdlPropertyList = new ArrayList<>();
            entityType.getPropertyMap().values().forEach(property -> {
                CsdlProperty csdlProperty = new CsdlProperty()
                        .setName(property.getName())
                        .setType(property.getEdmType().getFullQualifiedName());
                csdlPropertyList.add(csdlProperty);
            });
            List<CsdlPropertyRef> keys = entityType.getPropertyMap().values().stream()
                    .filter(EntityProperty::isKey)
                    .map(item1 -> new CsdlPropertyRef().setName(item1.getName()))
                    .collect(Collectors.toList());

            csdlEntityType = new CsdlEntityType()
                    .setName(entityType.getName())
                    .setProperties(csdlPropertyList)
                    .setKey(keys);

            List<CsdlNavigationProperty> navigationProperties = new ArrayList<>();
            edmRepository.findEntitySetForEntityTypeName(entityTypeName.getName())
                    .ifPresent(myEntitySet -> myEntitySet.getNavigationBindings()
                            .forEach(action -> {
                                String targetEntityType = action.getReturnType().getEntityType();
                                CsdlNavigationProperty navProp = new CsdlNavigationProperty();
                                navProp.setName(action.getName())
                                        .setCollection(action.getReturnType().isCollection())
                                        .setType(new FullQualifiedName(NAMESPACE,
                                                targetEntityType));
                                navigationProperties.add(navProp);
                            }));
            csdlEntityType.setNavigationProperties(navigationProperties);
        }
        return Optional.ofNullable(csdlEntityType);
    }

    public Optional<CsdlAction> findCsdlForAction(FullQualifiedName actionName) {

        CsdlAction csdlAction = null;

        Optional<Callable> actionOptional = edmRepository.findAction(actionName.getName());
        if (actionOptional.isPresent()) {
            Callable action = actionOptional.get();

            csdlAction = new CsdlAction()
                    .setName(action.getName());

            List<CsdlParameter> parameters = new ArrayList<>();
            CsdlAction finalCsdlAction = csdlAction;
            action.getParameter().forEach(parameter -> {
                CsdlParameter csdlParameter = new CsdlParameter();

                if (parameter.isBindingParameter()) {
                    csdlParameter.setName(parameter.getName());
                    csdlParameter.setType(new FullQualifiedName(NAMESPACE,
                            parameter.getEntityType()));
                    finalCsdlAction.setBound(true);
                    finalCsdlAction.setEntitySetPath(parameter.getName());
                } else {
                    csdlParameter.setName(parameter.getName());
                    csdlParameter.setType(parameter.getEdmType().getFullQualifiedName());
                }

                csdlParameter.setCollection(false);
                parameters.add(csdlParameter);
            });

            csdlAction.setParameters(parameters);

            Parameter returnType = action.getReturnType();

            FullQualifiedName returnTypeFQN;
            if (returnType.getEdmType() != null) {
                returnTypeFQN = returnType.getEdmType().getFullQualifiedName();
            } else {
                EntityType entityType =
                        edmRepository.findEntityType(returnType.getEntityType())
                                .orElseThrow(() -> new ODataRuntimeException(
                                        "" + returnType.getEntityType() + " is not a EntityType"));
                returnTypeFQN = new FullQualifiedName(NAMESPACE, entityType.getName());
            }
            csdlAction.setReturnType(new CsdlReturnType()
                    .setType(returnTypeFQN)
                    .setCollection(returnType.isCollection()));
        }
        return Optional.ofNullable(csdlAction);
    }

    public Optional<CsdlFunction> findCsdlForFunction(FullQualifiedName functionName) {

        CsdlFunction csdlFunction = null;

        /*
        if (functions.containsKey(functionName.getName())) {
            Function function = functions.get(functionName.getName());

            List<CsdlParameter> parameters = new ArrayList<>();
            // First Parameter bound Entity
            String entitySet = function.getEntitySet();
            EntitySet entitySet1 =
                    findEntitySet(entitySet).orElseThrow(() -> new ODataRuntimeException("FIXME"));
            CsdlParameter es_param = new CsdlParameter();
            es_param.setName(entitySet1.getEntityType());
            es_param.setType(new FullQualifiedName(NAMESPACE, entitySet1.getEntityType()));
            es_param.setCollection(false);
            parameters.add(es_param);

            csdlFunction = new CsdlFunction()
                    .setName(function.getName())
                    .setParameters(parameters)
                    .setBound(true)
                    .setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                            .setCollection(false));
        }
        // TODO implement functions
         */
        return Optional.ofNullable(csdlFunction);
    }
}

