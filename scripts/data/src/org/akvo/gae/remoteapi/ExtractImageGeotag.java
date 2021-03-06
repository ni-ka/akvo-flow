/*
 *  Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.gae.remoteapi;

import static org.akvo.gae.remoteapi.DataUtils.batchSaveEntities;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.waterforpeople.mapping.domain.response.value.Location;
import org.waterforpeople.mapping.domain.response.value.Media;
import org.waterforpeople.mapping.serialization.response.MediaResponse;

import com.gallatinsystems.common.util.S3Util;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.Directory;
import com.drew.metadata.exif.GpsDirectory;

/*
 * - Bring question answers up to date
 */
public class ExtractImageGeotag implements Process {

    private boolean doIt = false;
    private String S3bucket;
    private String S3id;
    private String S3secret;

    @Override
    public void execute(DatastoreService ds, String[] args) throws Exception {

        if (args.length >= 3 ) {
            S3bucket = args[0];
            S3id = args[1];
            S3secret = args[2];
            if (args.length == 4 && args[3].equals("--doit")) {
                doIt = true;
            }
        } else {
            System.out.printf("#Arguments: S3-bucketname S3-id S3-secret [--doit to execute]\n");
            return;
        }
        System.out.printf("#S3-bucketname %s, S3-id %s, S3-secret %s\n", S3bucket, S3id, S3secret);
        File tmpdir = new File("/tmp/exif/");
        tmpdir.mkdir();
        processQuestions(ds);
    }

    private void processQuestions(DatastoreService ds) throws ParseException {
        System.out.println("#Processing Questions from 2018 and later");
        int total = 0;
        int nontagged = 0;
        int json = 0;
        int nonjson = 0;
        int tagged = 0;
        SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd");
        Date tooOld = df.parse("2018-01-01");

        final Query qq = new Query("QuestionAnswerStore").setFilter(new Query.FilterPredicate("type", FilterOperator.EQUAL, "IMAGE"));
        final PreparedQuery qpq = ds.prepare(qq);
        List<Entity> questionsToFix = new ArrayList<Entity>();

        for (Entity q : qpq.asIterable(FetchOptions.Builder.withChunkSize(500))) {
            total++;
            boolean forceSave = false;
            Media media;
            String v = (String) q.getProperty("value"); //Assume valueText was not used for this image
            Date collected = (Date) q.getProperty("collectionDate");
            if (collected.before(tooOld)) continue; //Debug! Concentrate on recent data.

            System.out.printf(" Old IMAGE value '%s'\n", v);
            if (v != null && !v.trim().equals("")) {
                if (v.startsWith("{")) {
                    json++;
                    //Parse it
                    media = MediaResponse.parse(v);
                    if (media.getLocation() != null) { //Best case: Already known (could check validity)
                        //System.out.println("Location in " + q);
                        tagged++;
                        continue; //Skip
                    }
                    //also want to skip if location is present, but null, to avoid re-evaluation
                    if (v.matches("\"location\":null")) {
                        System.out.printf("Null location in IMAGE %d: '%s'\n", q.getKey().getId(), v);
                        tagged++;
                        continue; //Skip
                    }
                } else {
                    nonjson++;
                    forceSave = true;//handle legacy values: convert them to JSON while we're here
                    v = Paths.get(v).getFileName().toString(); //strip path, it is never used
                    media = new Media();
                    media.setFilename(v);
                }
            } else {
                System.out.printf("#ERROR null or empty value for IMAGE %d: '%s'\n", q.getKey().getId(), v);
                continue; //TODO convert this to JSON too?
            }
            //No location known; must read the file
            Location loc = new Location();
            Boolean tagFound = fetchLocationFromJpegInS3(media.getFilename(), loc);
            if (tagFound != null || forceSave) {

                if (tagFound == null) { // We cannot know (right now - file may arrive later)
                    v = MediaResponse.formatWithoutGeotag(media);
                } else { //We do know!
                    if (tagFound.equals(Boolean.FALSE)) {
                        loc = null; //There is no tag!
                    }
                    media.setLocation(loc);
                    v = MediaResponse.formatWithGeotag(media);
                }
                System.out.printf(" New IMAGE value '%s'\n", v);
                q.setProperty("value", v);
                questionsToFix.add(q);
                if (doIt && questionsToFix.size() >= 250) {
                    // save once we have 250, so that an occasional
                    // db failure will not ruin everything
                    System.out.printf("#Fixing %d Questions\n", questionsToFix.size());
                    batchSaveEntities(ds, questionsToFix);
                    questionsToFix.clear();
                }
            }
        }
        System.out.println("Found " + total + " images.");
        System.out.println("  JSON " + json + " answers.");
        System.out.println("  Non-JSON " + nonjson + " answers.");
        if (doIt) {
            System.out.printf("#Fixing last %d Questions of %d\n", questionsToFix.size(), total);
            batchSaveEntities(ds, questionsToFix);
        } else {
            System.out.println("This was a dry run. " + questionsToFix.size() + " changes not saved to datastore");
        }
    }

