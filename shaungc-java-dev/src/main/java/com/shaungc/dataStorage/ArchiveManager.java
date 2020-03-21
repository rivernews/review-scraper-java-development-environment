package com.shaungc.dataStorage;

import com.shaungc.dataTypes.BasicParsedData;
import com.shaungc.dataTypes.EmployeeReviewData;
import com.shaungc.dataTypes.GlassdoorReviewMetadata;
import com.shaungc.javadev.Configuration;
import com.shaungc.utilities.HttpService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import software.amazon.awssdk.regions.Region;

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

    private static final String BUCKET_NAME = Configuration.AWS_S3_ARCHIVE_BUCKET_NAME;
    private static final Region BUCKET_REGION = Region.US_WEST_2;

    private static String BUKET_BASE_URL = String.format("https://s3.console.aws.amazon.com/s3/buckets/%s/", ArchiveManager.BUCKET_NAME);

    public static String BUCKET_URL = String.format(
        "%s?region=%s&tab=overview",
        ArchiveManager.BUKET_BASE_URL,
        ArchiveManager.BUCKET_REGION
    );

    public String orgName;
    public String orgId;

    // s3 does not allow a `/` at front; if you do so, you will get an empty-named folder
    private final String gdOrgOverviewPageUrlsDirectory = Path.of("all-urls").toString();

    public ArchiveManager() {
        this.s3Service = new S3Service(ArchiveManager.BUCKET_NAME, ArchiveManager.BUCKET_REGION);
        this.s3Service.createBucket();
        // orgName and orgId will be set after org meta is scraped
    }

    public ArchiveManager(final String orgName, final String orgId) {
        this.s3Service = new S3Service(ArchiveManager.BUCKET_NAME, ArchiveManager.BUCKET_REGION);
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

    public String getFullUrlOnS3FromFilePathBasedOnOrgDirectory(final String filePathBasedOnOrgDirectory) {
        final String urlString = String.format(
            "https://%s.s3-%s.amazonaws.com/%s",
            ArchiveManager.BUCKET_NAME,
            ArchiveManager.BUCKET_REGION,
            filePathBasedOnOrgDirectory
        );

        return HttpService.encodeUrl(urlString);
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

    public void writeGlassdoorOrganizationMetadataAsJson(final BasicParsedData orgMetadata) {
        final String orgMetadataDirectory = Path.of(this.getOrganizationDirectory(), "meta").toString();

        final String filenameWithoutExtension = Instant.now().toString();

        this.s3Service.putLatestObject(orgMetadataDirectory, filenameWithoutExtension, orgMetadata, FileType.JSON);

        // also write company overview page url
        // no need to check exist or not, just overwrite is fine
        final String orgOverviewPageUrlObjectKey = Path.of(this.gdOrgOverviewPageUrlsDirectory, this.getOrganizationDirectory()).toString();

        this.s3Service.putObjectOfString(orgOverviewPageUrlObjectKey, orgMetadata.companyOverviewPageUrl);
    }

    public void writeGlassdoorOrganizationReviewsMetadataAsJson(final GlassdoorReviewMetadata reviewMetadata) {
        final String reviewMetadataDirectory = Path.of(this.getOrganizationDirectory(), "reviews-meta").toString();

        final String filenameWithoutExtension = Instant.now().toString();

        this.s3Service.putLatestObject(reviewMetadataDirectory, filenameWithoutExtension, reviewMetadata, FileType.JSON);
    }

    /**
     * @return Whether or not a new review data is written to a file on s3
     */
    private Boolean writeReviewData(final String reviewId, final String subDirectory, final String filename, final Object data) {
        final String reviewDataDirectory = Path.of(this.getGlassdoorOrgReviewDataDirectory(), subDirectory, reviewId).toString();

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
