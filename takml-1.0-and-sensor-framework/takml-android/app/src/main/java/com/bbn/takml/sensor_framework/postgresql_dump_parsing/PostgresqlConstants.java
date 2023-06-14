package com.bbn.takml.sensor_framework.postgresql_dump_parsing;

public class PostgresqlConstants {

    // will map to SQLITE_DATATYPE_TEXT
    public static final String POSTGRESQL_DATATYPE_TEXT = "text";
    public static final String POSTGRESQL_DATATYPE_TIMESTAMP = "timestamp";

    // will map to SQLITE_DATATYPE_INTEGER
    public static final String POSTGRESQL_DATATYPE_BIG_INT = "bigint";
    public static final String POSTGRESQL_DATATYPE_INTEGER = "integer";
    public static final String POSTGRESQL_DATATYPE_SMALL_INT = "smallint";

    // will map to SQLITE_DATATYPE_REAL
    public static final String POSTGRESQL_DATATYPE_REAL = "real";

    // will map to SQLITE_DATA_TYPE_TEXT with GeoJSON encoding
    public static final String POSTGRESQL_DATATYPE_GEOM = "geom";

    // all other postgresql data types will map to SQLITE_DATATYPE_BLOB

}
