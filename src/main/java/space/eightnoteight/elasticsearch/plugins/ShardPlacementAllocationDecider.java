package space.eightnoteight.elasticsearch.plugins;

import joptsimple.internal.Strings;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDecider;
import org.elasticsearch.cluster.routing.allocation.decider.Decision;
import org.elasticsearch.common.settings.Settings;

import java.util.*;

public class ShardPlacementAllocationDecider extends AllocationDecider {

    private static final String NAME = ShardPlacementAllocationDecider.class.getName();

    private static final String PLUGIN_SETTINGS_PREFIX = "index.space.eightnoteight.elasticsearch.plugins.shard_placement_allocation_decider";

    static final String PLACEMENT_MAP_SETTINGS_KEY = Strings.join(
            new String[] {PLUGIN_SETTINGS_PREFIX, "placement"}, "."
    );

    /**
     * Initializes a new {@link AllocationDecider}
     *
     * @param settings {@link Settings} used by this {@link AllocationDecider}
     */
    protected ShardPlacementAllocationDecider(Settings settings) {
        super(settings);
    }


    @Override
    public Decision canRemain(ShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        return canShardAllocateHelper(shardRouting, node, allocation);
    }


    @Override
    public Decision canAllocate(ShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        return canShardAllocateHelper(shardRouting, node, allocation);
    }

    private Decision canShardAllocateHelper(ShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        int shardId = shardRouting.shardId().getId();
        IndexMetaData indexMetaData = allocation.metaData().getIndexSafe(shardRouting.index());
        Settings indexSettings = indexMetaData.getSettings();
        String[] shardPlacementSettings = org.elasticsearch.common.Strings.tokenizeToStringArray(indexSettings.get(PLACEMENT_MAP_SETTINGS_KEY), ";");
        if (shardPlacementSettings.length == 0) {
            logger.debug(
                    "no settings were configured for this index", shardRouting.shardId()
            );
            return super.canAllocate(shardRouting, node, allocation);
        }

        if (shardPlacementSettings.length != indexMetaData.getNumberOfShards()) {
            logger.error(
                    "mis-configured settings, insufficient number of shards placement settings available for this index",
                    (Object) shardPlacementSettings
            );
            return allocation.decision(
                    Decision.NO, NAME,
                    "mis-configured settings, insufficient number of shards placement settings available for this index"
            );
        }

        Set<String> nodeNames = org.elasticsearch.common.Strings.tokenizeByCommaToSet(shardPlacementSettings[shardId]);
        if (nodeNames.size() != indexMetaData.getNumberOfReplicas() + 1) {
            logger.error(
                    "mis-configured settings, insufficient number of nodes available for this shard and its replicas",
                    nodeNames
            );
            return allocation.decision(
                    Decision.NO, NAME,
                    "mis-configured settings, insufficient number of nodes available for this shard and its replicas"
            );
        }

        if (!nodeNames.contains(node.node().getName())) {
            logger.error(
                    "mis-configured settings, insufficient number of nodes available for this shard and its replicas",
                    nodeNames,
                    node.node().getName()
            );
            return allocation.decision(
                    Decision.NO, NAME,
                    String.format(
                            "node is not one of the configured nodes for this shard (%s)",
                            Strings.join(nodeNames.toArray(new String[0]), ", ")
                    )
            );
        }

        return allocation.decision(Decision.YES, NAME, "index placement settings constraints passed");
    }
}
