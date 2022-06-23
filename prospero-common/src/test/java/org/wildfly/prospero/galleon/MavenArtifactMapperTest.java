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

package org.wildfly.prospero.galleon;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wildfly.channel.ArtifactCoordinate;
import org.wildfly.channel.MavenArtifact;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MavenArtifactMapperTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testMapChannelArtifactToGalleonArtifact() throws Exception {
        final MavenArtifact channelArtifact = new MavenArtifact("foo.bar", "test", "jar", "", "1.2.3", temporaryFolder.newFile());
        final org.jboss.galleon.universe.maven.MavenArtifact galleonArtifact = galleonArtifact("foo.bar", "test", "jar");

        assertTrue(MavenArtifactMapper.isSameArtifact(channelArtifact, galleonArtifact));
    }

    @Test
    public void testMapListOfGalleonArtifactsToGav() throws Exception {
        final List<org.jboss.galleon.universe.maven.MavenArtifact> galleonArtifacts = Arrays.asList(
                galleonArtifact("foo.bar", "test1", "jar"),
                galleonArtifact("foo.bar", "test2", "jar"));

        final MavenArtifactMapper mavenArtifactMapper = new MavenArtifactMapper(galleonArtifacts);
        List<ArtifactCoordinate> gavs = mavenArtifactMapper.toChannelArtifacts();

        assertNotNull(gavs);
        assertEquals(2, gavs.size());
        assertEquals("test1", gavs.get(0).getArtifactId());
        assertEquals("test2", gavs.get(1).getArtifactId());
    }

    @Test
    public void testMapListOfMavenArtifactsBackToGalleonArtifacts() throws Exception {
        final List<org.jboss.galleon.universe.maven.MavenArtifact> galleonArtifacts = Arrays.asList(
                galleonArtifact("foo.bar", "test1", "jar"),
                galleonArtifact("foo.bar", "test2", "jar"));

        final File file1 = temporaryFolder.newFile();
        final File file2 = temporaryFolder.newFile();
        final List<MavenArtifact> channelArtifacts = Arrays.asList(
                new MavenArtifact("foo.bar", "test2", "jar", "", "1.2.4", file2),
                new MavenArtifact("foo.bar", "test1", "jar", "", "1.2.3", file1)
        );

        final MavenArtifactMapper mavenArtifactMapper = new MavenArtifactMapper(galleonArtifacts);

        List<org.jboss.galleon.universe.maven.MavenArtifact> mappedArtifacts =
                (List<org.jboss.galleon.universe.maven.MavenArtifact>) mavenArtifactMapper.applyResolution(channelArtifacts);

        assertNotNull(mappedArtifacts);
        assertEquals(2, mappedArtifacts.size());
        assertEquals("test1", mappedArtifacts.get(0).getArtifactId());
        assertEquals("1.2.3", mappedArtifacts.get(0).getVersion());
        assertEquals(file1.toPath(), mappedArtifacts.get(0).getPath());
        assertEquals("test2", mappedArtifacts.get(1).getArtifactId());
        assertEquals(file2.toPath(), mappedArtifacts.get(1).getPath());
        assertEquals("1.2.4", mappedArtifacts.get(1).getVersion());
    }

    private org.jboss.galleon.universe.maven.MavenArtifact galleonArtifact(String groupId, String artifactId, String extension) {
        final org.jboss.galleon.universe.maven.MavenArtifact galleonArtifact = new org.jboss.galleon.universe.maven.MavenArtifact();
        galleonArtifact.setGroupId(groupId);
        galleonArtifact.setArtifactId(artifactId);
        galleonArtifact.setExtension(extension);
        galleonArtifact.setClassifier("");
        galleonArtifact.setVersion("");
        return galleonArtifact;
    }

}
