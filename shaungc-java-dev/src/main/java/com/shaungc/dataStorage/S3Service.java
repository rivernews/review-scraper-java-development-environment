package com.shaungc.dataStorage;

import com.google.gson.Gson;
import com.shaungc.utilities.Logger;
import java.nio.charset.StandardCharsets;
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

    public S3Service(String buketName) {
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

            s3.putBucketTagging(
                PutBucketTaggingRequest
                    .builder()
                    .bucket(this.bucketName)
                    .tagging(Tagging.builder().tagSet(Tag.builder().key("costGroup").value("scraperJob").build()).build())
                    .build()
            );

            Logger.info("Bucket created using default configuration: " + this.bucketName);
        } catch (BucketAlreadyOwnedByYouException e) {
            Logger.info("Bucket already own by you, will do nothing");
        } catch (BucketAlreadyExistsException e) {
            Logger.info("Bucket name used by others and must be corrected first: " + this.bucketName);
            throw e;
        } catch (Exception e) {
            Logger.info("Unknown error occured while using the bucket name " + this.bucketName);
            throw e;
        }
    }

    public static String serializeJavaObject(Object object) {
        return S3Service.GSON_TOOL.toJson(object);
    }

    public static String toMD5Base64String(Object object) {
        if (object instanceof String) {
            return BinaryUtils.toBase64(Md5Utils.computeMD5Hash(((String) object).getBytes(StandardCharsets.UTF_8)));
        } else {
            return BinaryUtils.toBase64(Md5Utils.computeMD5Hash(S3Service.serializeJavaObject(object).getBytes(StandardCharsets.UTF_8)));
        }
    }

    protected void putObjectOfString(String key, String content) {
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put("md5", S3Service.toMD5Base64String(content));

        // Put Object
        this.s3.putObject(
                PutObjectRequest.builder().bucket(this.bucketName).key(key).metadata(metadata).build(),
                RequestBody.fromString(content)
            );
    }

    protected String getObjectMd5(String key) {
        HeadObjectResponse res = this.s3.headObject(HeadObjectRequest.builder().bucket(this.bucketName).key(key).build());
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
     * @return
     *      md5 value - object exists and has md5 in metadata
     *      empty string - object exists but has no md5 in metadata
     *      null - object does not exist
     */
    protected String doesObjectExistAndGetMd5(String key) {
        try {
            return this.getObjectMd5(key);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    // listObject
    // https://docs.aws.amazon.com/cli/latest/reference/s3api/list-objects-v2.html
    // SO: https://stackoverflow.com/a/53856863/9814131
    private ListObjectsV2Iterable listObjects(String prefix) {
        return this.s3.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(this.bucketName).prefix(prefix).build());
    }

    /**
     * @param bucketName
     * @param prefix
     * @return objecy key; if no object exists, will return `null`
     */
    public String getLatestObjectKey(String bucketName, String prefix) {
        ListObjectsV2Iterable paginatedList = this.listObjects(prefix);
        Instant latestTime = null;
        String key = null;
        for (ListObjectsV2Response page : paginatedList) {
            for (S3Object object : page.contents()) {
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
}
