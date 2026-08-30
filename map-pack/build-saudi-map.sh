#!/usr/bin/env bash
set -Eeuo pipefail

readonly MAPSFORGE_VERSION="0.30.0"
readonly MAPSFORGE_CREATOR_COMMIT="053fbdaa7e37c25a6d562fec0a3d57b646e7c79b"
readonly SAUDI_RELATION_ID="307584"
readonly SOURCE_URL="https://download.geofabrik.de/asia/gcc-states-latest.osm.pbf"
readonly SOURCE_MD5_URL="${SOURCE_URL}.md5"
readonly LAND_URL="https://osmdata.openstreetmap.de/download/land-polygons-split-4326.zip"
readonly MAP_WRITER_URL="https://github.com/mapsforge/mapsforge/releases/download/${MAPSFORGE_VERSION}/mapsforge-map-writer-${MAPSFORGE_VERSION}-jar-with-dependencies.jar"
readonly POI_WRITER_URL="https://github.com/mapsforge/mapsforge/releases/download/${MAPSFORGE_VERSION}/mapsforge-poi-writer-${MAPSFORGE_VERSION}-jar-with-dependencies.jar"
readonly MAP_WRITER_SHA256="2ed0f79498916259826e0b6ef132ecc427e27999b26736036358733279825626"
readonly POI_WRITER_SHA256="dd9f0586acb29482e147d4f8c70291e1396dc4b14ee50224bbd2b9ab5105f7c9"

# A small rectangular margin is intentional: it preserves coastlines and border roads.
readonly LEFT="34.30"
readonly BOTTOM="16.20"
readonly RIGHT="55.85"
readonly TOP="32.30"
readonly START_POSITION="24.7136,46.6753"

readonly SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "${SCRIPT_ROOT}/.." && pwd)"
readonly WORK_ROOT="${MAP_BUILD_WORK_ROOT:-${REPOSITORY_ROOT}/.map-build}"
readonly OUTPUT_ROOT="${MAP_BUILD_OUTPUT_ROOT:-${REPOSITORY_ROOT}/map-output}"
readonly TOOL_ROOT="${WORK_ROOT}/tools"
readonly JAVA_TMP_ROOT="${WORK_ROOT}/java-tmp"
readonly OSMOSIS_PLUGIN_ROOT="${HOME}/.openstreetmap/osmosis/plugins"

readonly SOURCE_FILE="${WORK_ROOT}/gcc-states-latest.osm.pbf"
readonly SOURCE_MD5_FILE="${WORK_ROOT}/gcc-states-latest.osm.pbf.md5"
readonly BOUNDARY_FILE="${WORK_ROOT}/saudi-boundary.osm.pbf"
readonly SAUDI_PBF="${WORK_ROOT}/saudi-2026.osm.pbf"
readonly MERGED_PBF="${WORK_ROOT}/saudi-2026-land-sea.osm.pbf"
readonly MAP_FILE="${OUTPUT_ROOT}/Saudi-2026.map"
readonly POI_FILE="${OUTPUT_ROOT}/Saudi-2026.poi"
readonly MAP_REPORT="${OUTPUT_ROOT}/verification/map-reader-0.25.0.json"
readonly POI_REPORT="${OUTPUT_ROOT}/verification/poi-report.txt"

