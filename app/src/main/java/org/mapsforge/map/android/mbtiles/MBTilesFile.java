/*
 * Copyright 2025 cpesch
 * Copyright 2025 moving-bits
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * Source: Mapsforge 0.26.1. Vendored so Launcher can retain the proven 0.25.0 vector
 * renderer while adding the official raster MBTiles reader on older Android devices.
 */
package org.mapsforge.map.android.mbtiles;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import org.mapsforge.core.model.BoundingBox;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MBTilesFile {
    protected final SQLiteDatabase database;
    private Map<String, String> metadata;

    private static final String SELECT_METADATA = "SELECT name, value FROM metadata";
    private static final String SELECT_TILES = "SELECT tile_data FROM tiles WHERE zoom_level=%s AND tile_column=%s AND tile_row=%s ORDER BY zoom_level DESC LIMIT 1";
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("png", "jpg", "jpeg");

    public MBTilesFile(final File file) {
        this.database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        final String format = getFormat();
        if (format == null) {
            close();
            throw new IllegalArgumentException("metadata.format is missing");
        }
        if (!SUPPORTED_FORMATS.contains(format.toLowerCase())) {
            close();
            throw new IllegalArgumentException("Unsupported MBTiles raster format: " + format);
        }
    }

    public void close() {
        try {
            metadata = null;
            if (database.isOpen()) database.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BoundingBox getBoundingBox() {
        final String bounds = getMetadata().get("bounds");
        if (bounds == null) return null;
        final String[] split = bounds.split(",");
        if (split.length != 4) return null;
        try {
            return new BoundingBox(
                    Double.parseDouble(split[1]), Double.parseDouble(split[0]),
                    Double.parseDouble(split[3]), Double.parseDouble(split[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String getFormat() {
        return getMetadata().get("format");
    }

    private Integer getMetadataInt(final String name) {
        final String value = getMetadata().get(name);
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
            try (Cursor cursor = database.rawQuery(SELECT_METADATA, null)) {
                while (cursor.moveToNext()) {
                    metadata.put(cursor.getString(0), cursor.getString(1));
                }
            }
        }
        return metadata;
    }

    private Integer getZoomLevel(final String name, final String function) {
        if (getMetadataInt(name) == null) {
            final String result = getSingleValue("SELECT " + function + "(zoom_level) AS value FROM tiles");
            if (result != null) getMetadata().put(name, result);
        }
        return getMetadataInt(name);
    }

    public int getZoomLevelMax() {
        final Integer value = getZoomLevel("maxzoom", "MAX");
        return value == null ? 20 : value;
    }

    public int getZoomLevelMin() {
        final Integer value = getZoomLevel("minzoom", "MIN");
        return value == null ? 0 : value;
    }

    private String getSingleValue(final String query) {
        try (Cursor cursor = database.rawQuery(query, null)) {
            if (cursor.moveToNext()) {
                final int index = cursor.getColumnIndex("value");
                return index < 0 ? null : cursor.getString(index);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public InputStream getTileAsBytes(final int tileX, final int tileY, final byte zoomLevel) {
        final long tmsTileY = (1L << zoomLevel) - tileY - 1L;
        try (Cursor cursor = database.rawQuery(String.format(SELECT_TILES, zoomLevel, tileX, tmsTileY), null)) {
            if (cursor.moveToNext()) {
                final int index = cursor.getColumnIndex("tile_data");
                return index < 0 ? null : new ByteArrayInputStream(cursor.getBlob(index));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
