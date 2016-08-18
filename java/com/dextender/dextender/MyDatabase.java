package com.dextender.dextender;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.sql.SQLException;

//-----------------------------------------------------------------------------------------
// Created by livolsi on 2/2/2014.
// How to call from other methods:
//      MyDatabase dexDB = new dexDB(your_method.this);
//      boolean rc=true;
//      try {
//      dexDB.open();
//      dexDB.insert(xxx);
//      dexDB.close();
//      } catch (Exception e){
//        rc=false;
//      }
// 
//  The database is the bridge between the service and the UI
//
//-----------------------------------------------------------------------------------------
public class MyDatabase {

    int glb_slotId;
    long glb_sequenceId;
    public static Integer MAX_LOG_SLOTS=100;
    public static Integer MAX_BG_SLOTS=288;

    public static Integer MAX_LOG_DISPLAY_ENTRIES=20;

    public static final String DATABASE_NAME="dexDB";
    public static final String DATABASE_TABLE_ACCOUNT_INFO="dexAccount";
    public static final String DATABASE_TABLE_BG="dexBg";
    public static final String DATABASE_TABLE_SERVICES="dexServices";
    public static final String DATABASE_TABLE_LOG="dexLogs";
    public static final String DATABASE_TABLE_ALARM="dexAlarm";
    public static final String DATABASE_TABLE_WEB_TRACKER="dexWebTracker";
    public static final String DATABASE_TABLE_SLOT="dexSlotTracker";
    public static final String DATABASE_TABLE_MESSAGES="dexMessages";
    public static final String DATABASE_TABLE_OPTIONS="dexOptions";
    public static final String DATABASE_TABLE_TREND="dexTrend";
    public static final int    DATABASE_VERSION=12;

    // DB version 10 = PROD version 2.0
    // DB version 11 = "Belmar 2.1.0.035"
    // DB version 12 = "Cape May 3"

    MyTools tools = new MyTools();                                                  // Call the httpd class

    //-----------------------------------------------------------
    // Account Columns
    //-----------------------------------------------------------
    public static final String   COL_ACCOUNT_ID             = "id";                                 //  generic key
    public static final String   COL_ACCOUNT_ACCOUNT_ID     = "account_id";                         //  Your dexcom account number
    public static final String   COL_ACCOUNT_SUB_ID         = "subscription_id";
    public static final String   COL_ACCOUNT_APPLICATION_ID = "application_id";                     //  The application id dexcom expects
    public static final String   COL_ACCOUNT_ACCOUNT_PWD    = "account_pwd";                        //  Your dexcom password
    public static final String   COL_ACCOUNT_SESSION_ID     = "session_id";
    public static final String   COL_ACCOUNT_DEXTWEB_STATUS = "dextweb_status";                     // Has this been validated against our site ?
    //-----------------------------------------------------------
    // Bg Columns
    //-----------------------------------------------------------
    //public static final String COL_BG_SEQ          = "seq_id";          //  Column name of the table, not the value of the column
    public static final String COL_BG_SLOT_ID      = "slot_id";           //  Column name of the table, not the value of the column
    public static final String COL_BG_VALUE        = "bg_value";          //  Column name of the table, not the value of the column
    public static final String COL_BG_TREND        = "trend";             //  Column name of the table, not the value of the column
    public static final String COL_BG_DEX_DATE     = "dex_date";          //  Column name of the table, not the value of the column
    public static final String COL_BG_CREATED_DATE = "created_date";      //  Column name of the table, not the value of the column

    //-----------------------------------------------------------
    // Services columns
    //-----------------------------------------------------------
    // public static final String COL_SERVICE_ID      = "service_id";       // service id (ie 1 = web, 2=usb)
    public static final String COL_SERVICE_NAME    = "service_name";      // usb, web, etc..
    public static final String COL_SERVICE_STATUS  = "status";            // is the service active
    public static final String COL_SERVICE_LRT     = "last_run_time";     // last time the service was started/stopped or ran
    //public static final String COL_CREATED_DT      = "created_date";      // When we created this record

    //-----------------------------------------------------------
    // Log columns
    //-----------------------------------------------------------
    public static final String COL_LOG_ID          = "log_id";            //  Column name of the table, not the value of the column
    public static final String COL_LOG_SLOT_ID     = "slot_id";           //  Column name of the table, not the value of the column
    public static final String COL_LOG_ERROR_LEVEL = "error_level";
    public static final String COL_LOG_COMMENT     = "comment";           //  Column name of the table, not the value of the column
    public static final String COL_LOG_CREATED     = "created_date";        //  Column name of the table, not the value of the column

    //-----------------------------------------------------------
    // Slot tracker columns
    //-----------------------------------------------------------
    public static final String COL_SLOT_TNAME       = "table_name";       //  name of the table (either BG/LOG)
    public static final String COL_SLOT_ID          = "slot_id";          //  The slot that we are targeting
    public static final String COL_SLOT_SEQUENCE_ID = "seq_id";           //  The sequence that we are targeting
    public static final String COL_SLOT_CREATED     = "created_date";     //  The sequence that we are targeting

