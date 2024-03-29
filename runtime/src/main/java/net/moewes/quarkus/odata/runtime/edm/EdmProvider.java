package net.moewes.quarkus.odata.runtime.edm;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlPropertyValue;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class EdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "Quarkus.OData";

    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER =
            new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    private final EdmRepository repository;
    private final CsdlBuilder csdlBuilder;

    public EdmProvider(EdmRepository repository, CsdlBuilder csdlBuilder) {
        super();
        this.repository = repository;
        this.csdlBuilder = csdlBuilder;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {

        return csdlBuilder.findCsdlForEntityType(entityTypeName).orElse(null);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {

        return csdlBuilder.findCsdlForEntitySet(entitySetName).orElse(null);
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {

        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() {

        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        List<CsdlEntityType> entityTypes = new ArrayList<>();
        for (String item : repository.getEntityTypeNames()) {
            entityTypes.add(getEntityType(new FullQualifiedName(NAMESPACE, item)));
        }
        schema.setEntityTypes(entityTypes);
        List<CsdlAction> actions = new ArrayList<>();
        for (String action : repository.getActionNames()) {
            actions.addAll(getActions(new FullQualifiedName(NAMESPACE, action)));
        }
        schema.setActions(actions);
        List<CsdlFunction> functions = new ArrayList<>();
        for (String function : repository.getFunctionNames()) {
            functions.addAll(getFunctions(new FullQualifiedName(NAMESPACE, function)));
        }
        schema.setFunctions(functions);

        schema.setEntityContainer(getEntityContainer());

        List<CsdlSchema> schemas = new ArrayList<>();
        schemas.add(schema);
        return schemas;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {

        List<CsdlEntitySet> entitySets = new ArrayList<>();

        for (String item : repository.getEntitySetNames()) {
            entitySets.add(getEntitySet(CONTAINER, item));
        }

        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    @Override
    public List<CsdlAction> getActions(FullQualifiedName actionName) {

        List<CsdlAction> result = new ArrayList<>();
        csdlBuilder.findCsdlForAction(actionName).ifPresent(result::add);
        return result;
    }

    @Override
    public List<CsdlFunction> getFunctions(FullQualifiedName functionName) {

        List<CsdlFunction> result = new ArrayList<>();
        csdlBuilder.findCsdlForFunction(functionName).ifPresent(result::add);
        return result;
    }

    @Override
    public CsdlTerm getTerm(FullQualifiedName termName) {

        Logger.getLogger("term").info(termName.getFullQualifiedNameAsString());
        CsdlTerm term = new CsdlTerm();
        term.setName(termName.getName());
        CsdlComplexType type = new CsdlComplexType();
        type.setName("InsertRestrictionsType");
        CsdlProperty property = new CsdlProperty();
        property.setName("Insertable")
                .setType(EdmPrimitiveTypeKind.Boolean.toString())
                .setDefaultValue("true");
        type.setProperties(Arrays.asList(property));
        term.setType("x.y.z");

        return term;
    }

    public CsdlAnnotations getAnnotationsGroup(FullQualifiedName targetName, String qualifier) {
        // FIXME
        CsdlAnnotations csdlAnnotations = new CsdlAnnotations();
        csdlAnnotations.setTarget(targetName.getFullQualifiedNameAsString());

        CsdlConstantExpression i =
                new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.String);
        i.setValue("Quarkus.OData.draftEdit");

        CsdlPropertyValue value = new CsdlPropertyValue();
        value.setValue(i);
        value.setProperty("EditAction");

        CsdlRecord r = new CsdlRecord();
        r.setPropertyValues(Arrays.asList(value));

        CsdlAnnotation a = new CsdlAnnotation();
        a.setTerm("Common.DraftRoot");
        a.setExpression(r);

        csdlAnnotations.setAnnotations(Arrays.asList(a));


        return csdlAnnotations;
    }
}
