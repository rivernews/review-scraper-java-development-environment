package com.shaungc.dataStorage;

import com.google.gson.Gson;
import com.shaungc.exceptions.ScraperShouldHaltException;
import com.shaungc.utilities.Logger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

/**
 * S3Service
 */
public class S3Service {
    private final S3Client s3;

    private final String bucketName;

    // S3 Canned ACL:
    // https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#canned-acl
    static BucketCannedACL BUCKET_ACCESS_CANNED_ACL = BucketCannedACL.PRIVATE;

    // https://github.com/google/gson
    public static Gson GSON_TOOL = new Gson();

    public S3Service(final String buketName) {
        s3 = S3Client.builder().region(Region.US_WEST_2).build();
        this.bucketName = buketName;
    }

    protected void createBucket() {
        Logger.info("About to create S3 bucket...");
        try {
            s3.createBucket(
                CreateBucketRequest
                    .builder()
                    .bucket(this.bucketName)
                    .acl(S3Service.BUCKET_ACCESS_CANNED_ACL)
                    .createBucketConfiguration(CreateBucketConfiguration.builder().build())
                    // seems like using default will already set to private
                    .build()
            );

            // enable public access block
            // to prevent accidentally allowing public access
            s3.putPublicAccessBlock(
                PutPublicAccessBlockRequest
                    .builder()
                    .bucket(this.bucketName)
                    .publicAccessBlockConfiguration(
                        PublicAccessBlockConfiguration
                            .builder()
                            .blockPublicAcls(true)
                            .ignorePublicAcls(true)
                            .blockPublicPolicy(true)
                            .restrictPublicBuckets(true)
                            .build()
                    )
                    .build()
            );

            Logger.info("Bucket created using default configuration: " + this.bucketName);
        } catch (final BucketAlreadyOwnedByYouException e) {
            Logger.info("Bucket already own by you, will do nothing");
        } catch (final BucketAlreadyExistsException e) {
            Logger.info("Bucket name used by others and must be corrected first: " + this.bucketName);
            throw e;
        } catch (final Exception e) {
            Logger.info("Unknown error occured while using the bucket name " + this.bucketName);
            throw e;
        }

        // here we ensure the bucket exist
        // set tag on bucket
        s3.putBucketTagging(
            PutBucketTaggingRequest
                .builder()
                .bucket(this.bucketName)
                .tagging(Tagging.builder().tagSet(Tag.builder().key("costGroup").value("scraperJob").build()).build())
                .build()
        );
    }

    public static String serializeJavaObjectAsJsonStyle(final Object object) {
        return S3Service.GSON_TOOL.toJson(object);
    }

    public static String toMD5Base64String(final Object object) {
        if (object instanceof String) {
            return BinaryUtils.toBase64(Md5Utils.computeMD5Hash(((String) object).getBytes(StandardCharsets.UTF_8)));
        } else {
            return BinaryUtils.toBase64(
                Md5Utils.computeMD5Hash(S3Service.serializeJavaObjectAsJsonStyle(object).getBytes(StandardCharsets.UTF_8))
            );
        }
    }

    protected void putObjectOfString(final String key, final String content, final Boolean checkMd5OnS3IsOutdatedFirst) {
        if (checkMd5OnS3IsOutdatedFirst) {
            // check if we need to make request first -
            // if already exists, and md5 is same as our data, then do nothing
            // only if not exist, or md5 not the same, do we need to write
            final String md5OnS3 = this.doesObjectExistAndGetMd5(key);
            if (md5OnS3 != null) {
                if (md5OnS3.isEmpty()) {
                    throw new ScraperShouldHaltException(this.getNoMd5ErrorMessage(key));
                }

                if (md5OnS3.equals(S3Service.toMD5Base64String(content))) {
                    Logger.info("object at key " + key + " has identical md5, will skip writing.");
                    return;
                }
            }
        }

        // Prepare md5
        final HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put("md5", S3Service.toMD5Base64String(content));

        // Put Object
        this.s3.putObject(
                PutObjectRequest.builder().bucket(this.bucketName).key(key).metadata(metadata).build(),
                RequestBody.fromString(content)
            );
    }

    protected void putObjectOfString(final String key, final String content) {
        this.putObjectOfString(key, content, false);
    }

    public void putFileOnS3(final String pathUntilFilename, final Object object, final FileType fileType) {
        String dumpDataAsString;
        if (fileType == FileType.JSON) {
            dumpDataAsString = S3Service.serializeJavaObjectAsJsonStyle(object);
            this.putObjectOfString(S3Service.getFullPathAsJsonFile(pathUntilFilename), dumpDataAsString);
        } else if (fileType == FileType.HTML) {
            dumpDataAsString = object.toString();
            this.putObjectOfString(S3Service.getFullPathAsHtmlFile(pathUntilFilename), dumpDataAsString);
        } else {
            // default serialize by json style, but not adding any extension
            dumpDataAsString = S3Service.serializeJavaObjectAsJsonStyle(object);
            this.putObjectOfString(pathUntilFilename, dumpDataAsString);
        }

        Logger.info(fileType + " dumped to path " + String.join(".", pathUntilFilename, fileType.getExtension()));
        Logger.info("Dumped data:\n" + dumpDataAsString.substring(0, Math.min(dumpDataAsString.length(), 100)) + "...\n");
    }