    //-----------------------------------------------------------
    // Alarm columns
    //-----------------------------------------------------------
    public static final String COL_ALARM_ID    = "alarm_id";                                        // Either 1(low), 2(high) or 3(sustained)
    public static final String COL_ALARM_COUNT = "alarm_count";                                     // The number of times we've called this alarm
    public static final String COL_ALARM_TIME = "last_update";                                      //  The time we alarmed

    //-----------------------------------------------------------
    // Option columns
    //-----------------------------------------------------------
    //public static final String COL_OPTION_ID     = "option_id";                                   // Either 1(low), 2(high) or 3(sustained)
    //public static final String COL_OPTION_NAME   = "option_name";                                 // The number of times we've called this alarm
    //public static final String COL_OPTION_STATUS = "status";
    //public static final String COL_OPTION_DATE   = "last_update";

    //-----------------------------------------------------------
    // message columns
    //-----------------------------------------------------------
    //public static final String COL_MSG_ID        = "msg_id";                                        // Right now this will be 1, server only
    public static final String COL_MSG_TYPE      = "msg_type";                                      // Right now, this will be 'server'
    public static final String COL_MSG_STATUS    = "msg_status";                                    // 0 do not show, 1 show
    public static final String COL_MSG_MSG       = "msg_msg";                                       // the actual message
    //public static final String COL_MSG_DATE      = "last_update";                                   // when it was created

    private DbHelper       ourHelper;
    private final Context  ourContext;
    private SQLiteDatabase ourDatabase;


