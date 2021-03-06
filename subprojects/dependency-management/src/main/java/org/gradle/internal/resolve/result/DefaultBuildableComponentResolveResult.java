/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.resolve.result;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.internal.component.model.ComponentResolveMetadata;
import org.gradle.internal.resolve.ModuleVersionNotFoundException;
import org.gradle.internal.resolve.ModuleVersionResolveException;

public class DefaultBuildableComponentResolveResult extends DefaultResourceAwareResolveResult implements BuildableComponentResolveResult {
    private ComponentResolveMetadata metadata;
    private ModuleVersionResolveException failure;

    public DefaultBuildableComponentResolveResult failed(ModuleVersionResolveException failure) {
        metadata = null;
        this.failure = failure;
        return this;
    }

    @Override
    public void notFound(ModuleComponentIdentifier versionIdentifier) {
        failed(new ModuleVersionNotFoundException(DefaultModuleVersionIdentifier.newId(versionIdentifier), getAttempted()));
    }

    public void resolved(ComponentResolveMetadata metaData) {
        this.metadata = metaData;
    }

    public void setMetadata(ComponentResolveMetadata metadata) {
        assertResolved();
        this.metadata = metadata;
    }

    @Override
    public ComponentIdentifier getId() {
        assertResolved();
        return metadata.getId();
    }

    public ModuleVersionIdentifier getModuleVersionId() throws ModuleVersionResolveException {
        assertResolved();
        return metadata.getModuleVersionId();
    }

    public ComponentResolveMetadata getMetadata() throws ModuleVersionResolveException {
        assertResolved();
        return metadata;
    }

    public ModuleVersionResolveException getFailure() {
        assertHasResult();
        return failure;
    }

    private void assertResolved() {
        assertHasResult();
        if (failure != null) {
            throw failure;
        }
    }

    private void assertHasResult() {
        if (!hasResult()) {
            throw new IllegalStateException("No result has been specified.");
        }
    }

    public boolean hasResult() {
        return failure != null || metadata != null;
    }

    public void applyTo(BuildableComponentIdResolveResult idResolve) {
        super.applyTo(idResolve);
        if (failure != null) {
            idResolve.failed(failure);
        }
        if (metadata != null) {
            idResolve.resolved(metadata);
        }
    }
}
