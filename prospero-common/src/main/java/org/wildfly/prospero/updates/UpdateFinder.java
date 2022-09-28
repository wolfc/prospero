/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.prospero.updates;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jboss.galleon.ProvisioningException;
import org.jboss.galleon.ProvisioningManager;
import org.jboss.galleon.layout.ProvisioningPlan;
import org.jboss.logging.Logger;
import org.wildfly.channel.ChannelSession;
import org.wildfly.channel.UnresolvedMavenArtifactException;
import org.wildfly.prospero.api.ArtifactChange;
import org.wildfly.prospero.api.exceptions.ArtifactResolutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class UpdateFinder implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(UpdateFinder.class);

    public static final int UPDATES_SEARCH_PARALLELISM = 10;

    private final ChannelSession channelSession;
    private final ProvisioningManager provisioningManager;
    private final ExecutorService executorService;

    public UpdateFinder(ChannelSession channelSession, ProvisioningManager provisioningManager) {
        this.channelSession = channelSession;
        this.provisioningManager = provisioningManager;
        this.executorService = Executors.newWorkStealingPool(UPDATES_SEARCH_PARALLELISM);
    }

    public UpdateSet findUpdates(List<Artifact> artifacts) throws ArtifactResolutionException, ProvisioningException {
        // use parallel executor to speed up the artifact resolution
        List<CompletableFuture<Optional<ArtifactChange>>> allPackages = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            final CompletableFuture<Optional<ArtifactChange>> cf = new CompletableFuture<>();
            executorService.submit(() -> {
                try {
                    final Optional<ArtifactChange> found = findUpdates(artifact);
                    cf.complete(found);
                } catch (Exception e) {
                    cf.completeExceptionally(e);
                }
            });
            allPackages.add(cf);
        }

        try {
            CompletableFuture.allOf(allPackages.toArray(new CompletableFuture[]{})).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof ArtifactResolutionException) {
                throw (ArtifactResolutionException) e.getCause();
            } else {
                throw e;
            }
        }

        final List<ArtifactChange> updates = allPackages.stream()
                .map(cf ->cf.getNow(Optional.empty()))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        final ProvisioningPlan fpUpdates = findFPUpdates();
        return new UpdateSet(fpUpdates, updates);
    }

    private Optional<ArtifactChange> findUpdates(Artifact artifact) throws ArtifactResolutionException {
        final String latestVersion;
        try {
            latestVersion = channelSession.findLatestMavenArtifactVersion(artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getExtension(), artifact.getClassifier(), artifact.getVersion());
        } catch (UnresolvedMavenArtifactException e) {
            // TODO: I18N
            LOGGER.warn(String.format("Artifact [%s:%s] not found", artifact.getGroupId(), artifact.getArtifactId()));
            return Optional.empty();
        }
        final Artifact latest = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(), latestVersion);


        if (latestVersion == null || latest.getVersion().equals(artifact.getVersion())) {
            return Optional.empty();
        } else {
            return Optional.of(new ArtifactChange(artifact, latest));
        }
    }

    private ProvisioningPlan findFPUpdates() throws ProvisioningException {
        return provisioningManager.getUpdates(true);
    }

    @Override
    public void close() {
        this.executorService.shutdown();
    }

}
