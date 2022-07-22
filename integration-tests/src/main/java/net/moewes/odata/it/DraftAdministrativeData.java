package net.moewes.odata.it;

import lombok.Data;
import net.moewes.quarkus.odata.annotations.ODataEntity;

@ODataEntity("DraftAdministrativeData")
@Data
public class DraftAdministrativeData {

    private String draftUUID; // TODO UUID

    /*
    <Key>
<PropertyRef Name="DraftUUID"/>
</Key>
<Property Name="DraftUUID" Type="Edm.Guid" Nullable="false"/>
<Property Name="CreationDateTime" Type="Edm.DateTimeOffset" Precision="7"/>
<Property Name="CreatedByUser" Type="Edm.String" MaxLength="256"/>
<Property Name="DraftIsCreatedByMe" Type="Edm.Boolean"/>
<Property Name="LastChangeDateTime" Type="Edm.DateTimeOffset" Precision="7"/>
<Property Name="LastChangedByUser" Type="Edm.String" MaxLength="256"/>
<Property Name="InProcessByUser" Type="Edm.String" MaxLength="256"/>
<Property Name="DraftIsProcessedByMe" Type="Edm.Boolean"/>
     */
}
