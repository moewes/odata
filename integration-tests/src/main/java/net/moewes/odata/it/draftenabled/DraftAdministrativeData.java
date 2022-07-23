package net.moewes.odata.it.draftenabled;

import lombok.Data;
import net.moewes.quarkus.odata.annotations.EntityKey;
import net.moewes.quarkus.odata.annotations.ODataEntity;

import java.util.UUID;

@ODataEntity("DraftAdministrativeData")
@Data
public class DraftAdministrativeData {

    @EntityKey
    private UUID draftUUID;
    // CreationDateTime Type="Edm.DateTimeOffset" Precision="7"/>
    private String createdByUser;
    private boolean draftIsCreatedByMe;
    // LastChangeDataTime  Type="Edm.DateTimeOffset" Precision="7"/>
    private String lastChangeByUser;
    private String inProcessByUser;
    private boolean draftIsProcessedByMe;
}
