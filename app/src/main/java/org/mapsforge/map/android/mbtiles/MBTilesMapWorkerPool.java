/*
 * Copyright 2025 cpesch
 * Copyright 2025 moving-bits
 * Licensed under the GNU Lesser General Public License v3 or later.
 * Source: Mapsforge 0.26.1.
 */
package org.mapsforge.map.android.mbtiles;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class MBTilesMapWorkerPool implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(MBTilesMapWorkerPool.class.getName());

    private final MBTilesRenderer renderer;
    private final JobQueue<MBTilesRendererJob> jobQueue;
    private final Layer layer;
    private final TileCache tileCache;
    private boolean inShutdown;
    private boolean isRunning;
    private ExecutorService self;
    private ExecutorService workers;

    MBTilesMapWorkerPool(final TileCache tileCache, final JobQueue<MBTilesRendererJob> jobQueue,
                         final MBTilesRenderer renderer, final TileMBTilesLayer layer) {
        this.tileCache = tileCache;
        this.jobQueue = jobQueue;
        this.renderer = renderer;
        this.layer = layer;
    }

    @Override
    public void run() {
        try {
            while (!inShutdown) {
                final MBTilesRendererJob job = jobQueue.get(Parameters.NUMBER_OF_THREADS);
                if (job == null) continue;
                if (!tileCache.containsKey(job)) workers.execute(new MapWorker(job));
                else jobQueue.remove(job);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException e) {
            if (!inShutdown) LOGGER.log(Level.WARNING, "MBTiles worker rejected", e);
        }
    }

    synchronized void start() {
        if (isRunning) return;
        inShutdown = false;
        self = Executors.newSingleThreadExecutor();
        // The target head unit has 1 GB RAM. Two tile workers prevent memory spikes.
        workers = Executors.newFixedThreadPool(Math.min(2, Parameters.NUMBER_OF_THREADS));
        self.execute(this);
        isRunning = true;
    }

    synchronized void stop() {
        if (!isRunning) return;
        inShutdown = true;
        jobQueue.interrupt();
        self.shutdown();
        workers.shutdown();
        stopExecutor(self);
        stopExecutor(workers);
        isRunning = false;
    }

    private static void stopExecutor(ExecutorService executor) {
        try {
            if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private final class MapWorker implements Runnable {
        private final MBTilesRendererJob rendererJob;

        MapWorker(final MBTilesRendererJob rendererJob) {
            this.rendererJob = rendererJob;
        }

        @Override
        public void run() {
            TileBitmap bitmap = null;
            try {
                if (inShutdown) return;
                bitmap = renderer.executeJob(rendererJob);
                if (inShutdown) return;
                if (bitmap != null) tileCache.put(rendererJob, bitmap);
                layer.requestRedraw();
            } finally {
                jobQueue.remove(rendererJob);
                if (bitmap != null) bitmap.decrementRefCount();
            }
        }
    }
}
