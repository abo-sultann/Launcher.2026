package com.launchercar.maps;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class MapPackVerifier {
    private static final byte ZOOM = 14;
    private static final int TILE_SIZE = 256;

    private static final City[] CITIES = {
            new City("Riyadh", 24.7136, 46.6753),
            new City("Jeddah", 21.5433, 39.1728),
            new City("Makkah", 21.3891, 39.8579),
            new City("Madinah", 24.5247, 39.5692),
            new City("Dammam", 26.4207, 50.0888),
            new City("Tabuk", 28.3838, 36.5550),
            new City("Abha", 18.2465, 42.5117)
    };

    private MapPackVerifier() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: MapPackVerifier <map-file> <report-json>");
        }

        File mapPath = new File(args[0]);
        Path reportPath = Path.of(args[1]);
        int nonEmptyCityGrids = 0;
        int tilesRead = 0;
        int namedObjects = 0;
        int arabicNames = 0;

        MapFile mapFile = new MapFile(mapPath, "ar");
        try {
            MapFileInfo info = mapFile.getMapFileInfo();
            BoundingBox bounds = info.boundingBox;

            require(info.fileSize == mapPath.length(), "Map header file size mismatch");
            require(info.fileVersion >= 4 && info.fileVersion <= 5,
                    "Unsupported map format version: " + info.fileVersion);
            require(info.languagesPreference != null
                            && info.languagesPreference.contains("ar")
                            && info.languagesPreference.contains("en"),
                    "Arabic/English language metadata is missing");

            for (City city : CITIES) {
                require(bounds.contains(city.latitude, city.longitude),
                        city.name + " is outside map bounds");

                int centerX = MercatorProjection.longitudeToTileX(city.longitude, ZOOM);
                int centerY = MercatorProjection.latitudeToTileY(city.latitude, ZOOM);
                int cityObjects = 0;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        Tile tile = new Tile(centerX + dx, centerY + dy, ZOOM, TILE_SIZE);
                        MapReadResult result = mapFile.readMapData(tile);
                        require(result != null, "Null tile result near " + city.name + ": " + tile);
                        tilesRead++;
                        cityObjects += result.pois.size() + result.ways.size();

                        MapReadResult named = mapFile.readNamedItems(tile);
                        require(named != null, "Null named tile result near " + city.name + ": " + tile);
                        namedObjects += named.pois.size() + named.ways.size();
                        arabicNames += countArabicNames(named);
                    }
                }

                if (cityObjects > 0) {
                    nonEmptyCityGrids++;
                }
                System.out.printf(Locale.ROOT, "%s: %,d map objects%n", city.name, cityObjects);
            }

            require(nonEmptyCityGrids == CITIES.length,
                    "One or more major-city tile grids contained no map data");
            require(namedObjects > 100, "Insufficient named map objects: " + namedObjects);
            require(arabicNames > 20, "Insufficient Arabic labels: " + arabicNames);

            String json = String.format(Locale.ROOT,
                    "{\n" +
                            "  \"readerVersion\": \"0.25.0\",\n" +
                            "  \"mapFormatVersion\": %d,\n" +
                            "  \"languages\": \"%s\",\n" +
                            "  \"bounds\": \"%s\",\n" +
                            "  \"citiesChecked\": %d,\n" +
                            "  \"tilesRead\": %d,\n" +
                            "  \"namedObjects\": %d,\n" +
                            "  \"arabicNames\": %d,\n" +
                            "  \"result\": \"pass\"\n" +
                            "}\n",
                    info.fileVersion,
                    escape(info.languagesPreference),
                    escape(bounds.toString()),
                    CITIES.length,
                    tilesRead,
                    namedObjects,
                    arabicNames);

            Files.createDirectories(reportPath.getParent());
            Files.writeString(reportPath, json, StandardCharsets.UTF_8);
            System.out.println(json);
        } finally {
            mapFile.close();
        }
    }

    private static int countArabicNames(MapReadResult result) {
        int count = 0;
        for (PointOfInterest poi : result.pois) {
            count += countArabicNameTags(poi.tags);
        }
        for (Way way : result.ways) {
            count += countArabicNameTags(way.tags);
        }
        return count;
    }

    private static int countArabicNameTags(Iterable<Tag> tags) {
        int count = 0;
        for (Tag tag : tags) {
            if ("name".equals(tag.key) && containsArabic(tag.value)) {
                count++;
            }
        }
        return count;
    }

    private static boolean containsArabic(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(value.charAt(i));
            if (block == Character.UnicodeBlock.ARABIC
                    || block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A
                    || block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B) {
                return true;
            }
        }
        return false;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class City {
        private final String name;
        private final double latitude;
        private final double longitude;

        private City(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
