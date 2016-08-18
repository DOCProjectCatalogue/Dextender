package com.dextender.dextender;

import android.os.StrictMode;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

//-------------------------------------------------------------------------------------
// Class    : MyHttpPost
// Called by: Routines within MyService
// Author   : Mike LiVolsi / Originally by various (lots of examples on the web)
// Date     : jan. 2014
//
// Purpose  : In keeping with the spirit of modularity, this class should be called
//            from the "services" routines.
//            Specifically to the application, once the service gets the value
//            from the USB, it should call this class and it's functions and post
//            this information to the web. Additionally, it should store this information
//            locally in a SQLite database. On the "transmit" end, the service should
//            post, while as a client, the service should "get" the results.
//            All this routine does is "post" and "get"
//-------------------------------------------------------------------------------------

public class MyHttpPost {

    String outString;

    //-------------------------------------------------------------------------------------
    // Method   : webGetBG - Get BG values from the web (uses post method)
    //            NOTE: There's obviously confusion in the android community fragment_about
    //            post vs. get. Get appends everything on the query string
    //            whereas post, the query string will  be in the input stream and needs
    //            to be "popped" off that stream. When the server returns the value
    //            we can read the results (obviously)
    //            arguid - Comes from preferences
    //            argBgSeq - comes from our local database. Past the largest seq that we have
    //                       and should return records greater than that.
    //
    // Test cases passed: 00000000           - Server is down                                            - Passed
    //                    11000000|recs      - Server up, user is good, get values to the right of '|'
    //                    10000000           - Server up, user failed                                    - Passed
    // -------------------------------------------------------------------------------------
    public String webLogin2Dex(URI inWebUrl, String[] outMessage, String argAccountId, String argAppId, String argAccountPwd) {

        boolean Rc;
        String data = "00000000|";
        String json;

        try {
            // 1 . create the httpd client
            HttpClient client = new DefaultHttpClient();

            // 2. make the post request
            HttpPost post = new HttpPost(inWebUrl);

            //3. build the JSON object
            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("accountId", argAccountId);
            jsonObject.accumulate("applicationId", argAppId);
            jsonObject.accumulate("password", argAccountPwd);

            // 4. convert JSONObject to JSON to String
            json = jsonObject.toString();

            // Log.d("MyHttpPost", "JSON:" + json + " URL:" + inWebUrl);
            // 5. set json to StringEntity
            StringEntity se = new StringEntity(json);

            // 6. set httpPost Entity
            post.setEntity(se);

            // 7. Set the headers
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0");


            HttpResponse response = null;

            //-----------------------------------------------------------------
            // 8. Call the web !!!
            //-----------------------------------------------------------------
            Rc = false;
            try {
                response = client.execute(post);

                if (response.getStatusLine().getStatusCode() == 500) {
                    return "10000000|user or password is not valid";
                } else {
                    Rc = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!Rc) {
                return data+" Error exception encountered in http calls to server";
            }

            //-----------------------------------------------------------------------------
            // Read the response. This is important because it could be an array of values
            //-----------------------------------------------------------------------------
            String line;                                                                        // used to read from the buffer
            StringBuilder strLine = new StringBuilder();                                        // used to build a concat string

            BufferedReader inBuff = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            strLine.append("11000000|");

            while ((line = inBuff.readLine()) != null) {
                // Append server response in string
                strLine.append(line);
            }
            inBuff.close();
            data = strLine.toString();

        } catch (Exception e) {
            e.printStackTrace();
            outMessage[0] = "Could not get account information - Cloud Server may be down";
        }

         //Log.d("MyHttpPost", "Returning to service:" + data);
        return data;                                                       // We are returning the string
    }

    //-------------------------------------------------------------------------------------
    // Get subscription number (3 freaking calls - Total Bullshit)
    //-------------------------------------------------------------------------------------
    public String webGetSubscriberId(URI inWebUrl, String[] outMessage) {

        boolean Rc;

        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(inWebUrl);

            post.setHeader("Accept", "application/json");

            HttpResponse response = null;

            //-----------------------------------------------------------------
            // Call the web !!!
            //-----------------------------------------------------------------
            Rc = false;

            try {
                // Log.d("MyHttpPost", "Arguments to post - URL :"+inWebUrl);

                response = client.execute(post);
                if (response.getStatusLine().getStatusCode() == 500) {
                    // Log.d("MyHttpPost", "String from server --->" + response);
                    return "10000000|LISTERR1 - Could not retrieve follow list";

                } else {
                    Rc = true;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if (!Rc) {
                return "00000000|Error getting subscriber id using session id";
            }

            //-----------------------------------------------------------------------------
            // Read the response. This is important because it could be an array of values
            //-----------------------------------------------------------------------------
            String line;                                                                            // used to read from the buffer
            StringBuilder strLine = new StringBuilder();                                            // used to build a concat string

            BufferedReader inBuff = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            while ((line = inBuff.readLine()) != null) {
                // Append server response in string
                strLine.append(line);
            }
            inBuff.close();
            String data = strLine.toString();

            if(data.length() < 20) {
                return "10000000|LISTERR2 - Could not retrieve follow list";
            }

            // Log.d("MyHttpPost", "Returning to service (bg):" + data);

            return "11000000|" + data;                                                       // We are returning the string
        } catch (Exception e) {
            e.printStackTrace();
            outMessage[0] = "Could not get account information - Cloud Server may be down";
        }
        return "10000000|unknown error encountered";                                                 // We are returning the string
    }


    //-------------------------------------------------------------------------------------
    // Method   : webGetBG - Get BG values from the web (uses post method)
    //            NOTE: There's obviously confusion in the android community fragment_about
    //            post vs. get. Get appends everything on the query string
    //            whereas post, the query string will  be in the input stream and needs
    //            to be "popped" off that stream. When the server returns the value
    //            we can read the results (obviously)
    //            arguid - Comes from preferences
    //            argBgSeq - comes from our local database. Past the largest seq that we have
    //                       and should return records greater than that.
    //
    // Test cases passed: 00000000           - Server is down                                            - Passed
    //                    11000000|recs      - Server up, user is good, get values to the right of '|'
    //                    10000000           - Server up, user failed                                    - Passed
    //                    11100000|msg|recs  - There's a server message
    //                    11200000           - server message is clear
    //                    11010000           - Smart band option changed - no smartband
    //                    11020000           - Smart band option changed - Subscribed
    //-------------------------------------------------------------------------------------
    public String webGetBg(URI inWebUrl, String[] outMessage) {

        boolean Rc;
        String data = "00000000|";

        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(inWebUrl);

            post.setHeader("Accept", "application/json");

            HttpResponse response = null;

            //-----------------------------------------------------------------
            // Call the web !!!
            //-----------------------------------------------------------------
            Rc = false;

            try {
                // Log.d("MyHttpPost", "Arguments to post - sessionid:"+inWebUrl);

                response = client.execute(post);
                if (response.getStatusLine().getStatusCode() != 200) {
                    // Log.d("MyHttpPost", "String from server --->" + response);
                    return data + "user or password is not valid";

                } else {
                    Rc = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!Rc) {
                return "00000000|Generic error from getting bg values";
            }

            //-----------------------------------------------------------------------------
            // Read the response. This is important because it could be an array of values
            //-----------------------------------------------------------------------------
            String line;                                                                        // used to read from the buffer
            StringBuilder strLine = new StringBuilder();                                        // used to build a concat string

            BufferedReader inBuff = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            strLine.append("11000000|");

            while ((line = inBuff.readLine()) != null) {
                // Append server response in string
                // Log.d("MyHttpPost", "[webgetbg]" + line);
                strLine.append(line);
            }
            inBuff.close();
            data = strLine.toString();

            //Log.d("MyHttpPost", "Returning to service (bg):"  + response.getStatusLine().getStatusCode() + " data " + data);

            return data;                                                       // We are returning the string
        } catch (Exception e) {
            e.printStackTrace();
            outMessage[0] = "Could not get account information - Cloud Server may be down";
        }
        return data;                                                       // We are returning the string
    }


    String validateDextenderAccount(final URI inWebUrl) {

        //Log.d("myhttppost", "Sending to URL--->" + inWebUrl);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        int timeoutConnection = 2000;
        int timeoutSocket = 2000;

        //---------------------------------------------------
        // Set connection and socket timeouts
        //---------------------------------------------------
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

        StringBuilder result = new StringBuilder();
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
        HttpGet request = new HttpGet(inWebUrl);
        HttpResponse response = null;
        Boolean Rc = false;

        try {
            response = httpClient.execute(request);
            Rc = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!Rc) {
            outString = "cloud error";
        } else {
            InputStream input = null;
            try {
                input = new BufferedInputStream(response.getEntity().getContent());
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte data[] = new byte[128];
            int currentByteReadCount;

            /** read response from input stream */
            try {
                while ((currentByteReadCount = input.read(data)) != -1) {
                    String readData = new String(data, 0, currentByteReadCount);
                    result.append(readData);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            outString = result.toString();
        }
        return outString;
    }


    //=================================================================================
    // Forward my BG to my web site
    // Same code as above !!
    //=================================================================================
    public String forwardBg(final URI inWebUrl) {
        return validateDextenderAccount(inWebUrl);   // need to wrap it
    }


}
