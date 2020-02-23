package com.shaungc.dataStorage;

import com.google.gson.Gson;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.GlassdoorReviewMetadata;
import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;
import java.time.Instant;
import java.util.Date;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

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
    public final S3Service s3Service;

    private static String BUCKET_NAME = Configuration.AWS_S3_ARCHIVE_BUCKET_NAME;

    public String orgName;
    public String orgId;

    public ArchiveManager() {
        this.s3Service = new S3Service(ArchiveManager.BUCKET_NAME);
        this.s3Service.createBucket();
    }

    // meta info functions

    public String doesObjectExist(String fullPath) {
        return this.s3Service.doesObjectExistAndGetMd5(fullPath);
    }

    public String doesGlassdoorOrganizationReviewExist(String reviewId) {
        String fullPathUntilFilename = this.getGlassdoorOrgReviewDataDirectory() + reviewId;
        return this.doesObjectExist(ArchiveManager.getFullPathAsJson(fullPathUntilFilename));
    }

    public Boolean isLatestObjectTheSame(Object object, String objectCollectionS3Prefix) {
        // no previous object exists

        // exist and latest object content is the same

        // exist but latest object content is not the same

        return false;
    }

    // getter functions

    private static String getFullPathAsJson(String fullPathUntilFilename) {
        return fullPathUntilFilename + "." + FileType.JSON.getExtension();
    }

    private static String getFullPathAsHtml(String fullPathUntilFilename) {
        return fullPathUntilFilename + "." + FileType.HTML.getExtension();
    }

    // path generating functions

    public String getOrganizationDirectory() {
        return this.orgName + "-" + this.orgId;
    }

    public static String getOrganizationDirectory(String orgId, String orgName) {
        return orgName + "-" + orgId;
    }

    public String getGlassdoorOrgReviewDataDirectory() {
        return this.getOrganizationDirectory() + "/reviews/";
    }

    public static String getGlassdoorOrgReviewDataFilenamePrefix(String reviewId) {
        return reviewId;
    }

    public static String getGlassdoorOrgReviewDataFilename(String reviewId) {
        // do not append timestamp for review data
        // due to its uniqueness
        return ArchiveManager.getGlassdoorOrgReviewDataFilenamePrefix(reviewId);
    }

    public static String getCollidedGlassdoorOrgReviewDataFilenamePrefix(String reviewId) {
        return "collision." + reviewId;
    }

    public static String getCollidedGlassdoorOrgReviewDataFilename(String reviewId) {
        return ArchiveManager.getCollidedGlassdoorOrgReviewDataFilenamePrefix(reviewId) + "." + Instant.now();
    }

    // write out functions

    /**
     * Dumps an object to S3 as json file
     *
     * @param pathUntilFilename - the full path to the file, without extension
     * @param object
     */
    public void jsonDump(String pathUntilFilename, Object object) {
        this.fileDump(pathUntilFilename, object, FileType.JSON);
    }

    public void htmlDump(String pathUntilFilename, Object object) {
        this.fileDump(pathUntilFilename, object, FileType.HTML);
    }

    public void fileDump(String pathUntilFilename, Object object, FileType fileType) {
        String dumpString = S3Service.serializeJavaObject(object);

        if (fileType == FileType.JSON) {
            this.s3Service.putObjectOfString(ArchiveManager.getFullPathAsJson(pathUntilFilename), dumpString);
            Logger.info("JSON dumped to path " + pathUntilFilename);
        } else if (fileType == FileType.HTML) {
            this.s3Service.putObjectOfString(ArchiveManager.getFullPathAsHtml(pathUntilFilename), dumpString);
            Logger.info("HTML dumped to path " + pathUntilFilename);
        } else {
            this.s3Service.putObjectOfString(pathUntilFilename, dumpString);
            Logger.info("file dumped to path " + pathUntilFilename);
        }

        Logger.info("Dumped data:\n" + dumpString.substring(0, Math.min(dumpString.length(), 100)) + "...\n");
    }

    // public void writeGlassdoorOrganizationMetadataAsJson(String orgId, String orgName, BasicParsedData orgMetadata) {
    //     this.jsonDump(
    //         ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/meta/" + orgMetadata.scrapedTimestamp,
    //         orgMetadata
    //     );
    // }

    public void writeGlassdoorOrganizationMetadataAsJson(BasicParsedData orgMetadata) {
        this.jsonDump(this.getOrganizationDirectory() + "/meta/" + orgMetadata.scrapedTimestamp, orgMetadata);
    }

    public void writeGlassdoorOrganizationReviewsMetadataAsJson(String orgId, String orgName, GlassdoorReviewMetadata reviewMetadata) {
        this.jsonDump(
                ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/reviews-meta/" + reviewMetadata.scrapedTimestamp,
                reviewMetadata
            );
    }

    public void writeGlassdoorOrganizationReviewsMetadataAsJson(GlassdoorReviewMetadata reviewMetadata) {
        Logger.infoAlsoSlack("Local review count is " + reviewMetadata.localReviewCount + ", we will scrape within these reviews.");
        this.jsonDump(this.getOrganizationDirectory() + "/reviews-meta/" + reviewMetadata.scrapedTimestamp, reviewMetadata);
    }

    // public static void writeGlassdoorOrganizationReviewDataAsJson(String orgId, String orgName, EmployeeReviewData reviewData) {
    //     ArchiveManager.jsonDump(ArchiveManager.getOrganizationDirectory(orgId, orgName) + "/reviews/" + reviewData.reviewId, reviewData);
    // }

    // public void writeGlassdoorOrganizationReviewDataAsJson(EmployeeReviewData reviewData) {
    //     ArchiveManager.jsonDump(
    //         this.getGlassdoorOrgReviewDataDirectory() + ArchiveManager.getGlassdoorOrgReviewDataFilename(reviewData.reviewId),
    //         reviewData
    //     );
    // }
    private Boolean writeReviewData(String reviewId, String subDirectory, String filename, Object data) {
        final String reviewDataDirectory = String.format("%s%s/%s/", this.getGlassdoorOrgReviewDataDirectory(), subDirectory, reviewId);
        final String pathUntilFilename = reviewDataDirectory + filename;
        final String latestObjectKey = this.s3Service.getLatestObjectKey(ArchiveManager.BUCKET_NAME, reviewDataDirectory);

        // only if not exist, or md5 not the same, do we need to write
        Boolean willWriteToS3 = false;
        if (latestObjectKey == null) {
            willWriteToS3 = true;
        } else {
            final String md5OnS3 = this.s3Service.getObjectMd5(latestObjectKey);

            if (md5OnS3.strip().isEmpty()) {
                throw new ScraperShouldHaltException("No md5 at key " + latestObjectKey);
            }

            willWriteToS3 = (!S3Service.toMD5Base64String(data).equals(md5OnS3));
        }

        if (willWriteToS3) {
            this.jsonDump(pathUntilFilename, data);
            Logger.info(subDirectory + ", new object or object md5 not the same, writing to s3. Review id: " + reviewId);
            return true;
        } else {
            Logger.debug(subDirectory + ", object md5 identical, will not write. Review id: " + reviewId);
            return false;
        }
    }

    public Boolean writeGlassdoorOrganizationReviewDataAsJson(EmployeeReviewData reviewData) {
        final String filename = Instant.now().toString();

        final String varyingDataDirectoryName = "varying";
        final String stableDataDirectoryName = "stable";

        // consider varying data (e.g. helpful count, ...)
        final Boolean writtenVaryingData =
            this.writeReviewData(reviewData.stableReviewData.reviewId, varyingDataDirectoryName, filename, reviewData.varyingReviewData);
        // consider stable data (e.g. review text, ...)
        final Boolean writtenStableData =
            this.writeReviewData(reviewData.stableReviewData.reviewId, stableDataDirectoryName, filename, reviewData.stableReviewData);

        return writtenStableData || writtenVaryingData;
    }

    public String writeCollidedGlassdoorOrganizationReviewDataAsJson(EmployeeReviewData reviewData) {
        final String pathUntilFilename =
            this.getGlassdoorOrgReviewDataDirectory() +
            ArchiveManager.getCollidedGlassdoorOrgReviewDataFilename(reviewData.stableReviewData.reviewId);

        this.jsonDump(pathUntilFilename, reviewData);

        return ArchiveManager.getFullPathAsJson(pathUntilFilename);
    }

    public String writeHtml(String filename, String html) {
        final String pathUntilFilename = this.getOrganizationDirectory() + "/logs/" + filename + "." + Instant.now();
        this.htmlDump(pathUntilFilename, html);

        // return the complete path (key) so that caller can make good use
        return ArchiveManager.getFullPathAsHtml(pathUntilFilename);
    }
    // misc helper functions

}
