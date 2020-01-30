package com.shaungc.dataStorage;

import com.google.gson.Gson;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.GlassdoorReviewMetadata;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;


/**
 * ArchiveManager
 */
public class ArchiveManager {

    static S3Service s3Service = new S3Service();

    private static String BUCKET_NAME = Configuration.AWS_S3_ARCHIVE_BUCKET_NAME;
    
    // https://github.com/google/gson
    public static Gson GSON_TOOL = new Gson();

    static {
        ArchiveManager.s3Service.createBucket(ArchiveManager.BUCKET_NAME);
    }

    public String orgName;
    public String orgId;
    
    public ArchiveManager(String orgName, String orgId) {
        this.orgName = orgName;
        this.orgId = orgId;
    }
    public ArchiveManager() {}

    // meta info functions

    static public Boolean doesObjectExist(String fullPath) {
        return ArchiveManager.s3Service.doesObjectExist(ArchiveManager.BUCKET_NAME, fullPath);
    }

    public Boolean doesGlassdoorOrganizationReviewExist(String reviewId) {
        String fullPathUntilFilename = this.getGlassdoorOrgReviewDataDirectory() + reviewId;
        return ArchiveManager.doesObjectExist(ArchiveManager.getFullPath(fullPathUntilFilename));
    }

    // getter functions

    static private String getFullPath(String fullPathUntilFilename) {
        return fullPathUntilFilename + ".json";
    }

    // path generating functions
    public String getOrganizationDirectory() {
        return this.orgName + "-" + this.orgId;
    }
    static public String getOrganizationDirectory(String orgId, String orgName) {
        return orgName + "-" + orgId;
    }

    public String getGlassdoorOrgReviewDataDirectory() {
        return this.getOrganizationDirectory() + "/reviews/";
    }

    // write out functions

    static public void jsonDump(String pathUntilFilename, Object object) {
        String dumpString = ArchiveManager.serializeJavaObject(object);

        ArchiveManager.s3Service.putObjectOfString(ArchiveManager.BUCKET_NAME, ArchiveManager.getFullPath(pathUntilFilename), dumpString);

        Logger.info("JSON dumped to path " + pathUntilFilename);

        Logger.info("Dumped data:\n" + dumpString.substring(0, Math.min(dumpString.length(), 100)) + "...\n");
    }

    static public void writeGlassdoorOrganizationMetadata(String orgId, String orgName, BasicParsedData orgMetadata) {
        ArchiveManager.jsonDump(ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/meta/" + orgMetadata.scrapedTimestamp, orgMetadata);
    }
    public void writeGlassdoorOrganizationMetadata(BasicParsedData orgMetadata) {
        ArchiveManager.jsonDump(this.getOrganizationDirectory() + "/meta/" + orgMetadata.scrapedTimestamp, orgMetadata);
    }

    static public void writeGlassdoorOrganizationReviewsMetadata(String orgId, String orgName, GlassdoorReviewMetadata reviewMetadata) {
        ArchiveManager.jsonDump(ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/reviews-meta/" + reviewMetadata.scrapedTimestamp, reviewMetadata);
    }
    public void writeGlassdoorOrganizationReviewsMetadata(GlassdoorReviewMetadata reviewMetadata) {
        Logger.infoAlsoSlack("Local review count is " + reviewMetadata.localReviewCount + ", we will scrape within these reviews.");
        ArchiveManager.jsonDump(this.getOrganizationDirectory() + "/reviews-meta/" + reviewMetadata.scrapedTimestamp, reviewMetadata);
    }

    static public void writeGlassdoorOrganizationReviewData(String orgId, String orgName, EmployeeReviewData reviewData) {
        ArchiveManager.jsonDump(ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/reviews/" + reviewData.reviewId, reviewData);
    }
    public void writeGlassdoorOrganizationReviewData(EmployeeReviewData reviewData) {
        ArchiveManager.jsonDump(this.getOrganizationDirectory() + "/reviews/" + reviewData.reviewId, reviewData);
    }

    // misc helper functions
    static public String serializeJavaObject(Object object) {
        return ArchiveManager.GSON_TOOL.toJson(object);
    }
}