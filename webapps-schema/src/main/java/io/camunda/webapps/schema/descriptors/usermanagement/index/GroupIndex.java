/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.descriptors.usermanagement.index;

import io.camunda.webapps.schema.descriptors.backup.Prio5Backup;
import io.camunda.webapps.schema.descriptors.usermanagement.UserManagementIndexDescriptor;
import io.camunda.webapps.schema.entities.usermanagement.EntityJoinRelation.EntityJoinRelationFactory;
import io.camunda.webapps.schema.entities.usermanagement.EntityJoinRelation.IdentityJoinRelationshipType;

public class GroupIndex extends UserManagementIndexDescriptor implements Prio5Backup {

  public static final String INDEX_NAME = "group";
  public static final String INDEX_VERSION = "8.8.0";
  public static final String KEY = "key";
  public static final String MEMBER_KEY = "memberKey";
  public static final String NAME = "name";
  public static final String JOIN = "join";

  public static final EntityJoinRelationFactory JOIN_RELATION_FACTORY =
      new EntityJoinRelationFactory(
          IdentityJoinRelationshipType.GROUP, IdentityJoinRelationshipType.MEMBER);

  public GroupIndex(final String indexPrefix, final boolean isElasticsearch) {
    super(indexPrefix, isElasticsearch);
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }

  @Override
  public String getVersion() {
    return INDEX_VERSION;
  }
}
