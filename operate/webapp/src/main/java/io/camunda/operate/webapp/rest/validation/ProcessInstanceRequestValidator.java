/*
 * Copyright Camunda Services GmbH
 *
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING, OR DISTRIBUTING THE SOFTWARE (“USE”), YOU INDICATE YOUR ACCEPTANCE TO AND ARE ENTERING INTO A CONTRACT WITH, THE LICENSOR ON THE TERMS SET OUT IN THIS AGREEMENT. IF YOU DO NOT AGREE TO THESE TERMS, YOU MUST NOT USE THE SOFTWARE. IF YOU ARE RECEIVING THE SOFTWARE ON BEHALF OF A LEGAL ENTITY, YOU REPRESENT AND WARRANT THAT YOU HAVE THE ACTUAL AUTHORITY TO AGREE TO THE TERMS AND CONDITIONS OF THIS AGREEMENT ON BEHALF OF SUCH ENTITY.
 * “Licensee” means you, an individual, or the entity on whose behalf you receive the Software.
 *
 * Permission is hereby granted, free of charge, to the Licensee obtaining a copy of this Software and associated documentation files to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject in each case to the following conditions:
 * Condition 1: If the Licensee distributes the Software or any derivative works of the Software, the Licensee must attach this Agreement.
 * Condition 2: Without limiting other conditions in this Agreement, the grant of rights is solely for non-production use as defined below.
 * "Non-production use" means any use of the Software that is not directly related to creating products, services, or systems that generate revenue or other direct or indirect economic benefits.  Examples of permitted non-production use include personal use, educational use, research, and development. Examples of prohibited production use include, without limitation, use for commercial, for-profit, or publicly accessible systems or use for commercial or revenue-generating purposes.
 *
 * If the Licensee is in breach of the Conditions, this Agreement, including the rights granted under it, will automatically terminate with immediate effect.
 *
 * SUBJECT AS SET OUT BELOW, THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * NOTHING IN THIS AGREEMENT EXCLUDES OR RESTRICTS A PARTY’S LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY THAT PARTY’S NEGLIGENCE, (B) FRAUD, OR (C) ANY OTHER LIABILITY TO THE EXTENT THAT IT CANNOT BE LAWFULLY EXCLUDED OR RESTRICTED.
 */
package io.camunda.operate.webapp.rest.validation;

import static io.camunda.operate.entities.OperationType.ADD_VARIABLE;
import static io.camunda.operate.entities.OperationType.UPDATE_VARIABLE;

import io.camunda.operate.util.rest.ValidLongId;
import io.camunda.operate.webapp.reader.OperationReader;
import io.camunda.operate.webapp.reader.VariableReader;
import io.camunda.operate.webapp.rest.dto.VariableRequestDto;
import io.camunda.operate.webapp.rest.dto.metadata.FlowNodeMetadataRequestDto;
import io.camunda.operate.webapp.rest.dto.operation.CreateBatchOperationRequestDto;
import io.camunda.operate.webapp.rest.dto.operation.CreateOperationRequestDto;
import io.camunda.operate.webapp.rest.exception.InvalidRequestException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProcessInstanceRequestValidator {
  private final VariableReader variableReader;
  private final OperationReader operationReader;

  private final CreateBatchOperationRequestValidator createBatchOperationRequestValidator;

  public ProcessInstanceRequestValidator(
      final VariableReader variableReader,
      final OperationReader operationReader,
      final CreateBatchOperationRequestValidator createBatchOperationRequestValidator) {
    this.variableReader = variableReader;
    this.operationReader = operationReader;
    this.createBatchOperationRequestValidator = createBatchOperationRequestValidator;
  }

  public void validateFlowNodeMetadataRequest(final FlowNodeMetadataRequestDto request) {
    if (request.getFlowNodeId() == null
        && request.getFlowNodeType() == null
        && request.getFlowNodeInstanceId() == null) {
      throw new InvalidRequestException(
          "At least flowNodeId or flowNodeInstanceId must be specifies in the request.");
    }
    if (request.getFlowNodeId() != null && request.getFlowNodeInstanceId() != null) {
      throw new InvalidRequestException(
          "Only one of flowNodeId or flowNodeInstanceId must be specifies in the request.");
    }
  }

  public void validateVariableRequest(final VariableRequestDto request) {
    if (request.getScopeId() == null) {
      throw new InvalidRequestException("ScopeId must be specifies in the request.");
    }
  }

  public void validateCreateBatchOperationRequest(
      final CreateBatchOperationRequestDto batchOperationRequest) {
    createBatchOperationRequestValidator.validate(batchOperationRequest);
  }

  public void validateCreateOperationRequest(
      final CreateOperationRequestDto operationRequest,
      @ValidLongId final String processInstanceId) {
    if (operationRequest.getOperationType() == null) {
      throw new InvalidRequestException("Operation type must be defined.");
    }
    if (Set.of(UPDATE_VARIABLE, ADD_VARIABLE).contains(operationRequest.getOperationType())
        && (operationRequest.getVariableScopeId() == null
            || operationRequest.getVariableName() == null
            || operationRequest.getVariableName().isEmpty()
            || operationRequest.getVariableValue() == null)) {
      throw new InvalidRequestException(
          "ScopeId, name and value must be defined for UPDATE_VARIABLE operation.");
    }
    if (operationRequest.getOperationType().equals(ADD_VARIABLE)
        && (variableReader.getVariableByName(
                    processInstanceId,
                    operationRequest.getVariableScopeId(),
                    operationRequest.getVariableName())
                != null
            || !operationReader
                .getOperations(
                    ADD_VARIABLE,
                    processInstanceId,
                    operationRequest.getVariableScopeId(),
                    operationRequest.getVariableName())
                .isEmpty())) {
      throw new InvalidRequestException(
          String.format(
              "Variable with the name \"%s\" already exists.", operationRequest.getVariableName()));
    }
  }
}
