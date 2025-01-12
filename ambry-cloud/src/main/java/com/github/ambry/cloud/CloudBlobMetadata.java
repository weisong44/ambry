/**
 * Copyright 2019 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.github.ambry.cloud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.ambry.commons.BlobId;
import com.github.ambry.utils.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Blob metadata document POJO class.
 */
@JsonSerialize(using = CloudBlobMetadata.MetadataSerializer.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudBlobMetadata {
  public static final String FIELD_ID = "id";
  public static final String FIELD_PARTITION_ID = "partitionId";
  public static final String FIELD_ACCOUNT_ID = "accountId";
  public static final String FIELD_CONTAINER_ID = "containerId";
  public static final String FIELD_SIZE = "size";
  public static final String FIELD_CREATION_TIME = "creationTime";
  public static final String FIELD_UPLOAD_TIME = "uploadTime";
  public static final String FIELD_DELETION_TIME = "deletionTime";
  public static final String FIELD_EXPIRATION_TIME = "expirationTime";
  public static final String FIELD_ENCRYPTION_ORIGIN = "encryptionOrigin";
  public static final String FIELD_VCR_KMS_CONTEXT = "vcrKmsContext";
  public static final String FIELD_CRYPTO_AGENT_FACTORY = "cryptoAgentFactory";
  public static final String FIELD_ENCRYPTED_SIZE = "encryptedSize";
  public static final String FIELD_NAME_SCHEME_VERSION = "nameSchemeVersion";
  public static final String FIELD_LIFE_VERSION = "lifeVersion";
  // Used for getting system generated last modified time field
  public static final String SYSTEM_GENERATED_FIELD_LAST_UPDATED_TIME = "_ts";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private String id;
  private String partitionId;
  private long creationTime;
  private long uploadTime;
  private long size;
  private int accountId;
  private int containerId;
  private long expirationTime = Utils.Infinite_Time;
  private long deletionTime = Utils.Infinite_Time;
  private int nameSchemeVersion = 0;
  private EncryptionOrigin encryptionOrigin = EncryptionOrigin.NONE;
  private String vcrKmsContext;
  private String cryptoAgentFactory;
  private long encryptedSize;
  private short lifeVersion;
  // this field is derived from the system generated last modified Time in the cosmos db which is present as "_ts" in
  // the incoming JSON. Since we are using custom serializer, this field won't be included in the outgoing JSON when
  // writing to cosmos.
  @JsonProperty(SYSTEM_GENERATED_FIELD_LAST_UPDATED_TIME)
  private long lastUpdateTime;

  /**
   * Possible values of encryption origin for cloud stored blobs.
   * Only considers encryption initiated by Ambry.
   */
  public enum EncryptionOrigin {

    /** Not encrypted by Ambry */
    NONE,
    /** Encrypted by Router */
    ROUTER,
    /** Encrypted by VCR */
    VCR
  }

  /**
   * Default constructor (for JSONSerializer).
   */
  public CloudBlobMetadata() {
  }

  /**
   * Constructor from {@link BlobId}.
   * @param blobId The BlobId for metadata record.
   * @param creationTime The blob creation time.
   * @param expirationTime The blob expiration time.
   * @param size The blob size.
   * @param encryptionOrigin The blob's encryption origin.
   */
  public CloudBlobMetadata(BlobId blobId, long creationTime, long expirationTime, long size,
      EncryptionOrigin encryptionOrigin) {
    this(blobId, creationTime, expirationTime, size, encryptionOrigin, (short) 0);
  }

  /**
   * Constructor from {@link BlobId}.
   * @param blobId The BlobId for metadata record.
   * @param creationTime The blob creation time.
   * @param expirationTime The blob expiration time.
   * @param size The blob size.
   * @param encryptionOrigin The blob's encryption origin.
   * @param lifeVersion The life version.
   */
  public CloudBlobMetadata(BlobId blobId, long creationTime, long expirationTime, long size,
      EncryptionOrigin encryptionOrigin, short lifeVersion) {
    this(blobId, creationTime, expirationTime, size, encryptionOrigin, null, null, 0, lifeVersion);
  }

  /**
   * Constructor from {@link BlobId}.
   * @param blobId The BlobId for metadata record.
   * @param creationTime The blob creation time.
   * @param expirationTime The blob expiration time.
   * @param size The blob size.
   * @param encryptionOrigin The blob's encryption origin.
   * @param vcrKmsContext The KMS context used to encrypt the blob.  Only used when encryptionOrigin = VCR.
   * @param cryptoAgentFactory The class name of the {@link CloudBlobCryptoAgentFactory} used to encrypt the blob.
   *                         Only used when encryptionOrigin = VCR.
   * @param encryptedSize The size of the uploaded blob if it was encrypted and then uploaded.
   *                      Only used when encryptionOrigin = VCR.
   * @param lifeVersion The life version.
   */
  public CloudBlobMetadata(BlobId blobId, long creationTime, long expirationTime, long size,
      EncryptionOrigin encryptionOrigin, String vcrKmsContext, String cryptoAgentFactory, long encryptedSize,
      short lifeVersion) {
    this.id = blobId.getID();
    this.partitionId = blobId.getPartition().toPathString();
    this.accountId = blobId.getAccountId();
    this.containerId = blobId.getContainerId();
    this.creationTime = creationTime;
    this.expirationTime = expirationTime;
    this.uploadTime = System.currentTimeMillis();
    this.deletionTime = Utils.Infinite_Time;
    this.size = size;
    this.encryptionOrigin = encryptionOrigin;
    this.vcrKmsContext = vcrKmsContext;
    this.cryptoAgentFactory = cryptoAgentFactory;
    this.encryptedSize = encryptedSize;
    this.lastUpdateTime = System.currentTimeMillis();
    this.lifeVersion = lifeVersion;
  }

  /**
   * @return the blob Id.
   */
  // Note: the field name and getter name must be this way to work with Azure CosmosDB.
  public String getId() {
    return id;
  }

  public CloudBlobMetadata setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * @return the partition Id.
   */
  public String getPartitionId() {
    return partitionId;
  }

  /**
   * Set the partition Id.
   * @param partitionId the partition Id of the blob.
   * @return this instance.
   */
  public CloudBlobMetadata setPartitionId(String partitionId) {
    this.partitionId = partitionId;
    return this;
  }

  /**
   * @return the blob creation time.
   */
  public long getCreationTime() {
    return creationTime;
  }

  /**
   * Set the creation time.
   * @param creationTime the creation time of the blob.
   * @return this instance.
   */
  public CloudBlobMetadata setCreationTime(long creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  /**
   * @return the blob upload time.
   */
  public long getUploadTime() {
    return uploadTime;
  }

  /**
   * Set the upload time.
   * @param uploadTime the upload time of the blob.
   * @return this instance.
   */
  public CloudBlobMetadata setUploadTime(long uploadTime) {
    this.uploadTime = uploadTime;
    return this;
  }

  /**
   * @return the blob expiration time.
   */
  public long getExpirationTime() {
    return expirationTime;
  }

  /**
   * Set the blob expiration time.
   * @param expirationTime the expiration time of the blob.
   * @return this instance.
   */
  public CloudBlobMetadata setExpirationTime(long expirationTime) {
    this.expirationTime = expirationTime;
    return this;
  }

  /**
   * @return the blob deletion time.
   */
  public long getDeletionTime() {
    return deletionTime;
  }

  /**
   * Set the blob deletion time.
   * @param deletionTime the deletion time of the blob.
   * @return this instance.
   */
  public CloudBlobMetadata setDeletionTime(long deletionTime) {
    this.deletionTime = deletionTime;
    return this;
  }

  /**
   * @return the blob size.
   */
  public long getSize() {
    return size;
  }

  /**
   * Set the size.
   * @param size the size of the blob in bytes.
   * @return this instance.
   */
  public CloudBlobMetadata setSize(long size) {
    this.size = size;
    return this;
  }

  /**
   * @return the account Id.
   */
  public int getAccountId() {
    return accountId;
  }

  /**
   * Set the account Id.
   * @param accountId the account Id of the blob.
   * @return this instance.
   */
  public CloudBlobMetadata setAccountId(int accountId) {
    this.accountId = accountId;
    return this;
  }

  /**
   * @return the container Id.
   */
  public int getContainerId() {
    return containerId;
  }

  /**
   * Set the container Id.
   * @param containerId the container Id of the blob.
   * @return this instance.
   */
  public CloudBlobMetadata setContainerId(int containerId) {
    this.containerId = containerId;
    return this;
  }

  /**
   * @return the {@link EncryptionOrigin}.
   */
  public EncryptionOrigin getEncryptionOrigin() {
    return encryptionOrigin;
  }

  /**
   * Sets the encryption origin.
   * @param encryptionOrigin the {@link EncryptionOrigin}.
   * @return this instance.
   */
  public CloudBlobMetadata setEncryptionOrigin(EncryptionOrigin encryptionOrigin) {
    this.encryptionOrigin = encryptionOrigin;
    return this;
  }

  /**
   * @return the VCR KMS context.
   */
  public String getVcrKmsContext() {
    return vcrKmsContext;
  }

  /**
   * Sets the VCR KMS context.
   * @param vcrKmsContext the KMS context used for encryption.
   * @return this instance.
   */
  public CloudBlobMetadata setVcrKmsContext(String vcrKmsContext) {
    this.vcrKmsContext = vcrKmsContext;
    return this;
  }

  /**
   * @return the blob naming scheme version.
   */
  public int getNameSchemeVersion() {
    return nameSchemeVersion;
  }

  /**
   * Sets the blob naming scheme version.
   * @param nameSchemeVersion the version of blob naming scheme.
   * @return this instance.
   */
  public CloudBlobMetadata setNameSchemeVersion(int nameSchemeVersion) {
    this.nameSchemeVersion = nameSchemeVersion;
    return this;
  }

  /**
   * @return the VCR crypto agent factory class name.
   */
  public String getCryptoAgentFactory() {
    return cryptoAgentFactory;
  }

  /**
   * Sets the VCR crypto agent factory class name.
   * @param cryptoAgentFactory the class name of the {@link CloudBlobCryptoAgentFactory} used for encryption.
   * @return this instance.
   */
  public CloudBlobMetadata setCryptoAgentFactory(String cryptoAgentFactory) {
    this.cryptoAgentFactory = cryptoAgentFactory;
    return this;
  }

  /**
   * @return the encrypted size of the blob if the blob was encrypted and uploaded to cloud, -1 otherwise
   */
  public long getEncryptedSize() {
    return encryptedSize;
  }

  /**
   * Sets the encrypted size of the blob
   * @param encryptedSize
   */
  public CloudBlobMetadata setEncryptedSize(long encryptedSize) {
    this.encryptedSize = encryptedSize;
    return this;
  }

  /**
   * @return the last update time of the blob.
   */
  public long getLastUpdateTime() {
    return lastUpdateTime;
  }

  /**
   * Sets the last update time of the blob.
   * @param lastUpdateTime last update time.
   */
  public void setLastUpdateTime(long lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  /**
   * @return the life version of the blob.
   */
  public short getLifeVersion() {
    return lifeVersion;
  }

  /**
   * Sets the life version of the blob.
   * @param lifeVersion life version.
   */
  public void setLifeVersion(short lifeVersion) {
    this.lifeVersion = lifeVersion;
  }

  /**
   * Utility to cap specified {@link CloudBlobMetadata} list by specified size of its blobs.
   * Always returns at least one metadata object irrespective of size.
   * @param originalList List of {@link CloudBlobMetadata}.
   * @param size total size of metadata's blobs.
   * @return {@link List} of {@link CloudBlobMetadata} capped by size.
   */
  public static List<CloudBlobMetadata> capMetadataListBySize(List<CloudBlobMetadata> originalList, long size) {
    long totalSize = 0;
    List<CloudBlobMetadata> cappedList = new ArrayList<>();
    for (CloudBlobMetadata metadata : originalList) {
      // Cap results at max size
      if (totalSize + metadata.getSize() > size) {
        if (cappedList.isEmpty()) {
          // We must add at least one regardless of size
          cappedList.add(metadata);
        }
        break;
      }
      cappedList.add(metadata);
      totalSize += metadata.getSize();
    }
    return cappedList;
  }

  /**
   * @return true if this blob is marked as deleted, false otherwise.
   */
  public boolean isDeleted() {
    return (deletionTime != Utils.Infinite_Time);
  }

  /**
   * @return true if this blob has expired, false otherwise.
   */
  public boolean isExpired() {
    return expirationTime != Utils.Infinite_Time && expirationTime < System.currentTimeMillis();
  }

  /**
   * A TTL-UPDATE to cloud sets the expirationTime to -1 in Azure blob metadata.
   * Additionally, a blob can be uploaded to Azure with expirationTime to -1.
   * Both these cases are indistinguishable in cloud because it is not a log-structured file on disk.
   * Both these cases do not warrant a ttl-update, and therefore we must return false when the expiry is -1.
   * @return True if expirationTime = -1, else false.
   */
  public boolean isTtlUpdated() {
    return expirationTime == Utils.Infinite_Time;
  }

  /**
   * @return true if this blob is undeleted.
   */
  public boolean isUndeleted() {
    return !isDeleted() && lifeVersion > 0;
  }

  /**
   * @param retentionPeriod period for which blobs marked a deleted aren't compacted away.
   * @return true if deletion time is outside retention window or blob is expired. false otherwise.
   */
  public boolean isCompactionCandidate(long retentionPeriod) {
    return (isDeleted() && deletionTime <= (System.currentTimeMillis() - retentionPeriod)) || (isExpired()
        && expirationTime <= (System.currentTimeMillis() - retentionPeriod));
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CloudBlobMetadata)) {
      return false;
    }
    CloudBlobMetadata om = (CloudBlobMetadata) o;
    return (Objects.equals(id, om.id) && Objects.equals(partitionId, om.partitionId) && accountId == om.accountId
        && containerId == om.containerId && size == om.size && creationTime == om.creationTime
        && uploadTime == om.uploadTime && expirationTime == om.expirationTime && deletionTime == om.deletionTime
        && nameSchemeVersion == om.nameSchemeVersion && encryptionOrigin == om.encryptionOrigin && Objects.equals(
        vcrKmsContext, om.vcrKmsContext) && Objects.equals(cryptoAgentFactory, om.cryptoAgentFactory)
        && encryptedSize == om.encryptedSize && lifeVersion == om.lifeVersion);
  }

  /**
   * @return a {@link HashMap} of metadata key-value pairs.
   */
  public Map<String, String> toMap() {
    return objectMapper.convertValue(this, new TypeReference<Map<String, String>>() {
    });
  }

  /**
   * @return a {@link CloudBlobMetadata} from a property map.
   * @param properties the key-value property map.
   */
  public static CloudBlobMetadata fromMap(Map<String, String> properties) {
    return objectMapper.convertValue(properties, CloudBlobMetadata.class);
  }

  /** Custom serializer for CloudBlobMetadata class that omits fields with default values. */
  static class MetadataSerializer extends StdSerializer<CloudBlobMetadata> {

    /** Default constructor required for Jackson serialization. */
    public MetadataSerializer() {
      super(CloudBlobMetadata.class);
    }

    @Override
    public void serialize(CloudBlobMetadata value, JsonGenerator gen, SerializerProvider provider) throws IOException {

      gen.writeStartObject();
      // Required fields
      gen.writeStringField(FIELD_ID, value.id);
      gen.writeStringField(FIELD_PARTITION_ID, value.partitionId);
      gen.writeNumberField(FIELD_ACCOUNT_ID, value.accountId);
      gen.writeNumberField(FIELD_CONTAINER_ID, value.containerId);
      gen.writeNumberField(FIELD_SIZE, value.size);
      // Optional fields with default values to exclude
      if (value.creationTime > 0) {
        gen.writeNumberField(FIELD_CREATION_TIME, value.creationTime);
      }
      if (value.uploadTime > 0) {
        gen.writeNumberField(FIELD_UPLOAD_TIME, value.uploadTime);
      }
      if (value.deletionTime > 0) {
        gen.writeNumberField(FIELD_DELETION_TIME, value.deletionTime);
      }
      if (value.expirationTime > 0) {
        gen.writeNumberField(FIELD_EXPIRATION_TIME, value.expirationTime);
      }
      if (value.nameSchemeVersion > 0) {
        gen.writeNumberField(FIELD_NAME_SCHEME_VERSION, value.nameSchemeVersion);
      }
      if (value.lifeVersion > 0) {
        gen.writeNumberField(FIELD_LIFE_VERSION, value.lifeVersion);
      }
      // Encryption fields that may or may not apply
      if (value.encryptionOrigin != null && value.encryptionOrigin != EncryptionOrigin.NONE) {
        gen.writeStringField(FIELD_ENCRYPTION_ORIGIN, value.encryptionOrigin.toString());
        if (value.encryptionOrigin == EncryptionOrigin.VCR) {
          gen.writeStringField(FIELD_VCR_KMS_CONTEXT, value.vcrKmsContext);
          gen.writeStringField(FIELD_CRYPTO_AGENT_FACTORY, value.cryptoAgentFactory);
          gen.writeNumberField(FIELD_ENCRYPTED_SIZE, value.encryptedSize);
        }
      }
      gen.writeEndObject();
    }
  }
}