    /*
     * Sample exif command output:
[GPS] GPS Latitude - 59/1 17/1 25324/1000
[GPS] GPS Longitude - 17/1 57/1 7827/1000
[GPS] GPS Altitude - 0 metres
[GPS] GPS Time-Stamp - 00:42:48.000 UTC
[GPS] GPS Processing Method - NETWORK
[GPS] GPS Date Stamp - 2013:04:13

     */
    Boolean fetchLocationFromJpegInS3(String filename, Location loc) {
        InputStream s = fetchImageFileFromS3(filename);
        if (s != null) {
            try {
                Metadata metadata = JpegMetadataReader.readMetadata(s);
                System.out.println("Using JpegMetadataReader");
                Directory directory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                if (directory == null) { //No GPS tag
                    return false;
                }

                Rational[] latTag = directory.getRationalArray(GpsDirectory.TAG_LATITUDE);
                String latRefTag = directory.getString(GpsDirectory.TAG_LATITUDE_REF);
                Rational[] lonTag = directory.getRationalArray(GpsDirectory.TAG_LONGITUDE);
                String lonRefTag = directory.getString(GpsDirectory.TAG_LONGITUDE_REF);
                Rational[] altTag = directory.getRationalArray(GpsDirectory.TAG_ALTITUDE);
                Integer altRefTag = directory.getInteger(GpsDirectory.TAG_ALTITUDE_REF);
                Rational[] accTag = directory.getRationalArray(GpsDirectory.TAG_H_POSITIONING_ERROR);
                if (latTag == null || lonTag == null) {
                    return false; //Bad GPS tag
                }
                Double lat = latTag[0].doubleValue() + latTag[1].doubleValue()/60.0 + latTag[2].doubleValue()/3600.0;
                if (latRefTag != null && latRefTag.contentEquals("S")) {
                    lat = -lat;
                }
                Double lon = lonTag[0].doubleValue() + lonTag[1].doubleValue()/60.0 + lonTag[2].doubleValue()/3600.0;
                if (lonRefTag != null && lonRefTag.contentEquals("W")) {
                    lon = -lon;
                }
                if (lat == 0.0 || lon == 0.0) {
                    return false; //While technically valid, treat as Bad GPS tag
                }
                Double alt;
                if (altTag != null) {
                    alt = altTag[0].doubleValue();
                } else {
                    alt = 0.0; //Optional; default to 0
                }
                if (altRefTag != null && altRefTag.equals(1)) { //0 = above, 1 below sea level
                    alt = -alt;
                }
                Float acc;
                if (accTag != null) {
                    acc = accTag[0].floatValue();
                } else {
                    acc = 0.0f; //Optional; default to 0
                }
                loc.setLatitude(lat);
                loc.setLongitude(lon);
                loc.setAltitude(alt);
                loc.setAccuracy(acc);
                System.out.printf("#  Extracted location N %f, E %f, up %f, acc %f\n", lat, lon, alt, acc);//Debug
                return true;
            } catch (JpegProcessingException e) {
                System.out.println(e);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        return null; //Can't tell
    }


    InputStream fetchImageFileFromS3(String filename) {
        // attempt to retrieve image file
        URLConnection conn = null;
        String s3bucket = com.gallatinsystems.common.util.PropertyUtil.getProperty("s3bucket");
        filename = Paths.get(filename).getFileName().toString(); //strip path, it is not used in S3
        System.out.println("Fetching " + filename);

        try {
            conn = S3Util.getConnection(S3bucket, "images/" + filename, S3id, S3secret);
            return new BufferedInputStream(conn.getInputStream());
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
