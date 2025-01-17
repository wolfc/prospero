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

package org.wildfly.prospero.cli.commands.options;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wildfly.prospero.cli.ArgumentParsingException;
import org.wildfly.prospero.wfchannel.MavenSessionManager;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class LocalRepoOptionsTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void defaultLocalPathIfNoOptionsSpecified() throws Exception {
        assertEquals(Optional.of(MavenSessionManager.LOCAL_MAVEN_REPO), LocalRepoOptions.getLocalRepo(null));
    }

    @Test
    public void emptyLocalPathIfNoLocalCacheSpecified() throws Exception {
        final LocalRepoOptions localRepoParam = new LocalRepoOptions();
        localRepoParam.noLocalCache = true;

        assertEquals(Optional.empty(), LocalRepoOptions.getLocalRepo(localRepoParam));
    }

    @Test
    public void customLocalPathIfLocalRepoSpecified() throws Exception {
        final LocalRepoOptions localRepoParam = new LocalRepoOptions();
        final Path localRepo = temp.newFolder().toPath();
        localRepoParam.localRepo = localRepo;

        assertEquals(Optional.of(localRepo), LocalRepoOptions.getLocalRepo(localRepoParam));
    }

    @Test
    public void localRepoPointsAtFile() throws Exception {
        final LocalRepoOptions localRepoParam = new LocalRepoOptions();
        File file = temp.newFile();
        localRepoParam.localRepo = file.toPath();
        Assert.assertThrows(ArgumentParsingException.class, () ->
                LocalRepoOptions.getLocalRepo(localRepoParam)
        );
    }
}