    // getter functions

    public static String getFullPathAsJsonFile(String fullPathUntilFilename) {
        if (fullPathUntilFilename.toLowerCase().endsWith("." + FileType.JSON.getExtension().toLowerCase())) {
            return fullPathUntilFilename;
        }

        return String.join(".", fullPathUntilFilename, FileType.JSON.getExtension());
    }

    public static String getFullPathAsHtmlFile(String fullPathUntilFilename) {
        if (fullPathUntilFilename.toLowerCase().endsWith("." + FileType.HTML.getExtension().toLowerCase())) {
            return fullPathUntilFilename;
        }

        return String.join(".", fullPathUntilFilename, FileType.HTML.getExtension());
    }

    /**
     * Returns object md5. If object exists but has no md5 metadata, will return an empty string.
     *
     * This method does not handle object not exist. Please be sure the object exist
     * before using this method.
     *
     * If you need to consider object not exist, use
     * `this.doesObjectExistAndGetMd5()` instead.
     */
    protected String getObjectMd5(final String key) {
        final HeadObjectResponse res = this.s3.headObject(HeadObjectRequest.builder().bucket(this.bucketName).key(key).build());
        if (res.hasMetadata()) {
            final Map<String, String> metadata = res.metadata();
            final String md5 = metadata.get("md5");
            if (md5 != null) {
                return md5;
            }
        }

        // backward compatibility for objects that don't have md5 metadata yet
        return "";
    }

    /**
     * @param key
     * @return md5 value - object exists and has md5 in metadata empty string -
     *         object exists but has no md5 in metadata null - object does not exist
     */
    protected String doesObjectExistAndGetMd5(final String key) {
        try {
            return this.getObjectMd5(key);
        } catch (final S3Exception e) {
            if (e.statusCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    // listObject
    // https://docs.aws.amazon.com/cli/latest/reference/s3api/list-objects-v2.html
    // SO: https://stackoverflow.com/a/53856863/9814131
    private ListObjectsV2Iterable listObjects(final String prefix) {
        return this.s3.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(this.bucketName).prefix(prefix).build());
    }

    /**
     * @param directoryAsPrefix
     * @return objecy key; if no object exists, will return `null`
     */
    public String getLatestObjectKey(final String directoryAsPrefix) {
        final ListObjectsV2Iterable paginatedList = this.listObjects(directoryAsPrefix);
        Instant latestTime = null;
        String key = null;
        for (final ListObjectsV2Response page : paginatedList) {
            for (final S3Object object : page.contents()) {
                final String objectKey = object.key();

                final String[] objectKeyTokensSplitBySlash = objectKey.split("/");

                final String filenameWithExtension = objectKeyTokensSplitBySlash[objectKeyTokensSplitBySlash.length - 1];

                final String objectTimestamp = filenameWithExtension.substring(0, filenameWithExtension.lastIndexOf("."));

                final Instant objectTime = Instant.parse(objectTimestamp);
                if (latestTime == null || objectTime.isAfter(latestTime)) {
                    latestTime = objectTime;
                    key = objectKey;
                }
            }
        }

        return key;
    }

    public Boolean putLatestObject(
        final String directoryAsPrefix,
        final String filenameWithoutExtension,
        final Object data,
        FileType fileType
    ) {
        final String latestObjectKey = this.getLatestObjectKey(directoryAsPrefix);

        // filter out cases where no need to write, or illegal cases
        if (latestObjectKey != null) {
            final String md5OnS3 = this.getObjectMd5(latestObjectKey);

            if (md5OnS3.strip().isEmpty()) {
                throw new ScraperShouldHaltException(this.getNoMd5ErrorMessage(latestObjectKey));
            }

            if (S3Service.toMD5Base64String(data).equals(md5OnS3)) {
                Logger.info(directoryAsPrefix + ", latest object " + latestObjectKey + " md5 is identical to our data, will not write.");
                return false;
            }
        }

        final String pathUntilFilenameWithoutExtension = Path.of(directoryAsPrefix, filenameWithoutExtension).toString();
        this.putFileOnS3(pathUntilFilenameWithoutExtension, data, fileType);
        Logger.info(directoryAsPrefix + ", new object, or latest md5 not the same, writing to s3.");
        return true;
    }

    private String getNoMd5ErrorMessage(final String key) {
        return (
            "The object at key " +
            key +
            " does not have md5. Every object is required to have a md5. If you believe the object was created by a previous version of scraper and not because of a problem with current scraper version, you may delete the object on s3 and re-launch this scraper job again."
        );
    }
}
