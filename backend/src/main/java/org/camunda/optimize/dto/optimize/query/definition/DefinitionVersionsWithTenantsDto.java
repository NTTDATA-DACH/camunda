/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

package org.camunda.optimize.dto.optimize.query.definition;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.camunda.optimize.dto.optimize.DefinitionType;
import org.camunda.optimize.dto.optimize.SimpleDefinitionDto;
import org.camunda.optimize.dto.optimize.TenantDto;

import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.naturalOrder;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
public class DefinitionVersionsWithTenantsDto extends SimpleDefinitionDto {

  @NonNull
  private List<DefinitionVersionWithTenantsDto> versions;
  @NonNull
  private List<TenantDto> allTenants;

  public DefinitionVersionsWithTenantsDto(@NonNull final String key,
                                          final String name,
                                          @NonNull final DefinitionType type,
                                          final Boolean isEventProcess,
                                          @NonNull final List<DefinitionVersionWithTenantsDto> versions,
                                          @NonNull final List<TenantDto> allTenants) {
    super(key, name, type, isEventProcess);
    this.versions = versions;
    this.allTenants = allTenants;
  }

  public void sort() {
    versions.sort(
      Comparator.comparing(
        DefinitionVersionWithTenantsDto::getVersion,
        (o1, o2) -> {
          if (StringUtils.isNumeric(o1) && StringUtils.isNumeric(o2)) {
            return Long.valueOf(o1).compareTo(Long.valueOf(o2));
          } else {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
          }
        }
      ).reversed()
    );
    allTenants.sort(Comparator.comparing(TenantDto::getId, Comparator.nullsFirst(naturalOrder())));
  }
}
