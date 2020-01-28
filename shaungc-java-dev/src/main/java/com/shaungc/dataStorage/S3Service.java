package com.shaungc.dataStorage;

import com.shaungc.javadev.Logger;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3Service
 */
public class S3Service {

    final private S3Client s3;

    // S3 Canned ACL:
    // https://docs.aws.amazon.com/AmazonS3/latest/dev/acl-overview.html#canned-acl
    static BucketCannedACL BUCKET_ACCESS_CANNED_ACL = BucketCannedACL.PRIVATE;

    public S3Service() {
        s3 = S3Client.builder().build();
    }

    protected void createBucket(String bucketName) {
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucketName)
                    .createBucketConfiguration(CreateBucketConfiguration.builder().build())
                    // TODO: set bucket to private
                    .acl(S3Service.BUCKET_ACCESS_CANNED_ACL).build());

            Logger.info("Bucket created using default configuration: " + bucketName);
        } catch (BucketAlreadyOwnedByYouException e) {
            Logger.info("Bucket already own by you, will do nothing");
        } catch (BucketAlreadyExistsException e) {
            Logger.info("Bucket name used by others and must be corrected first: " + bucketName);
            throw e;
        }
    }

    // TODO: object CRUD operations
    protected void putObjectOfString(String bucketName, String key, String content) {
        // Put Object
        this.s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(),
                RequestBody.fromString(content));
    }
}