# Quarkus OData V4 Extension

Quarkus Odata V4 Extension

## How to use

Check out the repository and build and install with maven or use GitHub packages.

Maven coordinates:

```
  <dependency>
     <groupId>net.moewes</groupId>
     <artifactId>quarkus-odata</artifactId>
     <version>${version}</version>
  </dependency>
```

### Create OData EntityType

Simply annotate a pojo class with the @ODataEntity annotation.

````
@ODataEntity("BasicEntity")
@Data
public class MyBasicEntity {

    @EntityKey
    private String id;

    private int number;
    private boolean flag;
    private String text;
}
````

In this example also Lombok is used to generate getters and setters.
Mark entity keys with @EntityKey annotation.

### Create OData EntitySets

Create a class and annotate it with @ODataEntitySet. Implement at least the interface EntityCollectionProvider<>. For
CRUD operations on a single entity implement the EntityProvider<T> interface.

````
@ODataEntitySet(value = "BasicSet", entityType = "BasicEntity")
public class MyBasicEnttySet
        implements EntityCollectionProvider<MyBasicEntity>, EntityProvider<MyBasicEntity> {
````

EntitySet classes are also managed CDI beans in application scope. So it is possible to inject other CDI beans.

### Navigation

For navigation add a method to your EntitySet class and annotate it with @OdataNavigationBinding. It can either return a
entityType annotated class or a list of such an entityType class. Also add one parameter for the entity.

```
 @ODataNavigationBinding("SiblingEntity")
 public MyEntity getSiblingEntity(MyEntity entity)
```

### Bound Actions

Bounded actions can be created in a similar way like navigation. Annotate the method with @ODataAction. Return type can
be a entityType class, a primitive type or a list of them (see supported data types). The first parameter must be the
bound entity, further parameter are possible (primitive types only, see supported data types)..

````
 @ODataAction
 public String myAction(MyEntity entity, String parameter)
````

### Supported Data Types

| Type in Pojo | Mapped Edm Type |
|--------------|-----------------|
| String       | Edm.String      |
| int          | Edm.Int32       |
| boolean      | Edm.Boolean     |
| UUID         | Edm.Guid        | 

### Other Features

Batch operations are supported.

### Known Limitations

* query options like filters are not supported
* Functions are not working correct
* Unbound Actions are not supported
* Data type not mentioned in supported data types

## Changelog

### 0.2.0-SNAPSHOT

* Based on Quarkus 2.11
* Updated Apache Olingo to 2.9
* Renamed @ODataService to @ODataEntitySet
* support int values
* support boolean values
* support UUID values

### 0.1.0

* Initial version
* Based on Quarkus 2.10