    private static class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }


        @Override
        public void onCreate(SQLiteDatabase db) {

            //--------------------------------------------------------------------------------------
            // Table account
            // Purpose      : A 288 slot circular queue. Kept in check by dexSlotTracker, but also
            //                referenced by the dexWebTracker
            //
            // seq_id       - sequence of this entry
            // bg_value     - from dex - bg value
            // trend        - from dex - bg trend
            // dex_date     - from dex - date on receiver - stored as epoch
            // created_date - when this record was created.
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_ACCOUNT_INFO +
                    " (id             integer primary key, " +
                    " account_id      varchar not null, " +
                    " subscription_id varchar not null, " +
                    " application_id  varchar not null, " +
                    " account_pwd     varchar not null, " +
                    " session_id      varchar not null, " +
                    " dextweb_status  varchar not null); ");

            //--------------------------------------------------------------------------------------
            // Table bg
            // Purpose      : A 288 slot circular queue. Kept in check by dexSlotTracker, but also
            //                referenced by the dexWebTracker
            //
            // seq_id       - sequence of this entry
            // bg_value     - from dex - bg value
            // trend        - from dex - bg trend
            // dex_date     - from dex - date on receiver - stored as epoch
            // created_date - when this record was created.
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_BG +
                    " (slot_id integer primary key, " +
                    " seq_id integer not null, " +
                    " bg_value integer not null, " +
                    " trend integer not null, " +
                    " dex_date integer not null, " +
                    " created_date integer not null); ");

            //--------------------------------------------------------------------------------------
            // Services
            // Keep track of the status of the services, such as cloud post, usb and general
            // services
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_SERVICES +
                    " (service_id integer primary key, " +
                    " service_name varchar not null, " +
                    " status integer not null, " +
                    " last_run_time integer not null, " +
                    " created_date integer not null); ");


            //--------------------------------------------------------------------------------------
            // log
            // log_id       - Sequence of this entry
            // comment      - Message/comment to display
            // create_date  - when this record was created (stored as epoch)
            //
            // NOTE: log is a circular queue of 100 entries
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_LOG +
                    " (slot_id integer primary key," +
                    " log_id integer not null," +
                    " error_level integer not null, " +
                    " comment varchar not null," +
                    " created_date integer not null);");

            //--------------------------------------------------------------------------------------
            // Slot Tracker
            // Purpose: Since BG and Logs are circular queues, the slot tracker keeps track of which
            //          slot in the BG and Log tables to put an entry
            //
            // table_name varchar
            // slot_id    integer  -- Used to keep track of which rows to stuff data in
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_SLOT +
                    " (table_name varchar not null, " +
                    " slot_id integer not null, " +
                    " seq_id integer not null," +
                    " created_date integer not null); ");

            //--------------------------------------------------------------------------------------
            // Web Tracker
            // table_name varchar
            // offset    integer  -- Used to keep track of which rows to stuff data in
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_WEB_TRACKER +
                    " (table_name varchar not null," +
                    " last_sequence_id integer not null," +
                    " last_sequence_date integer not null); ");

            //--------------------------------------------------------------------------------------
            // Alarm
            // table_name varchar
            // slot_id    integer  -- Used to keep track of which rows to stuff data in
            // 1 - low
            // 2 - high
            // 3 - sustained high
            // 4 - Receiver Failure
            // 5 - Hard Low   - set alarm_count to 1 when active, 0 when cleared
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_ALARM +
                    " (alarm_id integer not null, " +
                    " alarm_count integer not null, " +
                    " last_update integer not null);");

            //--------------------------------------------------------------------------------------
            // Options
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_OPTIONS +
                    " (option_id integer primary key, " +
                    " option_name varchar not null, " +
                    " status      integer not null, " +
                    " last_update integer not null);");

            //--------------------------------------------------------------------------------------
            // Trend
            // Only 1 record in this database
            // -1 falling, 0
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_TREND +
                    " (trend_id   integer primary key, " +
                    " bg          integer not null, " +
                    " dexdate     integer not null, " +
                    " last_update integer not null);");


            //--------------------------------------------------------------------------------------
            // messages
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_MESSAGES +
                    " (msg_id     integer primary key, " +
                    " msg_type    varchar not null, " +
                    " msg_status  integer not null, " +
                    " msg_msg     varchar not null, " +
                    " last_update integer not null);");

            //--------------------------------------------------------------------------------------
            // Indexes
            //--------------------------------------------------------------------------------------
            db.execSQL("CREATE INDEX IF NOT EXISTS x1_dexBg   on " + DATABASE_TABLE_BG  + "(seq_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS x1_dexLogs on " + DATABASE_TABLE_LOG + "(log_id);");
            //================
            // SEED DATA
            //================

            db.execSQL("insert into " + DATABASE_TABLE_ACCOUNT_INFO + " (id, account_id, subscription_id, application_id, account_pwd, session_id, dextweb_status) " +
                       "values (0, '0', '0','0', '0', '0', 'unknown');" );

            db.execSQL("insert into " + DATABASE_TABLE_BG + " (slot_id, seq_id, bg_value, trend, dex_date, created_date) " +
                       "values (0, 0, 98, 180, 1425510428, 1425510428);" );  // 3,4,2015 - 06:07:08
            int i;
            for(i=1; i < MAX_BG_SLOTS; i++) {
                db.execSQL("insert into " + DATABASE_TABLE_BG + " (slot_id, seq_id, bg_value, trend, dex_date, created_date) " +
                        "values (" + i + ", 0, 0, 0, 0, 0);" );
            }
            //--------------------------------------------------------------------------------------
            // Seed data to update later on
            // Status: 0 = off
            //         1 = On
            //         2 = failure
            //         3 = Other
            //--------------------------------------------------------------------------------------
            db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                       " values (1, 'Background Service', 0, 0, 1412713000);");
            db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                    " values (2, 'Web Handler', 0, 0, 1412713000);");
            db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                    " values (3, 'Cloud Account', 0, 0, 1412713000);");

            //-----------------------------
            // Seed data for logs
            //-----------------------------
            db.execSQL("insert into " + DATABASE_TABLE_LOG + " (slot_id, log_id, error_level, comment, created_date) " +
                    "values (0, 0, 0, 'Begin of local log', 0)");

            for(i=1; i < 100; i++) {
                db.execSQL("insert into " + DATABASE_TABLE_LOG + " (slot_id, log_id, error_level, comment, created_date) " +
                        "values (" + i + ",0, 0, '', 0);" );
            }

            //--------------------------------------
            // web tracker
            // The last_ID for BG is used to track the last BG we posted
            //--------------------------------------
            db.execSQL("insert into " + DATABASE_TABLE_WEB_TRACKER +
                    " (table_name, last_sequence_id, last_sequence_date) values ('" + DATABASE_TABLE_BG +"',0, 0)");

            //--------------------------------------------------------------------
            // Slot tracker
            // The last_ID for BG is used to track the last BG we posted
            // points to the current slot
            //--------------------------------------------------------------------
            db.execSQL("insert into " + DATABASE_TABLE_SLOT +
                    " (table_name, slot_id, seq_id, created_date) values ('dexLogs',1, 1, 0)");
            db.execSQL("insert into " + DATABASE_TABLE_SLOT +
                    " (table_name, slot_id, seq_id, created_date) values ('dexBg'  ,1, 1, 0)");

            //--------------------------------------
            // alarming codes
            //
            // 0 = All alarms
            // 1 = critical low    2 = low          3 = high
            // 4 = sustained       5 = data err     6 = high bg reset
            // 7 = low bg reset    8 = down trend   9 = high trend
            //
            //--------------------------------------
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (0,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (1,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (2,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (3,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (4,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (5,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (6,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (7,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (8,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (9,0,0)");
            db.execSQL("insert into " + DATABASE_TABLE_ALARM + " (alarm_id, alarm_count, last_update) values (10,0,0)");


            //--------------------------------------
            // options
            //--------------------------------------
            db.execSQL("insert into " + DATABASE_TABLE_OPTIONS + " (option_id, option_name, status, last_update) values (1,'smart band',0,0)");

            //--------------------------------------
            // messages
            //--------------------------------------
            db.execSQL("insert into " + DATABASE_TABLE_MESSAGES + " (msg_id, msg_type, msg_status, msg_msg, last_update) " +
                           " values (1,'server',0, '-', 0)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Log.d("DB-->", "New Version is" + newVersion);
            switch (newVersion) {
                case 12:
                    db.execSQL("delete from " + DATABASE_TABLE_SERVICES);
                    db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                            " values (1, 'Background Service', 0, 0, 1412713000);");
                    db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                            " values (2, 'Web Handler', 0, 0, 1412713000);");
                    db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                            " values (3, 'Cloud Account', 0, 0, 1412713000);");
                    db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                            " values (4, 'Internet Connection', 0, 0, 1412713000);");
                    break;
                case 11:
                    db.execSQL("delete from " + DATABASE_TABLE_SERVICES);
                    db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                            " values (1, 'Background Service', 0, 0, 1412713000);");
                    db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                            " values (2, 'Web Handler', 0, 0, 1412713000);");
                    db.execSQL("insert into " + DATABASE_TABLE_SERVICES + " (service_id, service_name, status, last_run_time, created_date) " +
                            " values (3, 'Cloud Account', 0, 0, 1412713000);");
                    break;
                default:
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_ACCOUNT_INFO);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_ALARM);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SLOT);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_LOG);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_BG);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SERVICES);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_WEB_TRACKER);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_OPTIONS);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_TREND);
                    db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_MESSAGES);
                    onCreate(db);                                                         // call method
                    break;
            }
        }
    }
    //----------------------------------------------------------------------
    // Constructor for this class
    //----------------------------------------------------------------------
    public MyDatabase(Context c) {
        //c = ge
        ourContext = c;                                                        // Private
    }

    public MyDatabase open() throws SQLException{
        ourHelper = new DbHelper(ourContext);
        ourDatabase = ourHelper.getWritableDatabase();
        return this;
    }

    public boolean close() {
       ourHelper.close();
       return true;
    }

    public String getAccountInfo(){
        String[]  columns = new String[] {COL_ACCOUNT_ACCOUNT_ID, COL_ACCOUNT_APPLICATION_ID, COL_ACCOUNT_ACCOUNT_PWD, COL_ACCOUNT_DEXTWEB_STATUS};
        String predicate = COL_ACCOUNT_ID + " = 0;";

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_ACCOUNT_INFO, columns, predicate, null, null, null, null, null);

        String result="";
        int irow1     = c1.getColumnIndex(COL_ACCOUNT_ACCOUNT_ID);
        int irow2     = c1.getColumnIndex(COL_ACCOUNT_APPLICATION_ID);
        int irow3     = c1.getColumnIndex(COL_ACCOUNT_ACCOUNT_PWD);
        int irow4     = c1.getColumnIndex(COL_ACCOUNT_DEXTWEB_STATUS);

        boolean recordsReadFlag=false;

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            result = c1.getString(irow1) + "|" + c1.getString(irow2) + "|" + c1.getString(irow3) + "|" + c1.getString(irow4);
            recordsReadFlag=true;
        }

        c1.close();
        // returns : xxxxxx|xxxxxxxx|xxxxxxx|xxx
        if(!recordsReadFlag) return null;
        else                 return result;
    }


    public String getSessionInfo(){
        String[]  columns = new String[] {COL_ACCOUNT_SUB_ID, COL_ACCOUNT_SESSION_ID};
        String predicate = COL_ACCOUNT_ID + " = 0;";

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_ACCOUNT_INFO, columns, predicate, null, null, null, null, null);

        String result="";
        int irow1     = c1.getColumnIndex(COL_ACCOUNT_SESSION_ID);
        int irow2     = c1.getColumnIndex(COL_ACCOUNT_SUB_ID);

        boolean recordsReadFlag=false;

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            result = c1.getString(irow1) + "|" + c1.getString(irow2);
            recordsReadFlag=true;
        }

        c1.close();
        // returns : xxxxxx|xxxxxxxx|xxxxxxx
        if(!recordsReadFlag) return null;
        else                 return result;
    }


    //-----------------------------------------------------------------------------------
    // Method : Insert log
    // Pass the comment, and time (as epoch value)
    // The sequence ID will autoincrement
    // I'm wondering how SQLite binds this SQL (is it the same as Oracle or MySQL ?)
    //-----------------------------------------------------------------------------------
    public boolean logIt(int argErrorLevel, String argComment) {


        if ( ! getCurrentSlot(DATABASE_TABLE_LOG)) {                                             // get the available slot id
            return false;
        }
        else {
            ContentValues cv = new ContentValues();
            cv.put(COL_LOG_ID, glb_sequenceId);
            cv.put(COL_LOG_ERROR_LEVEL, argErrorLevel);
            cv.put(COL_LOG_COMMENT, argComment);                             // Name/value pair - column name and the value
            cv.put(COL_LOG_CREATED, (System.currentTimeMillis() / 1000));

            String predicate = COL_LOG_SLOT_ID + "= '" + glb_slotId + "';";

            ourDatabase.update(DATABASE_TABLE_LOG, cv, predicate, null);
            incrementSlot(DATABASE_TABLE_LOG);

            return true;
        }
    }
    //-----------------------------------------------------------------------------------
    // Method :  bgIt
    // Pass the bg, trend and dexdate (as epoch value
    // The sequence ID come from the tracker
    // I'm wondering how SQLite binds this SQL (is it the same as Oracle or MySQL ?)
    // If the "client" we get the info from the web-site.
    // then, use the "master" sequence from the Master and store it here
    //-----------------------------------------------------------------------------------
    //  "2|1413332651|109|180"

    public boolean bgIt(String argDexDate, String argBg, String argTrend) {

        if ( ! getCurrentSlot(DATABASE_TABLE_BG)) {                                                 // get the available slot id
            return false;
        }
        else {

            ContentValues cv = new ContentValues();
            //long tmpLong;
            //if (argMasterSeq != null) {                                                             // when we have the dex, this will be null
            //     tmpLong = Long.parseLong(argMasterSeq);                                            // <--- we are the client
            //}
            //else {
            //    tmpLong = glb_sequenceId;                                                           // <--- we are the master
            //}
            //cv.put(COL_BG_SEQ, tmpLong);
            cv.put(COL_BG_VALUE, argBg);                                                            // Name/value pair - column name and the value
            cv.put(COL_BG_TREND, argTrend);                                                         // " " "
            cv.put(COL_BG_DEX_DATE, argDexDate);
            cv.put(COL_BG_CREATED_DATE, (System.currentTimeMillis() / 1000));                       // now()

            String predicate = COL_BG_SLOT_ID + "= '" + glb_slotId + "';";


            ourDatabase.update(DATABASE_TABLE_BG, cv, predicate, null);
            incrementSlot(DATABASE_TABLE_BG);

            return true;
        }
    }

    //------------------------------------------------------------------------------------
    // This method was originally commented out as I no longer needed the max.
    // Just like in other RDBMS...
    // .... the "AS MAX" reference below
    // The "AS MAX" is an alias to the column, so when we index the column, we
    // reference it by the alias, and not the "max (column_name) or "original column_name"
    //-------------------------------------------------------------------------------------
    public long getLastRunTime(){
        String[]  columns = new String[] {" max(" + COL_BG_CREATED_DATE + ") AS max" };
        Cursor c1 = ourDatabase.query(DATABASE_TABLE_BG, columns, null, null, null, null, null);
        long result = 0;
        int irowDate = c1.getColumnIndex("max");

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            result = c1.getLong(irowDate);
        }
        c1.close();
        return(result);
    }

    public long getLastDexBgTime(){
        String[]  columns = new String[] {" max(" + COL_BG_DEX_DATE + ") AS max" };
        Cursor c1 = ourDatabase.query(DATABASE_TABLE_BG, columns, null, null, null, null, null);
        long result = 0;
        int irow1 = c1.getColumnIndex("max");

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            result = c1.getLong(irow1);
        }
        c1.close();
        return(result);
    }


    //------------------------------------------------------------------------------------------
    // Used by the GUI and in the services (only get what we need)
    // ie. if we recorded the value from the dex, and we scan the dex again, only get what we
    // need
    // NOTE !!! - For graphing, this routine is flawed.
    //            The result will return the last row that we retrieved.
    //------------------------------------------------------------------------------------------
    public String getBgData(int inRefreshInterval){
        String[]  columns = new String[] {COL_BG_VALUE, COL_BG_TREND, COL_BG_DEX_DATE, COL_BG_CREATED_DATE};
        String predicate;
        String orderBy = COL_BG_DEX_DATE + " ASC ";
        predicate =  COL_BG_VALUE + "> 0" +
                    " and " + COL_BG_DEX_DATE + " > " + ((System.currentTimeMillis()/1000) - inRefreshInterval*60) + ";";   // last refresh

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_BG, columns, predicate, null, null, null, orderBy, null);

        String result="";
        int irowBg       = c1.getColumnIndex(COL_BG_VALUE);
        int irowTrend    = c1.getColumnIndex(COL_BG_TREND);
        int irowDexDate  = c1.getColumnIndex(COL_BG_DEX_DATE);
        int irowScanDate = c1.getColumnIndex(COL_BG_CREATED_DATE);

        boolean recordsReadFlag=false;
        long tempDate=0;                                                                            // sqlite isn't ordering this correctly

        //------- this will give us the max..note. we aren't adding to the result, but replacing
        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            if(c1.getLong(irowDexDate) > tempDate) {
                tempDate=c1.getLong(irowDexDate);

                result = "0|" + c1.getString(irowBg) + "|" +
                        c1.getString(irowTrend) + "|" + c1.getString(irowDexDate) + "|" + c1.getString(irowScanDate);
                recordsReadFlag = true;
                //Log.d("MyDatabase", "returning -->" + result);
            }
        }

        // returns : 1|123|180|1413400234   - seq|bg|trend|dexdate
        c1.close();

        if(!recordsReadFlag) return null;
        else                 return result;
    }

    public Integer getBgDataAsArrays(long[] argX, int[] argY, long argTime){

        String[]  columns = new String[] {COL_BG_VALUE, COL_BG_DEX_DATE};
        String predicate;
        String orderBy = COL_BG_DEX_DATE + " ASC ";
        predicate = COL_BG_DEX_DATE + " >= " + argTime;


        Cursor c1 = ourDatabase.query(DATABASE_TABLE_BG, columns, predicate, null, null, null, orderBy, null);
        //Cursor c1 = ourDatabase.query(DATABASE_TABLE_BG, columns, null, null, null, null, orderBy, null);

        int irowDexDate = c1.getColumnIndex(COL_BG_DEX_DATE);                                       // X-axis value
        int irowBg      = c1.getColumnIndex(COL_BG_VALUE);                                          // Y-axis Value



        int i=0;
        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            argX[i] = Integer.parseInt(c1.getString(irowDexDate));
            argY[i] = Integer.parseInt(c1.getString(irowBg));
            i=i+1;
        }

        // returns : 1|123|180|1413400234   - seq|bg|trend|dexdate

        c1.close();
        return i;

    }

    public Integer getLastBgDataAsArray(int[] argY, long argTime, int inLimit){

        String[]  columns = new String[] {COL_BG_VALUE};
        String predicate;
        String orderBy = COL_BG_DEX_DATE + " DESC LIMIT " + String.valueOf(inLimit);
        predicate = COL_BG_DEX_DATE + " >= " + argTime;

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_BG, columns, predicate, null, null, null, orderBy, null);
        //Cursor c1 = ourDatabase.query(DATABASE_TABLE_BG, columns, null, null, null, null, orderBy, null);

        int irowBg      = c1.getColumnIndex(COL_BG_VALUE);                                          // Y-axis Value



        int i=0;
        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            argY[i] = Integer.parseInt(c1.getString(irowBg));
            i=i+1;
        }

        // returns : via (argY) the array of BG values
        // returns : via method, record count

        c1.close();
        return i;

    }

    public Integer getLogData(String[] strArray, long currentTime){
        String[]  columns = new String[] {COL_LOG_ID, COL_LOG_ERROR_LEVEL, COL_LOG_COMMENT, COL_LOG_CREATED};
        String predicate = COL_LOG_CREATED + ">  (" + currentTime  + " - 86400) and log_id is not null";
        String limit="20";
        String orderBy = COL_LOG_ID + " DESC ";

        //public Cursor query (String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit)
        Cursor c1 = ourDatabase.query(DATABASE_TABLE_LOG, columns, predicate, null, null, null, orderBy, limit);

        int irow01      = c1.getColumnIndex(COL_LOG_ERROR_LEVEL);
        int irowComment = c1.getColumnIndex(COL_LOG_COMMENT);
        int irowDate    = c1.getColumnIndex(COL_LOG_CREATED);

        Integer i=0;
        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            strArray[i] =  c1.getInt(irow01) + "|" + tools.epoch2FmtTime(c1.getLong(irowDate), "dd-MM-yyyy HH:mm:ss")
                    + "|" + c1.getString(irowComment) + "\n";
            i++;
            if (i >= MAX_LOG_DISPLAY_ENTRIES) break;
        }

        c1.close();
        return i;
    }

    //==============================================================================================
    // SLOT Management
    //==============================================================================================
    //--------------------------------------------------------------------------------------------
    // getSlotSequence - Get the slot id and next sequence id from the slot tracker
    // Then increment the sequenceid and slotid, with the slotid changing based on a modulo
    // to keep the circular queue going.
    //--------------------------------------------------------------------------------------------
    public boolean getCurrentSlot(String argTableName){

        String[]  columns = new String[] {COL_SLOT_ID, COL_SLOT_SEQUENCE_ID };
        String predicate;
        predicate = COL_SLOT_TNAME + "= '" + argTableName + "'";

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_SLOT, columns, predicate, null, null, null, null);

        int  irowSlotId     = c1.getColumnIndex(COL_SLOT_ID);
        int  irowSequenceId = c1.getColumnIndex(COL_SLOT_SEQUENCE_ID);

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            glb_slotId     = c1.getInt(irowSlotId);
            glb_sequenceId = c1.getLong(irowSequenceId);
        }
        c1.close();
        return true;
    }

    public boolean incrementSlot(String argTableName) {

        String[]  columns = new String[] {COL_SLOT_ID, COL_SLOT_SEQUENCE_ID };
        String predicate;
        predicate = COL_SLOT_TNAME + "= '" + argTableName + "'";

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_SLOT, columns, predicate, null, null, null, null);

        int  irowSlotId     = c1.getColumnIndex(COL_SLOT_ID);
        int  irowSequenceId = c1.getColumnIndex(COL_SLOT_SEQUENCE_ID);

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            glb_slotId     = c1.getInt(irowSlotId);
            glb_sequenceId = c1.getLong(irowSequenceId);
        }
        //------------- maintain the cirucular log pointers ---
        int newSlotId;
        if(argTableName.equals(DATABASE_TABLE_LOG)) {
            newSlotId = (glb_slotId + 1) % MAX_LOG_SLOTS;
        }
        else {
            if(argTableName.equals(DATABASE_TABLE_BG) ) {
                newSlotId = (glb_slotId+1)% MAX_BG_SLOTS;
            }
            else {
                newSlotId=-1;
            }
        }
        long newSequenceId = glb_sequenceId+1;

        //---------------- Update the information ---------
        if(newSlotId >= 0) {
            updateSlotId(argTableName, newSlotId, newSequenceId);        }

        c1.close();
        return true;
    }

    //-------------------------------------------------------------------------
    // Update the pointer (offset) into the BG and logs
    //-------------------------------------------------------------------------
    public boolean updateSlotId(String argTableName, long argInSlot, long argInSequence) {

        ContentValues cv = new ContentValues();
        cv.put(COL_SLOT_ID,          argInSlot);
        cv.put(COL_SLOT_SEQUENCE_ID, argInSequence);
        cv.put(COL_SLOT_CREATED,     (System.currentTimeMillis()/1000) );

        String predicate = COL_SLOT_TNAME+ "= '" + argTableName + "';";

        ourDatabase.update(DATABASE_TABLE_SLOT, cv, predicate, null);                            // update it
        return true;
    }

    //-------------------------------------------------------------------------
    // Method    : getLastBg
    // Date      : Jan 2015
    // Author    : MLV
    // Purpose   : To return the last bg value that we recorded
    // Caveats   :
    //-------------------------------------------------------------------------

    public Integer getLastBg() {

        String[] columns = new String[] {COL_BG_VALUE};
        String   predicate = COL_BG_DEX_DATE + " = ( select max(" + COL_BG_DEX_DATE + ") from " + DATABASE_TABLE_BG + ")";
        Cursor c1 = ourDatabase.query(DATABASE_TABLE_BG, columns, predicate, null, null, null, null);
        Integer result = 0;

        int irow1 = c1.getColumnIndex(COL_BG_VALUE);

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            result = c1.getInt(irow1);
        }
        c1.close();
        return(result);
    }

    //==============================================================================================
    // SERVICES
    //==============================================================================================
    public Integer getServiceStatus(String argServiceName){
        String[]  columns = new String[] {COL_SERVICE_STATUS};
        String predicate = COL_SERVICE_NAME + "= '" + argServiceName + "'";

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_SERVICES, columns, predicate, null, null, null, null);
        Integer result=0;
        int irowServiceStatus     = c1.getColumnIndex(COL_SERVICE_STATUS);

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            result = c1.getInt(irowServiceStatus);
        }

        c1.close();
        return result;
    }

    public long getLastServiceRunTime(String argServiceName){

        String[]  columns = new String[] {COL_SERVICE_LRT};
        String predicate = COL_SERVICE_NAME + "= '" + argServiceName + "'";

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_SERVICES, columns, predicate, null, null, null, null);
        long result=0;
        int irowServiceStatus     = c1.getColumnIndex(COL_SERVICE_LRT);

        for(c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()){
            result = c1.getLong(irowServiceStatus);
        }
        c1.close();
        return result;
    }
    //-------------------------------------------------------------------------
    // Update the services with the latest status
    //-------------------------------------------------------------------------
    public int updateServiceStatus(String argServiceName, int argStatus){
        ContentValues cv = new ContentValues();
        cv.put(COL_SERVICE_STATUS, argStatus);
        cv.put(COL_SERVICE_LRT, (System.currentTimeMillis()/1000));
        String predicate = COL_SERVICE_NAME + "= '" + argServiceName + "';";

        return ourDatabase.update(DATABASE_TABLE_SERVICES, cv, predicate, null);
    }

    //==============================================================================================
    // ALARM
    //==============================================================================================
    //-------------------------------------------------------------------------
    // Method : updateAlarm
    // Author : MLV
    // Purpose: To update the alarm table with the current time. Used for
    //          Snoozing
    //-------------------------------------------------------------------------
    public void updateAlarm(int argAlarmId, int argAlarmCount){
        ContentValues cv = new ContentValues();
        cv.put(COL_ALARM_TIME, (System.currentTimeMillis()/1000));
        cv.put(COL_ALARM_COUNT, argAlarmCount);

        String predicate = COL_ALARM_ID + "= " + argAlarmId + ";";

        ourDatabase.update(DATABASE_TABLE_ALARM, cv, predicate, null);
    }

    //-------------------------------------------------------------------------
    // Method : clearAlarm
    // Author : MLV
    // Purpose: If last alarm time is 0, then this is a fresh alarm and
    //          usually sound right away
    //-------------------------------------------------------------------------

    public void clearAlarm(int argAlarmId){
        ContentValues cv = new ContentValues();
        cv.put(COL_ALARM_COUNT, 0);
        cv.put(COL_ALARM_TIME, 0);
        String predicate = COL_ALARM_ID + "= " + argAlarmId + ";";

        ourDatabase.update(DATABASE_TABLE_ALARM, cv, predicate, null);
    }
    //-------------------------------------------------------------------------
    // Method : getAlarm
    // Author : MLV
    // Purpose: To get the time the alarm was rung.
    //          Since int, long aren't passed by reference, but strings are,
    //          we will pass the time as a string and let the calling function
    //          deal with changing it to a long (c, c++ so much better)
    //-------------------------------------------------------------------------
    public void getAlarm(int argAlarmId, String[] outAlarmTime, Integer[] outAlarmCount ) {

        String[] columns = new String[]{COL_ALARM_COUNT, COL_ALARM_TIME};
        String predicate = COL_ALARM_ID + "= " + argAlarmId;

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_ALARM, columns, predicate, null, null, null, null);

        int irow01  = c1.getColumnIndex(COL_ALARM_COUNT);
        int irow02  = c1.getColumnIndex(COL_ALARM_TIME);

        for (c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()) {
            outAlarmCount[0] = c1.getInt(irow01);
            outAlarmTime[0] = c1.getString(irow02);
        }
        c1.close();
    }

    public int  getAlarmCount(int argAlarmId ) {

        int outAlarmCount=0;
        String[] columns = new String[]{COL_ALARM_COUNT};
        String predicate = COL_ALARM_ID + "= " + argAlarmId;

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_ALARM, columns, predicate, null, null, null, null);

        int irowAlarmCount = c1.getColumnIndex(COL_ALARM_COUNT);

        for (c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()) {
            outAlarmCount = c1.getInt(irowAlarmCount);
        }
        c1.close();
        return outAlarmCount;
    }

    //==============================================================================================
    // OPTIONS
    //==============================================================================================
    public boolean updateAccountValidateDextender(String inCloudStatus) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ACCOUNT_DEXTWEB_STATUS, inCloudStatus);

        String predicate = COL_ACCOUNT_ID + "= 0;";
        ourDatabase.update(DATABASE_TABLE_ACCOUNT_INFO, cv, predicate, null);

        return true;
    }

    public boolean updateAccountSession(String inSessionString) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ACCOUNT_SESSION_ID, inSessionString);

        String predicate = COL_ACCOUNT_ID + "= 0;";
        ourDatabase.update(DATABASE_TABLE_ACCOUNT_INFO, cv, predicate, null);

        return true;
    }

    public boolean updateAccountSubscriptionId(String inSubIdString) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ACCOUNT_SUB_ID, inSubIdString);

        String predicate = COL_ACCOUNT_ID + "= 0;";
        ourDatabase.update(DATABASE_TABLE_ACCOUNT_INFO, cv, predicate, null);

        return true;
    }

    public boolean updateAccountInformation(String inAccountId, String inApplicationId, String inAccountPwd) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ACCOUNT_ACCOUNT_ID, inAccountId);
        cv.put(COL_ACCOUNT_APPLICATION_ID, inApplicationId);
        cv.put(COL_ACCOUNT_ACCOUNT_PWD, inAccountPwd);

        String predicate = COL_ACCOUNT_ID + "= 0;";
        ourDatabase.update(DATABASE_TABLE_ACCOUNT_INFO, cv, predicate, null);

        return true;
    }


    //-------------------------------------------------------------------------
    // Method : getServerMessage
    // Author : MLV
    // Purpose: Get any server messages that were passed (if any)
    //-------------------------------------------------------------------------
    public String getServerMessage(){

        String outString=null;
        String[] columns = new String[]{COL_MSG_MSG};
        String predicate = COL_MSG_TYPE + "= 'server' and " + COL_MSG_STATUS + "> 0";

        Cursor c1 = ourDatabase.query(DATABASE_TABLE_MESSAGES, columns, predicate, null, null, null, null);

        int irowMsg = c1.getColumnIndex(COL_MSG_MSG);

        for (c1.moveToFirst(); !c1.isAfterLast(); c1.moveToNext()) {
             outString = c1.getString(irowMsg);
        }
        c1.close();
        return outString;
    }
    //==============================================================================================
    // Clear all logs and bg values
    //==============================================================================================
    //public boolean resetAll() {

    //    if(resetBg()) {
    //        if (resetLog()) {
    //            logIt(4, "Log cleared");
    //            if (resetSlotTracker()) {
    //                if (resetWebTracker()) {
    //                    return true;
    //                }
    //            }
    //        }
    //    }
    //    else {
    //        return false;
    //    }
    //    return false;
    //}

    //public boolean resetBg() {

    //    ContentValues cv = new ContentValues();

    //    cv.put(COL_BG_SEQ, 0);
    //    cv.put(COL_BG_VALUE, 0);                                                                    // Name/value pair - column name and the value
    //    cv.put(COL_BG_TREND, 0);                                                                    // " " "
    //    cv.put(COL_BG_DEX_DATE, 0);
    //    cv.put(COL_BG_CREATED_DATE, (System.currentTimeMillis() / 1000));                           // now()

    //    try {
    //        ourDatabase.update(DATABASE_TABLE_BG, cv, null, null);
    //    }
    //    catch (Exception e) {
    //        e.printStackTrace();
    //        return false;
    //    }

    //    ContentValues cv2 = new ContentValues();
    //    cv2.put(COL_BG_SEQ, 0);
    //    cv2.put(COL_BG_VALUE, 98);                                                                  // Name/value pair - column name and the value
    //    cv2.put(COL_BG_TREND, 180);                                                                 // " " "
    //    cv2.put(COL_BG_DEX_DATE, 1425510428);
    //    cv2.put(COL_BG_CREATED_DATE, (System.currentTimeMillis() / 1000));                          // now()
    //    String predicate = COL_BG_SLOT_ID + " = 0";
    //    try {
    //        ourDatabase.update(DATABASE_TABLE_BG, cv2, predicate, null);
    //    }
    //    catch (Exception e) {
    //        e.printStackTrace();
    //        return false;
    //    }


    //    return true;
    //}

    //public boolean resetLog() {
    //    ContentValues cv = new ContentValues();
    //    cv.put(COL_LOG_ID, 0);
    //    cv.put(COL_LOG_ERROR_LEVEL, 0);
    //    cv.put(COL_LOG_ERROR_LEVEL, 0);
    //    cv.put(COL_LOG_CREATED, 0);
    //    cv.put(COL_LOG_COMMENT, "-");

    //    ourDatabase.update(DATABASE_TABLE_LOG, cv, null, null);
    //    return true;
    //}

    //public boolean resetWebTracker() {
    //    ContentValues cv = new ContentValues();
    //    cv.put(COL_WEBTRACK_DATE, 0);
    //    cv.put(COL_WEBTRACK_SEQUENCE_ID, 0);

    //    ourDatabase.update(DATABASE_TABLE_WEB_TRACKER, cv, null, null);
    //    return true;
    //}

    //public boolean resetSlotTracker() {
    //    ContentValues cv = new ContentValues();
    //    cv.put(COL_SLOT_CREATED, 0);
    //    cv.put(COL_SLOT_ID, 1);
    //    cv.put(COL_SLOT_SEQUENCE_ID, 1);

    //    ourDatabase.update(DATABASE_TABLE_SLOT, cv, null, null);
    //    return true;
    //}
}
