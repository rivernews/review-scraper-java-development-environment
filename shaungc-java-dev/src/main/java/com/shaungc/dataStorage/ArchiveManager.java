package com.shaungc.dataStorage;

import java.util.Date;

import com.google.gson.Gson;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.GlassdoorReviewMetadata;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;


enum FileType {
    JSON("json"),
    HTML("html");

    private final String extension;

    private FileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return this.extension;
    }
}


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
        return ArchiveManager.doesObjectExist(ArchiveManager.getFullPathAsJson(fullPathUntilFilename));
    }

    // getter functions

    static private String getFullPathAsJson(String fullPathUntilFilename) {
        return fullPathUntilFilename + FileType.JSON.getExtension();
    }
    static private String getFullPathAsHtml(String fullPathUntilFilename) {
        return fullPathUntilFilename + FileType.HTML.getExtension();
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
        ArchiveManager.fileDump(pathUntilFilename, object, FileType.JSON);
    }

    static public void htmlDump(String pathUntilFilename, Object object) {
        ArchiveManager.fileDump(pathUntilFilename, object, FileType.HTML);
    }

    static public void fileDump(String pathUntilFilename, Object object, FileType fileType) {
        String dumpString = ArchiveManager.serializeJavaObject(object);

        if (fileType == FileType.JSON) {
            ArchiveManager.s3Service.putObjectOfString(ArchiveManager.BUCKET_NAME, ArchiveManager.getFullPathAsJson(pathUntilFilename), dumpString);
            Logger.info("JSON dumped to path " + pathUntilFilename);
        } else if (fileType == FileType.HTML) {
            ArchiveManager.s3Service.putObjectOfString(ArchiveManager.BUCKET_NAME, ArchiveManager.getFullPathAsHtml(pathUntilFilename), dumpString);
            Logger.info("HTML dumped to path " + pathUntilFilename);
        } else {
            ArchiveManager.s3Service.putObjectOfString(ArchiveManager.BUCKET_NAME, pathUntilFilename, dumpString);
            Logger.info("file dumped to path " + pathUntilFilename);
        }

        Logger.info("Dumped data:\n" + dumpString.substring(0, Math.min(dumpString.length(), 100)) + "...\n");
    }

    static public void writeGlassdoorOrganizationMetadataAsJson(String orgId, String orgName, BasicParsedData orgMetadata) {
        ArchiveManager.jsonDump(ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/meta/" + orgMetadata.scrapedTimestamp, orgMetadata);
    }
    public void writeGlassdoorOrganizationMetadataAsJson(BasicParsedData orgMetadata) {
        ArchiveManager.jsonDump(this.getOrganizationDirectory() + "/meta/" + orgMetadata.scrapedTimestamp, orgMetadata);
    }

    static public void writeGlassdoorOrganizationReviewsMetadataAsJson(String orgId, String orgName, GlassdoorReviewMetadata reviewMetadata) {
        ArchiveManager.jsonDump(ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/reviews-meta/" + reviewMetadata.scrapedTimestamp, reviewMetadata);
    }
    public void writeGlassdoorOrganizationReviewsMetadataAsJson(GlassdoorReviewMetadata reviewMetadata) {
        Logger.infoAlsoSlack("Local review count is " + reviewMetadata.localReviewCount + ", we will scrape within these reviews.");
        ArchiveManager.jsonDump(this.getOrganizationDirectory() + "/reviews-meta/" + reviewMetadata.scrapedTimestamp, reviewMetadata);
    }

    static public void writeGlassdoorOrganizationReviewDataAsJson(String orgId, String orgName, EmployeeReviewData reviewData) {
        ArchiveManager.jsonDump(ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/reviews/" + reviewData.reviewId, reviewData);
    }
    public void writeGlassdoorOrganizationReviewDataAsJson(EmployeeReviewData reviewData) {
        ArchiveManager.jsonDump(this.getOrganizationDirectory() + "/reviews/" + reviewData.reviewId, reviewData);
    }
    public void writeCollidedGlassdoorOrganizationReviewDataAsJson(EmployeeReviewData reviewData) {
        final String filename = "collision." + reviewData.reviewId + "." + new Date();
        ArchiveManager.jsonDump(this.getOrganizationDirectory() + "/reviews/" + filename, reviewData);
    }

    public String writeHtml(String filename, String html) {
        final String pathUntilFilename = this.getOrganizationDirectory() + "/logs/" + filename + "." + new Date();
        ArchiveManager.htmlDump(pathUntilFilename, html);
        
        // return the complete path (key) so that caller can make good use
        return ArchiveManager.getFullPathAsHtml(pathUntilFilename);
    }

    // misc helper functions
    static public String serializeJavaObject(Object object) {
        return ArchiveManager.GSON_TOOL.toJson(object);
    }
}