package com.shaungc.dataStorage;

import com.shaungc.utilities.Logger;
import java.nio.charset.StandardCharsets;
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
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

/**
 * S3Service
 */
public class S3Service {
    private final S3Client s3;

    // S3 Canned ACL:
    // https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#canned-acl
    static BucketCannedACL BUCKET_ACCESS_CANNED_ACL = BucketCannedACL.PRIVATE;

    public S3Service() {
        s3 = S3Client.builder().region(Region.US_WEST_2).build();
    }

    protected void createBucket(String bucketName) {
        try {
            s3.createBucket(
                CreateBucketRequest
                    .builder()
                    .bucket(bucketName)
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
                    .bucket(bucketName)
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

            Logger.info("Bucket created using default configuration: " + bucketName);
        } catch (BucketAlreadyOwnedByYouException e) {
            Logger.info("Bucket already own by you, will do nothing");
        } catch (BucketAlreadyExistsException e) {
            Logger.info("Bucket name used by others and must be corrected first: " + bucketName);
            throw e;
        } catch (Exception e) {
            Logger.info("Unknown error occured while using the bucket name " + bucketName);
            throw e;
        }
    }

    public static String toMD5Base64String(String content) {
        return BinaryUtils.toBase64(Md5Utils.computeMD5Hash(content.getBytes(StandardCharsets.UTF_8)));
    }

    // TODO: object CRUD operations
    protected void putObjectOfString(String bucketName, String key, String content) {
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put("md5", S3Service.toMD5Base64String(content));

        // Put Object
        this.s3.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key).metadata(metadata).build(),
                RequestBody.fromString(content)
            );
    }

    protected String doesObjectExist(String bucketName, String key) {
        try {
            HeadObjectResponse res = this.s3.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
            if (res.hasMetadata()) {
                final Map<String, String> metadata = res.metadata();
                final String md5 = metadata.get("md5");
                if (md5 != null) {
                    return md5;
                }
            }
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return null;
            }
            throw e;
        }

        // backward compatibility for objects that don't have md5 metadata yet
        return "";
    }
}
