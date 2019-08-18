package space.eightnoteight.elasticsearch.plugins;

import org.elasticsearch.cluster.routing.allocation.decider.AllocationDecider;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.ClusterPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ShardPlacementAllocationDeciderPlugin extends Plugin implements ClusterPlugin {
    @Override
    public Collection<AllocationDecider> createAllocationDeciders(Settings settings, ClusterSettings clusterSettings) {
        return new ArrayList<AllocationDecider>() {{
            add(new ShardPlacementAllocationDecider(settings));
        }};
    }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> settings = new ArrayList<>();
        settings.add(new Setting<>(
                ShardPlacementAllocationDecider.PLACEMENT_MAP_SETTINGS_KEY,
                "", Function.identity(), Setting.Property.IndexScope, Setting.Property.Dynamic
        ));
        return settings;
    }
}
