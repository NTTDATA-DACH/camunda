package org.camunda.optimize.upgrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.camunda.optimize.service.es.schema.ElasticsearchMetadataService;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.camunda.optimize.upgrade.AbstractUpgradeIT;
import org.camunda.optimize.upgrade.exception.UpgradeRuntimeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.camunda.optimize.upgrade.EnvironmentConfigUtil.createEmptyEnvConfig;
import static org.camunda.optimize.upgrade.EnvironmentConfigUtil.deleteEnvConfig;
import static org.junit.Assert.fail;

public class ValidationServiceIT extends AbstractUpgradeIT {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ValidationService underTest;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    underTest = new ValidationService(
      new ConfigurationService(),
      new ElasticsearchMetadataService(new ObjectMapper())
    );
    initSchema(Lists.newArrayList(METADATA_TYPE));
  }

  @Test
  public void versionValidationBreaksWithoutIndex() {
    try {
      underTest.validateVersions(restClient, "2.0", "2.1");
    } catch (UpgradeRuntimeException e) {
      //expected
      return;
    }

    fail("Exception expected");
  }

  @Test
  public void versionValidationBreaksWithoutMatchingVersion() {
    //given
    setMetadataIndexVersion("Test");

    try {
      //when
      underTest.validateVersions(restClient, "2.0", "2.1");
    } catch (UpgradeRuntimeException e) {
      //expected
      //then
      return;
    }

    fail("Exception expected");
  }

  @Test
  public void versionValidationPassesWithMatchingVersion() {
    //given
    setMetadataIndexVersion("2.0");

    //when
    underTest.validateVersions(restClient, "2.0", "2.1");

    //then - no exception
  }

  @Test
  public void toVersionIsNotAllowedToBeNull() {
    //given
    setMetadataIndexVersion("2.0");

    try {
      //when
      underTest.validateVersions(restClient, "2.0", null);
    } catch (UpgradeRuntimeException e) {
      //expected
      //then
      return;
    }

    fail("Exception expected");
  }

  @Test
  public void toVersionIsNotAllowedToBeEmptyString() {
    //given
    setMetadataIndexVersion("2.0");

    try {
      //when
      underTest.validateVersions(restClient, "2.0", "");
    } catch (UpgradeRuntimeException e) {
      //expected
      //then
      return;
    }

    fail("Exception expected");
  }

  @Test
  public void validateThrowsExceptionWithoutEnvironmentConfig() throws Exception {
    // given
    deleteEnvConfig();

    //throws
    thrown.expect(RuntimeException.class);
    thrown.expectMessage("Couldn't read environment-config.yaml from environment folder in Optimize root!");

    //when
    underTest.validateEnvironmentConfigInClasspath();
  }

  @Test
  public void validateWithEnvironmentConfig() throws Exception {
    //given
    createEmptyEnvConfig();

    //when
    underTest.validateEnvironmentConfigInClasspath();
  }
}