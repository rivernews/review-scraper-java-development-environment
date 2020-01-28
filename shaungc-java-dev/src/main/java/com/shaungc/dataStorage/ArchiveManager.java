package com.shaungc.dataStorage;

import com.google.gson.Gson;
import com.shaungc.javadev.Configuration;
import com.shaungc.javadev.Logger;


/**
 * ArchiveManager
 */
public class ArchiveManager {

    S3Service s3Service;

    private static String BUCKET_NAME = Configuration.AWS_S3_ARCHIVE_BUCKET_NAME;
    
    // https://github.com/google/gson
    private static Gson GSON_TOOL = new Gson();

    public ArchiveManager() {
        this.s3Service = new S3Service();
        s3Service.createBucket(ArchiveManager.BUCKET_NAME);
    }

    // TODO: company C/R/U/(D) operations
    public void jsonDump(String pathUntilFilename, Object object) {
        String dumpString = ArchiveManager.GSON_TOOL.toJson(object);

        this.s3Service.putObjectOfString(ArchiveManager.BUCKET_NAME, pathUntilFilename + ".json", dumpString);

        Logger.info("JSON dumped to path " + pathUntilFilename);

        Logger.info("Dumped data:\n" + dumpString.substring(0, Math.min(dumpString.length(), 500)) + "...\n");
    }
}