download() {
  local url="$1"
  local output="$2"
  curl --fail --location --retry 5 --retry-delay 4 --show-error \
    --output "${output}" "${url}"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

for command_name in curl jq md5sum mvn ogr2ogr osmosis osmium python3 sha256sum sqlite3 unzip; do
  require_command "${command_name}"
done

mkdir -p "${WORK_ROOT}" "${OUTPUT_ROOT}/verification" "${TOOL_ROOT}" \
  "${JAVA_TMP_ROOT}" "${OSMOSIS_PLUGIN_ROOT}"
export JAVACMD_OPTIONS="${JAVACMD_OPTIONS:--Xmx5200m} -Djava.io.tmpdir=${JAVA_TMP_ROOT}"

echo "[1/9] Installing pinned Mapsforge writer plugins"
map_writer_jar="${OSMOSIS_PLUGIN_ROOT}/mapsforge-map-writer-${MAPSFORGE_VERSION}-jar-with-dependencies.jar"
poi_writer_jar="${OSMOSIS_PLUGIN_ROOT}/mapsforge-poi-writer-${MAPSFORGE_VERSION}-jar-with-dependencies.jar"
download "${MAP_WRITER_URL}" "${map_writer_jar}"
download "${POI_WRITER_URL}" "${poi_writer_jar}"
printf '%s  %s\n' "${MAP_WRITER_SHA256}" "${map_writer_jar}" | sha256sum --check --strict
printf '%s  %s\n' "${POI_WRITER_SHA256}" "${poi_writer_jar}" | sha256sum --check --strict

creator_raw="https://raw.githubusercontent.com/mapsforge/mapsforge-creator/${MAPSFORGE_CREATOR_COMMIT}"
shape2osm="${TOOL_ROOT}/shape2osm.py"
tag_transform="${TOOL_ROOT}/tag-transform.xml"
download "${creator_raw}/shape2osm.py" "${shape2osm}"
download "${creator_raw}/tag-transform.xml" "${tag_transform}"

echo "[2/9] Downloading and validating current GCC OpenStreetMap data"
download "${SOURCE_URL}" "${SOURCE_FILE}"
download "${SOURCE_MD5_URL}" "${SOURCE_MD5_FILE}"
(
  cd "${WORK_ROOT}"
  md5sum --check "$(basename "${SOURCE_MD5_FILE}")"
)
source_timestamp="$(osmium fileinfo --get=header.option.osmosis_replication_timestamp "${SOURCE_FILE}")"
source_sha256="$(sha256sum "${SOURCE_FILE}" | awk '{print $1}')"

echo "[3/9] Extracting the official Saudi Arabia relation and country data"
osmium getid --add-referenced "${SOURCE_FILE}" "r${SAUDI_RELATION_ID}" \
  --output "${BOUNDARY_FILE}" --overwrite
osmium extract \
  --polygon "${BOUNDARY_FILE}" \
  --strategy smart \
  --option types=multipolygon,boundary \
  --set-bounds \
  "${SOURCE_FILE}" \
  --output "${SAUDI_PBF}" \
  --overwrite
osmium fileinfo --extended --no-crc "${SAUDI_PBF}" | tee "${OUTPUT_ROOT}/verification/source-extract.txt"

echo "[4/9] Adding complete land and sea polygons"
land_zip="${WORK_ROOT}/land-polygons-split-4326.zip"
land_directory="${WORK_ROOT}/land-polygons-split-4326"
land_clip="${WORK_ROOT}/land.shp"
download "${LAND_URL}" "${land_zip}"
unzip -q "${land_zip}" -d "${WORK_ROOT}"
ogr2ogr -overwrite -skipfailures \
  -clipsrc "${LEFT}" "${BOTTOM}" "${RIGHT}" "${TOP}" \
  "${land_clip}" "${land_directory}/land_polygons.shp"
python3 "${shape2osm}" -l "${WORK_ROOT}/land" "${land_clip}"

sea_file="${WORK_ROOT}/sea.osm"
cp "${SCRIPT_ROOT}/sea.osm" "${sea_file}"
sed -i \
  -e "s/\$LEFT/${LEFT}/g" \
  -e "s/\$BOTTOM/${BOTTOM}/g" \
  -e "s/\$RIGHT/${RIGHT}/g" \
  -e "s/\$TOP/${TOP}/g" \
  "${sea_file}"

shopt -s nullglob
land_osm_files=("${WORK_ROOT}"/land*.osm)
if ((${#land_osm_files[@]} == 0)); then
  echo "Land conversion did not produce OSM files" >&2
  exit 1
fi

osmosis_merge=(
  osmosis
  --read-pbf "file=${SAUDI_PBF}"
  --read-xml "file=${sea_file}"
  --sort
  --merge
)
for land_osm_file in "${land_osm_files[@]}"; do
  osmosis_merge+=(--read-xml "file=${land_osm_file}" --sort --merge)
done
osmosis_merge+=(--write-pbf "file=${MERGED_PBF}" omitmetadata=true)
"${osmosis_merge[@]}"

echo "[5/9] Writing Arabic-first Mapsforge map"
osmosis \
  --read-pbf "file=${MERGED_PBF}" \
  --tag-transform "file=${tag_transform}" \
  --mapfile-writer \
    "file=${MAP_FILE}" \
    "bbox=${BOTTOM},${LEFT},${TOP},${RIGHT}" \
    "map-start-position=${START_POSITION}" \
    map-start-zoom=7 \
    preferred-languages=ar,en \
    tag-values=true \
    label-position=true \
    polylabel=false \
    type=hd \
    threads=2 \
    "comment=Launcher 2026 Saudi offline map; data OpenStreetMap contributors" \
    progress-logs=true

echo "[6/9] Writing searchable POI v4 database"
osmosis \
  --read-pbf "file=${SAUDI_PBF}" \
  --poi-writer \
    "file=${POI_FILE}" \
    "bbox=${BOTTOM},${LEFT},${TOP},${RIGHT}" \
    all-tags=true \
    preferred-language=ar \
    normalize=true \
    names=false \
    ways=true \
    way-filtering=false \
    geo-tags=true \
    filter-categories=true \
    "comment=Launcher 2026 Saudi offline POI; data OpenStreetMap contributors" \
    progress-logs=true

echo "[7/9] Verifying map compatibility with Launcher Mapsforge 0.25.0"
magic="$(head -c 20 "${MAP_FILE}")"
if [[ "${magic}" != "mapsforge binary OSM" ]]; then
  echo "Invalid Mapsforge map header" >&2
  exit 1
fi
mvn --batch-mode --quiet --file "${SCRIPT_ROOT}/verifier/pom.xml" \
  -DskipTests package dependency:copy-dependencies
java -cp "${SCRIPT_ROOT}/verifier/target/classes:${SCRIPT_ROOT}/verifier/target/dependency/*" \
  com.launchercar.maps.MapPackVerifier "${MAP_FILE}" "${MAP_REPORT}"

echo "[8/9] Verifying POI integrity, Arabic data, normalization, and FTS search"
poi_integrity="$(sqlite3 "${POI_FILE}" 'PRAGMA quick_check;')"
poi_version="$(sqlite3 "${POI_FILE}" "SELECT value FROM metadata WHERE name='version' LIMIT 1;")"
poi_count="$(sqlite3 "${POI_FILE}" 'SELECT COUNT(*) FROM poi_data;')"
poi_arabic_samples="$(sqlite3 "${POI_FILE}" "SELECT COUNT(*) FROM poi_data WHERE data LIKE '%الرياض%' OR data LIKE '%جدة%' OR data LIKE '%مكة%' OR data LIKE '%المدينة%' OR data LIKE '%الدمام%';")"
poi_normalized="$(sqlite3 "${POI_FILE}" "SELECT COUNT(*) FROM poi_data WHERE data LIKE '%normalized_name=%';")"
poi_fts_riyadh="$(sqlite3 "${POI_FILE}" "SELECT COUNT(*) FROM poi_data_fts WHERE data LIKE '%الرياض%';")"

if [[ "${poi_integrity}" != "ok" || "${poi_version}" != "4" ]]; then
  echo "POI database integrity/version check failed" >&2
  exit 1
fi
if ((poi_count < 10000 || poi_arabic_samples < 1 || poi_normalized < 1 || poi_fts_riyadh < 1)); then
  echo "POI Arabic/search coverage check failed" >&2
  exit 1
fi

{
  printf 'integrity=%s\n' "${poi_integrity}"
  printf 'version=%s\n' "${poi_version}"
  printf 'poi_count=%s\n' "${poi_count}"
  printf 'arabic_city_records=%s\n' "${poi_arabic_samples}"
  printf 'normalized_records=%s\n' "${poi_normalized}"
  printf 'fts_riyadh_matches=%s\n' "${poi_fts_riyadh}"
  printf '\nmetadata:\n'
  sqlite3 -header -column "${POI_FILE}" 'SELECT name, value FROM metadata ORDER BY name;'
} | tee "${POI_REPORT}"

echo "[9/9] Creating checksums and build manifest"
map_sha256="$(sha256sum "${MAP_FILE}" | awk '{print $1}')"
poi_sha256="$(sha256sum "${POI_FILE}" | awk '{print $1}')"
map_size="$(stat --format=%s "${MAP_FILE}")"
poi_size="$(stat --format=%s "${POI_FILE}")"
generated_at="$(date --utc +'%Y-%m-%dT%H:%M:%SZ')"

cp "${SCRIPT_ROOT}/PACKAGE-README-AR.md" "${OUTPUT_ROOT}/README-AR.md"
(
  cd "${OUTPUT_ROOT}"
  sha256sum Saudi-2026.map Saudi-2026.poi > SHA256SUMS
)

jq --null-input \
  --arg generatedAt "${generated_at}" \
  --arg sourceTimestamp "${source_timestamp}" \
  --arg sourceUrl "${SOURCE_URL}" \
  --arg sourceSha256 "${source_sha256}" \
  --arg mapSha256 "${map_sha256}" \
  --arg poiSha256 "${poi_sha256}" \
  --argjson mapSize "${map_size}" \
  --argjson poiSize "${poi_size}" \
  --argjson poiCount "${poi_count}" \
  --argjson arabicCityRecords "${poi_arabic_samples}" \
  --argjson normalizedRecords "${poi_normalized}" \
  --argjson ftsRiyadhMatches "${poi_fts_riyadh}" \
  --slurpfile mapVerification "${MAP_REPORT}" \
  '{
    pack: "Saudi-2026",
    generatedAt: $generatedAt,
    coverage: "Saudi Arabia with a small coastline and border margin",
    relation: { type: "OpenStreetMap", id: 307584 },
    source: {
      provider: "Geofabrik",
      url: $sourceUrl,
      dataTimestamp: $sourceTimestamp,
      sha256: $sourceSha256
    },
    mapsforge: {
      writerVersion: "0.30.0",
      preferredLanguages: ["ar", "en"],
      includesLandSeaPolygons: true,
      compatibilityVerifiedWithReader: "0.25.0"
    },
    poi: {
      formatVersion: 4,
      fullTextSearch: "FTS5 trigram",
      normalizedArabicNames: true,
      records: $poiCount,
      arabicCityRecords: $arabicCityRecords,
      normalizedRecords: $normalizedRecords,
      ftsRiyadhMatches: $ftsRiyadhMatches
    },
    files: {
      map: { name: "Saudi-2026.map", bytes: $mapSize, sha256: $mapSha256 },
      poi: { name: "Saudi-2026.poi", bytes: $poiSize, sha256: $poiSha256 }
    },
    verification: $mapVerification[0]
  }' > "${OUTPUT_ROOT}/manifest.json"

find "${OUTPUT_ROOT}" -maxdepth 2 -type f -printf '%P %s bytes\n' | sort
df -h "${WORK_ROOT}"

