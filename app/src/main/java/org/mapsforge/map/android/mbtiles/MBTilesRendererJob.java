/*
 * Copyright 2025 cpesch
 * Copyright 2025 moving-bits
 * Licensed under the GNU Lesser General Public License v3 or later.
 * Source: Mapsforge 0.26.1.
 */
package org.mapsforge.map.android.mbtiles;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;

public class MBTilesRendererJob extends Job {
    private final MBTilesRenderer renderer;
    private final int hashCodeValue;

    public MBTilesRendererJob(final Tile tile, final MBTilesRenderer renderer, final boolean isTransparent) {
        super(tile, isTransparent);
        this.renderer = renderer;
        this.hashCodeValue = calculateHashCode();
    }

    public MBTilesRenderer getDatabaseRenderer() {
        return renderer;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj) || !(obj instanceof MBTilesRendererJob)) return false;
        return renderer.equals(((MBTilesRendererJob) obj).renderer);
    }

    @Override
    public int hashCode() {
        return hashCodeValue;
    }

    private int calculateHashCode() {
        return 31 * super.hashCode() + renderer.hashCode();
    }
}
