/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.search.filter;

import io.camunda.util.ObjectBuilder;
import java.util.Set;

public record GroupFilter(Long groupKey, String name, Set<Long> memberKeys) implements FilterBase {

  public static final class Builder implements ObjectBuilder<GroupFilter> {
    private Long groupKey;
    private String name;
    private Set<Long> memberKeys;

    public Builder groupKey(final Long value) {
      groupKey = value;
      return this;
    }

    public Builder name(final String value) {
      name = value;
      return this;
    }

    public Builder memberKey(final Long value) {
      return memberKeys(Set.of(value));
    }

    public Builder memberKeys(final Set<Long> value) {
      memberKeys = value;
      return this;
    }

    @Override
    public GroupFilter build() {
      return new GroupFilter(groupKey, name, memberKeys);
    }
  }
}
