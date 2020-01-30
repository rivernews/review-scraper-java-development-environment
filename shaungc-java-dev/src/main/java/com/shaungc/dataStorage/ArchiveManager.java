package com.shaungc.dataStorage;

import com.google.gson.Gson;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.GlassdoorReviewMetadata;
import com.shaungc.javadev.Configuration;
import com.shaungc.javadev.Logger;


/**
 * ArchiveManager
 */
public class ArchiveManager {

    static S3Service s3Service = new S3Service();

    private static String BUCKET_NAME = Configuration.AWS_S3_ARCHIVE_BUCKET_NAME;
    
    // https://github.com/google/gson
    private static Gson GSON_TOOL = new Gson();

    static {
        ArchiveManager.s3Service.createBucket(ArchiveManager.BUCKET_NAME);
    }

    public String orgName;
    
    public ArchiveManager(String orgName) {
        this.orgName = orgName;
    }
    public ArchiveManager() {}

    // TODO: company C/R/U/(D) operations
    static public void jsonDump(String pathUntilFilename, Object object) {
        String dumpString = ArchiveManager.GSON_TOOL.toJson(object);

        ArchiveManager.s3Service.putObjectOfString(ArchiveManager.BUCKET_NAME, pathUntilFilename + ".json", dumpString);

        Logger.info("JSON dumped to path " + pathUntilFilename);

        Logger.info("Dumped data:\n" + dumpString.substring(0, Math.min(dumpString.length(), 500)) + "...\n");
    }

    static public Boolean doesObjectExist(String fullPath) {
        return ArchiveManager.s3Service.doesObjectExist(ArchiveManager.BUCKET_NAME, fullPath);
    }

    static public void writeGlassdoorOrganizationMetadata(String orgName, BasicParsedData orgMetadata) {
        ArchiveManager.jsonDump(orgName + "/meta/" + orgMetadata.scrapedTimestamp, orgMetadata);
    }
    public void writeGlassdoorOrganizationMetadata(BasicParsedData orgMetadata) {
        ArchiveManager.jsonDump(this.orgName + "/meta/" + orgMetadata.scrapedTimestamp, orgMetadata);
    }


    static public void writeGlassdoorOrganizationReviewsMetadata(String orgName, GlassdoorReviewMetadata reviewMetadata) {
        ArchiveManager.jsonDump(orgName + "/reviews-meta/" + reviewMetadata.scrapedTimestamp, reviewMetadata);
    }
    public void writeGlassdoorOrganizationReviewsMetadata(GlassdoorReviewMetadata reviewMetadata) {
        ArchiveManager.jsonDump(this.orgName + "/reviews-meta/" + reviewMetadata.scrapedTimestamp, reviewMetadata);
    }

    static public void writeGlassdoorOrganizationReviewData(String orgName, EmployeeReviewData reviewData) {
        ArchiveManager.jsonDump(orgName + "/reviews/" + reviewData.reviewId, reviewData);
    }
    public void writeGlassdoorOrganizationReviewData(EmployeeReviewData reviewData) {
        ArchiveManager.jsonDump(this.orgName + "/reviews/" + reviewData.reviewId, reviewData);
    }
}