CREATE KEYSPACE IF NOT EXISTS lcmap_landsat_test
WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1};


CREATE TABLE IF NOT EXISTS lcmap_landsat_test.sources (
    id text,
    uri text static,
    checksum text static,
    state_at timestamp,
    state_name text,
    state_desc text,
    ubid text,
    PRIMARY KEY (id, state_at)
)
WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
AND  COMPACTION = { 'class' :  'LeveledCompactionStrategy' };


CREATE TABLE IF NOT EXISTS lcmap_landsat_test.conus (
    x int,
    y int,
    ubid text,
    acquired timestamp,
    data blob,
    source text,
    PRIMARY KEY ((x, y, ubid), acquired)
)
WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
AND  COMPACTION = { 'class' :  'LeveledCompactionStrategy' };


CREATE TABLE IF NOT EXISTS lcmap_landsat_test.tile_specs (
    keyspace_name text,
    table_name text,
    projection text static,
    tile_x int static,
    tile_y int static,
    pixel_x float static,
    pixel_y float static,
    shift_x float static,
    shift_y float static,
    resample_method text static,
    ubid text,
    satellite text,
    instrument text,
    band_product text,
    band_category text,
    band_name text,
    band_long_name text,
    band_short_name text,
    band_spectrum list<double>,
    data_fill int,
    data_range list<int>,
    data_scale float,
    data_type text,
    data_units text,
    data_shape list<int> static,
    data_mask map<int, text>,
    PRIMARY KEY ((keyspace_name, table_name), ubid));
