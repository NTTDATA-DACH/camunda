/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.system.partitions.impl.steps;

import io.atomix.raft.RaftServer.Role;
import io.camunda.zeebe.broker.system.partitions.PartitionTransitionContext;
import io.camunda.zeebe.broker.system.partitions.PartitionTransitionStep;
import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

public final class MetricsStep implements PartitionTransitionStep {

  @Override
  public ActorFuture<Void> prepareTransition(
      final PartitionTransitionContext context, final long term, final Role targetRole) {
    final var transitionMeterRegistry =
        (CompositeMeterRegistry) context.getPartitionTransitionMeterRegistry();
    final var startupMeterRegistry = context.getPartitionStartupMeterRegistry();
    if (transitionMeterRegistry != null) {
      // Clear all meters from the partition registry. Their values are invalid after the
      // transition.
      transitionMeterRegistry.clear();
      // Remove the backing broker registry from the partition registry so that we can close the
      // partition registry without closing the broker registry.
      // This also increases reliability in case something holds on to the partition registry
      // because any new meters will no longer be forwarded to the broker registry.
      transitionMeterRegistry.remove(startupMeterRegistry);
      transitionMeterRegistry.close();
      context.setPartitionTransitionMeterRegistry(null);
    }
    return context.getConcurrencyControl().createCompletedFuture();
  }

  @Override
  public ActorFuture<Void> transitionTo(
      final PartitionTransitionContext context, final long term, final Role targetRole) {
    final var startupMeterRegistry = context.getPartitionStartupMeterRegistry();
    final var transitionRegistry = new CompositeMeterRegistry();

    // Wrap over the broker registry so that all meters are forwarded to the broker registry.
    transitionRegistry.add(startupMeterRegistry);

    context.setPartitionTransitionMeterRegistry(transitionRegistry);
    return context.getConcurrencyControl().createCompletedFuture();
  }

  @Override
  public String getName() {
    return "Metrics";
  }
}
