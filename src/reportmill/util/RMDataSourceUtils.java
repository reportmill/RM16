package reportmill.util;
import reportmill.shape.*;
import rmdraw.scene.SGView;

/**
 * Utilities related to datasource.
 */
public class RMDataSourceUtils {

    /**
     * Returns the entity given shape should show in keys browser.
     */
    public static Entity getDatasetEntity(SGView aShape)
    {
        // Handle RMCrossTabFrame
        if (aShape instanceof RMCrossTabFrame) {
            RMCrossTabFrame ctf = (RMCrossTabFrame) aShape;
            RMCrossTab ctab = ctf.getTable();
            Entity entity = getParentEntity(aShape);
            return getDatasetEntityForShapeAndParentEntity(ctab, entity);
        }

        // Handle RMTableGroup
        if (aShape instanceof RMTableGroup) {
            RMTableGroup tableGroup = (RMTableGroup) aShape;
            RMTable table = tableGroup.getMainTable();
            Entity entity = getParentEntity(aShape);
            return table!=null ? getDatasetEntityForShapeAndParentEntity(table, entity) : entity;
        }

        // Get parent entity (just return null, if null)
        Entity parEntity = getParentEntity(aShape); if(parEntity==null) return null;
        return getDatasetEntityForShapeAndParentEntity(aShape, parEntity);
    }

    /**
     * Returns the parent entity for a shape.
     */
    private static Entity getParentEntity(SGView aShape)
    {
        // Handle RMDoc
        if (aShape instanceof RMDoc) { RMDoc doc = (RMDoc)aShape;
            RMDataSource dataSource = doc.getDataSource();
            Schema schema = dataSource!=null ? dataSource.getSchema() : null;
            Entity entity = schema!=null ? schema.getRootEntity() : null;
            return entity;
        }

        // Try parent
        SGView par = aShape.getParent();
        return par!=null ? getParentEntity(par) : null;
    }

    /**
     * Returns the entity given shape should show in keys browser.
     */
    private static Entity getDatasetEntityForShapeAndParentEntity(SGView aShape, Entity anEntity)
    {
        // If no parent entity, return null
        if (anEntity==null) return null;

        // Get Property for Shape.DatasetKey
        String dsetKey = aShape.getDatasetKey();
        Property prop = dsetKey!=null ? anEntity.getKeyPathProperty(dsetKey) : null;

        // Get Relation entity for property
        Entity entity = prop!=null && prop.isRelation()? prop.getRelationEntity() : null;
        return entity!=null? entity : anEntity;
    }

}
