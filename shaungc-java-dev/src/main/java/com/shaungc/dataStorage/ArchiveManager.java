package com.shaungc.dataStorage;

import com.google.gson.Gson;
import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.GlassdoorReviewMetadata;
import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.Logger;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

enum FileType {
    JSON("json"),
    HTML("html");

    private final String extension;

    private FileType(final String extension) {
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

    public ArchiveManager(final String orgName, final String orgId) {
        this.s3Service = new S3Service(ArchiveManager.BUCKET_NAME);
        this.s3Service.createBucket();

        this.orgName = orgName;
        this.orgId = orgId;
    }

    // meta info functions

    public String doesObjectExist(final String fullPath) {
        return this.s3Service.doesObjectExistAndGetMd5(fullPath);
    }

    public String doesGlassdoorOrganizationReviewExist(final String reviewId) {
        final String fullPathUntilFilename = this.getGlassdoorOrgReviewDataDirectory() + reviewId;
        return this.doesObjectExist(S3Service.getFullPathAsJsonFile(fullPathUntilFilename));
    }

    // path generating functions

    public String getOrganizationDirectory() {
        return this.orgName + "-" + this.orgId;
    }

    public static String getOrganizationDirectory(final String orgId, final String orgName) {
        return orgName + "-" + orgId;
    }

    public String getGlassdoorOrgReviewDataDirectory() {
        return this.getOrganizationDirectory() + "/reviews/";
    }

    public static String getGlassdoorOrgReviewDataFilenamePrefix(final String reviewId) {
        return reviewId;
    }

    public static String getGlassdoorOrgReviewDataFilename(final String reviewId) {
        // do not append timestamp for review data
        // due to its uniqueness
        return ArchiveManager.getGlassdoorOrgReviewDataFilenamePrefix(reviewId);
    }

    public static String getCollidedGlassdoorOrgReviewDataFilenamePrefix(final String reviewId) {
        return "collision." + reviewId;
    }

    public static String getCollidedGlassdoorOrgReviewDataFilename(final String reviewId) {
        return ArchiveManager.getCollidedGlassdoorOrgReviewDataFilenamePrefix(reviewId) + "." + Instant.now();
    }

    // write out functions

    /**
     * Dumps an object to S3 as json file
     *
     * @param pathUntilFilename - the full path to the file, without extension
     * @param object
     */
    public void putJsonOnS3(final String pathUntilFilename, final Object object) {
        this.s3Service.putFileOnS3(pathUntilFilename, object, FileType.JSON);
    }

    public void putHtmlOnS3(final String pathUntilFilename, final Object object) {
        this.s3Service.putFileOnS3(pathUntilFilename, object, FileType.HTML);
    }

    // public void putFileOnS3(String pathUntilFilename, Object object, FileType
    // fileType) {
    // String dumpString = S3Service.serializeJavaObjectAsJsonStyle(object);

    // if (fileType == FileType.JSON) {
    // this.s3Service.putObjectOfString(ArchiveManager.getFullPathAsJson(pathUntilFilename),
    // dumpString);
    // Logger.info("JSON dumped to path " + pathUntilFilename);
    // } else if (fileType == FileType.HTML) {
    // this.s3Service.putObjectOfString(ArchiveManager.getFullPathAsHtml(pathUntilFilename),
    // dumpString);
    // Logger.info("HTML dumped to path " + pathUntilFilename);
    // } else {
    // this.s3Service.putObjectOfString(pathUntilFilename, dumpString);
    // Logger.info("file dumped to path " + pathUntilFilename);
    // }

    // Logger.info("Dumped data:\n" + dumpString.substring(0,
    // Math.min(dumpString.length(), 100)) + "...\n");
    // }

    public void writeGlassdoorOrganizationMetadataAsJson(final BasicParsedData orgMetadata) {
        final String orgMetadataDirectory = Path.of(this.getOrganizationDirectory(), "meta").toString();

        final String filenameWithoutExtension = Instant.now().toString();

        this.s3Service.putLatestObject(orgMetadataDirectory, filenameWithoutExtension, orgMetadata, FileType.JSON);
    }

    public void writeGlassdoorOrganizationReviewsMetadataAsJson(
        final String orgId,
        final String orgName,
        final GlassdoorReviewMetadata reviewMetadata
    ) {
        final String reviewMetadataDirectory = Path.of(this.getOrganizationDirectory(), "reviews-meta").toString();

        final String filenameWithoutExtension = Instant.now().toString();

        this.s3Service.putLatestObject(reviewMetadataDirectory, filenameWithoutExtension, reviewMetadata, FileType.JSON);
    }

    public void writeGlassdoorOrganizationReviewsMetadataAsJson(final GlassdoorReviewMetadata reviewMetadata) {
        Logger.infoAlsoSlack("Local review count is " + reviewMetadata.localReviewCount + ", we will scrape within these reviews.");
        this.putJsonOnS3(this.getOrganizationDirectory() + "/reviews-meta/" + reviewMetadata.scrapedTimestamp, reviewMetadata);
    }

    /**
     * @return Whether or not a new review data is written to a file on s3
     */
    private Boolean writeReviewData(final String reviewId, final String subDirectory, final String filename, final Object data) {
        final String reviewDataDirectory = String.format("%s%s/%s/", this.getGlassdoorOrgReviewDataDirectory(), subDirectory, reviewId);

        return this.s3Service.putLatestObject(reviewDataDirectory, filename, data, FileType.JSON);
    }

    public Boolean writeGlassdoorOrganizationReviewDataAsJson(final EmployeeReviewData reviewData) {
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

    public String writeCollidedGlassdoorOrganizationReviewDataAsJson(final EmployeeReviewData reviewData) {
        final String pathUntilFilename =
            this.getGlassdoorOrgReviewDataDirectory() +
            ArchiveManager.getCollidedGlassdoorOrgReviewDataFilename(reviewData.stableReviewData.reviewId);

        this.putJsonOnS3(pathUntilFilename, reviewData);

        return S3Service.getFullPathAsJsonFile(pathUntilFilename);
    }

    public String writeHtml(final String filename, final String html) {
        final String pathUntilFilename = this.getOrganizationDirectory() + "/logs/" + filename + "." + Instant.now();
        this.putHtmlOnS3(pathUntilFilename, html);

        // return the complete path (key) so that caller can make good use
        return S3Service.getFullPathAsHtmlFile(pathUntilFilename);
    }
    // misc helper functions

}
