/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {useState} from 'react';
import {OperationItem} from 'modules/components/OperationItem';
import {OperationItems} from 'modules/components/OperationItems';
import {DeleteDefinitionModal} from 'modules/components/DeleteDefinitionModal';

type Props = {
  processName: string;
  processVersion: string;
};

const ProcessOperations: React.FC<Props> = ({processName, processVersion}) => {
  const [isDeleteModalVisible, setIsDeleteModalVisible] =
    useState<boolean>(false);

  return (
    <>
      <OperationItems>
        <OperationItem
          title={`Delete Process Definition "${processName} - Version ${processVersion}"`}
          type="DELETE"
          onClick={() => setIsDeleteModalVisible(true)}
        />
      </OperationItems>
      <DeleteDefinitionModal
        title="Delete Process Definition"
        description="You are about to delete the following process definition:"
        isVisible={isDeleteModalVisible}
        bodyContent=""
        confirmationText="Yes, I confirm I want to delete this process definition."
        onClose={() => setIsDeleteModalVisible(false)}
        onDelete={() => {}}
      />
    </>
  );
};

export {ProcessOperations};
