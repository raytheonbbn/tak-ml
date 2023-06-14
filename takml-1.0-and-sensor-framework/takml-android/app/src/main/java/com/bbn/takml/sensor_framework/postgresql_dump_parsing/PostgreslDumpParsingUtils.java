package com.bbn.takml.sensor_framework.postgresql_dump_parsing;

import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.PostgresqlConstants.POSTGRESQL_DATATYPE_BIG_INT;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.PostgresqlConstants.POSTGRESQL_DATATYPE_GEOM;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.PostgresqlConstants.POSTGRESQL_DATATYPE_INTEGER;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.PostgresqlConstants.POSTGRESQL_DATATYPE_REAL;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.PostgresqlConstants.POSTGRESQL_DATATYPE_SMALL_INT;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.PostgresqlConstants.POSTGRESQL_DATATYPE_TEXT;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.PostgresqlConstants.POSTGRESQL_DATATYPE_TIMESTAMP;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.SQLiteConstants.SQLITE_DATATYPE_BLOB;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.SQLiteConstants.SQLITE_DATATYPE_INTEGER;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.SQLiteConstants.SQLITE_DATATYPE_REAL;
import static com.bbn.takml.sensor_framework.postgresql_dump_parsing.SQLiteConstants.SQLITE_DATATYPE_TEXT;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostgreslDumpParsingUtils {

    public static final String TAG = PostgreslDumpParsingUtils.class.getSimpleName();

    public static final String POSTGRESQL_CREATE_TABLE_BEGIN_STRING = "CREATE TABLE";
    public static final String POSTGRESQL_CREATE_TABLE_END_STRING = ";";

    public static class SQLiteColumnStruct {
        public String name;
        public String dataType;
        public String extraSpecifiers;

        @Override
        public String toString() {
            return "(" + name + ", " + dataType + ", " + extraSpecifiers + ")";
        }
    }

    public static class SQLiteCreateTableCmdStruct {
        public String tableName;
        public List<SQLiteColumnStruct> columns;

        @Override
        public String toString() {
            return "[" + tableName + ", " + columns + "]";
        }

    }

    public static SQLiteCreateTableCmdStruct parseForCreateTableCmdStruct(String postgresqlCreateTableCmd) {

        SQLiteCreateTableCmdStruct ret = new SQLiteCreateTableCmdStruct();
        ret.columns = new ArrayList<>();

        // assume format of "CREATE TABLE <table name> (<column info 1>, <column info 2>, ...);
        postgresqlCreateTableCmd = postgresqlCreateTableCmd.substring(
                POSTGRESQL_CREATE_TABLE_BEGIN_STRING.length() + " ".length()
        );
        ret.tableName = postgresqlCreateTableCmd.substring(0, postgresqlCreateTableCmd.indexOf(" "));
        // assume that all of the table names are in the format: public.<table name>
        ret.tableName = ret.tableName.substring(ret.tableName.indexOf(".") + 1);
        // remove quotes from around table name if they are there
        boolean tableNameHadQuotes = false;
        if (ret.tableName.startsWith("\"") && ret.tableName.endsWith("\"")) {
            ret.tableName = ret.tableName.substring(1);
            ret.tableName = ret.tableName.substring(0, ret.tableName.length() - 1);
            tableNameHadQuotes = true;
        }

        postgresqlCreateTableCmd = postgresqlCreateTableCmd.substring(
                "public.".length() + ret.tableName.length() + " ".length() + (tableNameHadQuotes ? 2 : 0)
        );

        Log.d(TAG, "Extracted column information from postgresql create table cmd:\n" +
                postgresqlCreateTableCmd);

        // at this point string should just be (<column info 1>, <column info 2>, ...);
        postgresqlCreateTableCmd = postgresqlCreateTableCmd.substring(1);
        postgresqlCreateTableCmd = postgresqlCreateTableCmd.substring(0, postgresqlCreateTableCmd.length() - 2);
        // https://stackoverflow.com/questions/31993153/java-split-string-on-comma-except-when-between-parenthesis
        String[] columnInfos = postgresqlCreateTableCmd.split(",(?![^()]*\\))");
        for (String columnInfo : columnInfos) {
            // assume column info is in format "<column name> <data type> <other specifiers>"
            SQLiteColumnStruct columnStruct = new SQLiteColumnStruct();
            String[] parts = removeEmptyEntries(columnInfo.split("\\s+"));
            Log.d(TAG, "columnInfo: " + columnInfo);
            Log.d(TAG, "parts: " + Arrays.toString(parts));

            columnStruct.name = parts[0];
            // remove quotes from around name if they exist
            if (columnStruct.name.startsWith("\"") && columnStruct.name.endsWith("\"")) {
                columnStruct.name = columnStruct.name.substring(1);
                columnStruct.name = columnStruct.name.substring(0, columnStruct.name.length() - 1);
            }

            columnStruct.dataType = parts[1];

            String extraSpecifiersString = "";
            if (parts.length > 2) {
                for (int i = 2; i < parts.length; i++) {
                    extraSpecifiersString += parts[i] + (i == parts.length-1 ? "" : " ");
                }
            }
            columnStruct.extraSpecifiers = extraSpecifiersString;

            ret.columns.add(columnStruct);
        }

        Log.d(TAG, "Created SQLiteCreateTableCmdStruct: " + ret);

        return ret;
    }

    public static List<SQLiteCreateTableCmdStruct> loadTables(Context ctx, String pgDumpAssetName) {

        List<String> createTableStatements = new ArrayList<>();

        AssetManager am = ctx.getAssets();
        InputStream is = null;
        try {
            is = am.open(pgDumpAssetName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            boolean foundCreateTableBeginning = false;
            String currentCreateTableStatement = "";

            while ((line = reader.readLine()) != null) {
                // to find create table statements, assume they begin with the string "CREATE TABLE"
                // and end with the string ";"
                if (foundCreateTableBeginning && line.contains(POSTGRESQL_CREATE_TABLE_END_STRING)) {
                    currentCreateTableStatement += line.substring(0, line.indexOf(POSTGRESQL_CREATE_TABLE_END_STRING) + 1);
                    createTableStatements.add(currentCreateTableStatement);
                    foundCreateTableBeginning = false;
                }
                else if (foundCreateTableBeginning) {
                    currentCreateTableStatement += line + "\n";
                }
                else if (line.contains(POSTGRESQL_CREATE_TABLE_BEGIN_STRING)) {
                    currentCreateTableStatement = "";
                    currentCreateTableStatement += line.substring(line.indexOf(POSTGRESQL_CREATE_TABLE_BEGIN_STRING)) + "\n";
                    foundCreateTableBeginning = true;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to open server side db pg dump: " + e.getMessage());
        }

        List<SQLiteCreateTableCmdStruct> ret = new ArrayList<>();

        for (String createTableStatement : createTableStatements) {
            Log.d(TAG, "Found create table statement:\n" + createTableStatement);
            ret.add(parseForCreateTableCmdStruct(createTableStatement));
        }

        return ret;

    }

    public static String getSQLiteCreateTableCommand(SQLiteCreateTableCmdStruct s) {
        String ret = "CREATE TABLE " + s.tableName + " (";
        for (SQLiteColumnStruct c : s.columns) {

            String sqliteDataType = "";
            if (c.dataType.equals(POSTGRESQL_DATATYPE_TEXT) ||
                c.dataType.equals(POSTGRESQL_DATATYPE_TIMESTAMP)) {
                sqliteDataType = SQLITE_DATATYPE_TEXT;
            } else if (c.dataType.equals(POSTGRESQL_DATATYPE_BIG_INT) ||
                        c.dataType.equals(POSTGRESQL_DATATYPE_INTEGER) ||
                        c.dataType.equals(POSTGRESQL_DATATYPE_SMALL_INT)) {
                sqliteDataType = SQLITE_DATATYPE_INTEGER;
            } else if (c.dataType.equals(POSTGRESQL_DATATYPE_REAL)) {
                sqliteDataType = SQLITE_DATATYPE_REAL;
            } else if (c.dataType.equals(POSTGRESQL_DATATYPE_GEOM)) {
                sqliteDataType = SQLITE_DATATYPE_TEXT;
            } else {
                sqliteDataType = SQLITE_DATATYPE_BLOB;
            }
            // ret += c.name + " " + sqliteDataType + (c.extraSpecifiers.equals("") ? "" : " " + c.extraSpecifiers) + ", ";
            // leave out the extra specifiers from command, to avoid incompatibility between postgresql and sqlite
            if(c.name.equalsIgnoreCase("ID") && sqliteDataType.equals(SQLITE_DATATYPE_INTEGER)) {
                ret += c.name + " " + sqliteDataType + " primary key autoincrement, ";
            } else {
                ret += c.name + " " + sqliteDataType + ", ";
            }

        }

        ret = ret.substring(0, ret.length()-2); // get rid of last extra comma and space
        ret += ")";
        return ret;
    }

    private static String[] removeEmptyEntries(String[] in) {
        List<String> l = new ArrayList<>();
        for (String s : in) {
            if (!s.equals("")) {
                l.add(s);
            }
        }
        String[] ret = new String[l.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = l.get(i);
        }
        return ret;
    }

}
