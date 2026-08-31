/*
 * Copyright 2025 cpesch
 * Copyright 2025 moving-bits
 * Licensed under the GNU Lesser General Public License v3 or later.
 * Source: Mapsforge 0.26.1.
 */
package org.mapsforge.map.android.mbtiles;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;

import java.io.InputStream;
import java.util.logging.Logger;

public class MBTilesRenderer {
    private static final Logger LOGGER = Logger.getLogger(MBTilesRenderer.class.getName());

    private final MBTilesFile file;
    private final GraphicFactory graphicFactory;
    private final long timestamp = System.currentTimeMillis();

    MBTilesRenderer(final MBTilesFile file, final GraphicFactory graphicFactory) {
        this.file = file;
        this.graphicFactory = graphicFactory;
    }

    public TileBitmap executeJob(final MBTilesRendererJob rendererJob) {
        try (InputStream inputStream = file.getTileAsBytes(
                rendererJob.tile.tileX, rendererJob.tile.tileY, rendererJob.tile.zoomLevel)) {
            final TileBitmap bitmap;
            if (inputStream == null) {
                bitmap = graphicFactory.createTileBitmap(rendererJob.tile.tileSize, rendererJob.hasAlpha);
            } else {
                bitmap = graphicFactory.createTileBitmap(inputStream, rendererJob.tile.tileSize, rendererJob.hasAlpha);
                bitmap.scaleTo(rendererJob.tile.tileSize, rendererJob.tile.tileSize);
            }
            bitmap.setTimestamp(getDataTimestamp(rendererJob.tile));
            return bitmap;
        } catch (Exception e) {
            LOGGER.warning("Error while rendering MBTiles job " + rendererJob + ": " + e.getMessage());
            return null;
        }
    }

    public long getDataTimestamp(final Tile tile) {
        return timestamp;
    }

    MBTilesFile getMBTilesFile() {
        return file;
    }
}
