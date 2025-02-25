/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import { FC, useEffect, useState } from "react";
import { Tag } from "@carbon/react";
import { UseEntityModalCustomProps } from "src/components/modal";
import { assignTenantMember } from "src/utility/api/membership";
import useTranslate from "src/utility/localization";
import { useApi, useApiCall } from "src/utility/api/hooks";
import { searchUser, User } from "src/utility/api/users";
import { TranslatedErrorInlineNotification } from "src/components/notifications/InlineNotification";
import styled from "styled-components";
import DropdownSearch from "src/components/form/DropdownSearch";
import FormModal from "src/components/modal/FormModal";
import { Tenant } from "src/utility/api/tenants";

const SelectedUsers = styled.div`
  margin-top: 0;
`;

const AssignMembersModal: FC<
  UseEntityModalCustomProps<
    { id: Tenant["tenantKey"] },
    { assignedUsers: User[] }
  >
> = ({ entity: tenant, assignedUsers, onSuccess, open, onClose }) => {
  const { t, Translate } = useTranslate();
  const [selectedUsers, setSelectedUsers] = useState<User[]>([]);
  const [loadingAssignUser, setLoadingAssignUser] = useState(false);

  const {
    data: userSearchResults,
    loading,
    reload,
    errors,
  } = useApi(searchUser);

  const [callAssignUser] = useApiCall(assignTenantMember);

  const unassignedUsers =
    userSearchResults?.items.filter(
      ({ id }) =>
        !assignedUsers.some((user) => user.id === id) &&
        !selectedUsers.some((user) => user.id === id),
    ) || [];

  const onSelectUser = (user: User) => {
    setSelectedUsers([...selectedUsers, user]);
  };

  const onUnselectUser =
    ({ id }: User) =>
    () => {
      setSelectedUsers(selectedUsers.filter((user) => user.id !== id));
    };

  const canSubmit = tenant && selectedUsers.length;

  const handleSubmit = async () => {
    if (!canSubmit) return;

    setLoadingAssignUser(true);

    const results = await Promise.all(
      selectedUsers.map(({ id }) =>
        callAssignUser({ userId: id!, tenantId: tenant.id }),
      ),
    );

    setLoadingAssignUser(false);

    if (results.every(({ success }) => success)) {
      onSuccess();
    }
  };

  useEffect(() => {
    if (open) {
      setSelectedUsers([]);
    }
  }, [open]);

  return (
    <FormModal
      headline={t("Assign user")}
      confirmLabel={t("Assign user")}
      loading={loadingAssignUser}
      loadingDescription={t("Assigning user")}
      open={open}
      onSubmit={handleSubmit}
      submitDisabled={!canSubmit}
      onClose={onClose}
      overflowVisible
    >
      <p>
        <Translate>Search and assign user to tenant</Translate>
      </p>
      {selectedUsers.length > 0 && (
        <SelectedUsers>
          {selectedUsers.map((user) => (
            <Tag
              key={user.id}
              onClose={onUnselectUser(user)}
              size="md"
              type="blue"
              filter
            >
              {user.username}
            </Tag>
          ))}
        </SelectedUsers>
      )}

      <DropdownSearch
        autoFocus
        items={unassignedUsers}
        itemTitle={({ username }) => username}
        itemSubTitle={({ email }) => email}
        placeholder={t("Search by full name or email address")}
        onChange={() => null}
        onSelect={onSelectUser}
      />

      {!loading && errors && (
        <TranslatedErrorInlineNotification
          title={t("Users could not be loaded.")}
          actionButton={{ label: t("Retry"), onClick: reload }}
        />
      )}
    </FormModal>
  );
};

export default AssignMembersModal;
