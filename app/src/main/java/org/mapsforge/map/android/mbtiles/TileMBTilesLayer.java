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
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;

public class TileMBTilesLayer extends TileLayer<MBTilesRendererJob> implements Observer {
    private final MBTilesRenderer renderer;
    private MBTilesMapWorkerPool mapWorkerPool;

    public TileMBTilesLayer(final TileCache tileCache, final MapViewPosition mapViewPosition,
                            final boolean isTransparent, final MBTilesFile file,
                            final GraphicFactory graphicFactory) {
        super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);
        this.renderer = new MBTilesRenderer(file, graphicFactory);
    }

    @Override
    public synchronized void setDisplayModel(final DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        if (displayModel != null) {
            if (mapWorkerPool == null) {
                mapWorkerPool = new MBTilesMapWorkerPool(tileCache, jobQueue, renderer, this);
            }
            mapWorkerPool.start();
        } else if (mapWorkerPool != null) {
            mapWorkerPool.stop();
        }
    }

    @Override
    protected MBTilesRendererJob createJob(final Tile tile) {
        return new MBTilesRendererJob(tile, renderer, isTransparent);
    }

    @Override
    protected boolean isTileStale(final Tile tile, final TileBitmap bitmap) {
        return renderer.getDataTimestamp(tile) > bitmap.getTimestamp();
    }

    @Override
    protected void onAdd() {
        if (mapWorkerPool != null) mapWorkerPool.start();
        if (tileCache != null) tileCache.addObserver(this);
        super.onAdd();
    }

    @Override
    protected void onRemove() {
        if (mapWorkerPool != null) mapWorkerPool.stop();
        if (tileCache != null) tileCache.removeObserver(this);
        super.onRemove();
    }

    @Override
    public void onChange() {
        requestRedraw();
    }

    @Override
    public void onDestroy() {
        renderer.getMBTilesFile().close();
        super.onDestroy();
    }
}
